package st.evening.mc.prelude.api.data.state

import st.evening.mc.prelude.api.data.ser.NetworkReadable
import st.evening.mc.prelude.api.data.ser.NetworkWritable

sealed interface Piecewise {
    interface Atom : Piecewise, Observable.Atom, NetworkReadable, NetworkWritable {
        fun observeSync(observer: Observer.Simple)
    }

    interface Composite : Piecewise, Observable.Composite {
        fun getStateAtom(index: Int): Atom

        fun observeSync(observer: Observer.Indexed)
    }
}

inline fun Piecewise.Composite.forEachStateAtom(f: (Piecewise.Atom) -> Unit) {
    for (i in 0..<stateSize) {
        f(getStateAtom(i))
    }
}

fun MutableCollection<Piecewise.Atom>.addAtoms(child: Piecewise) {
    when (child) {
        is Piecewise.Atom -> this += child
        is Piecewise.Composite -> child.forEachStateAtom { this += it }
    }
}

fun Piecewise.Atom.observeAll(observer: Observer.Simple) {
    observeState(observer)
    observeSync(observer)
}

fun Piecewise.Composite.observeAll(observer: Observer.Indexed) {
    observeState(observer)
    observeSync(observer)
}

fun Piecewise.Atom.observeFromComposite(
    index: Int,
    stateObservers: Iterable<Observer.Indexed>,
    syncObservers: Iterable<Observer.Indexed>
) {
    observeState(Observer.Simple.bindIndex(index, stateObservers))
    observeSync(Observer.Simple.bindIndex(index, syncObservers))
}
