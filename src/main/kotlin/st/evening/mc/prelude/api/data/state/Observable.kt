package st.evening.mc.prelude.api.data.state

import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.collection.WeakValidityProxy
import st.evening.mc.prelude.api.util.collection.WeaklyValid

sealed interface Observer : WeaklyValid {
    interface Simple : Observer {
        fun onObservableUpdate()

        companion object {
            fun proxy(host: WeaklyValid, callback: () -> Unit): Simple = ProxySimpleObserver(host, callback)

            fun fixed(callback: () -> Unit): Simple = FixedSimpleObserver(callback)

            fun bindIndex(index: Int, observers: Iterable<Indexed>): Simple = BoundIndexObserver(index, observers)
        }
    }

    interface Indexed : Observer {
        fun onObservableUpdate(index: Int)

        companion object {
            fun proxy(host: WeaklyValid, callback: (Int) -> Unit): Indexed = ProxyIndexedObserver(host, callback)

            fun fixed(callback: (Int) -> Unit): Indexed = FixedIndexedObserver(callback)
        }
    }
}

fun Iterable<Observer.Simple>.onObservableUpdate() {
    forEach { it.onObservableUpdate() }
}

fun Iterable<Observer.Indexed>.onObservableUpdate(index: Int) {
    forEach { it.onObservableUpdate(index) }
}

private class ProxySimpleObserver(host: WeaklyValid, private val callback: () -> Unit) :
    Observer.Simple, WeaklyValid by WeakValidityProxy(host) {
    override fun onObservableUpdate() {
        callback()
    }
}

private class FixedSimpleObserver(private val callback: () -> Unit) : Observer.Simple {
    override val weakValidity: WeakValidity
        get() = WeakValidity.VALID

    override fun onObservableUpdate() {
        callback()
    }
}

private class BoundIndexObserver(private val index: Int, private val observers: Iterable<Observer.Indexed>) :
    Observer.Simple {
    override val weakValidity: WeakValidity
        get() = WeakValidity.VALID

    override fun onObservableUpdate() {
        observers.onObservableUpdate(index)
    }
}

private class ProxyIndexedObserver(host: WeaklyValid, private val callback: (Int) -> Unit) :
    Observer.Indexed, WeaklyValid by WeakValidityProxy(host) {
    override fun onObservableUpdate(index: Int) {
        callback(index)
    }
}

private class FixedIndexedObserver(private val callback: (Int) -> Unit) : Observer.Indexed {
    override val weakValidity: WeakValidity
        get() = WeakValidity.VALID

    override fun onObservableUpdate(index: Int) {
        callback(index)
    }
}

sealed interface Observable {
    interface Atom : Observable {
        fun observeState(observer: Observer.Simple)
    }

    interface Composite : Observable {
        val stateSize: Int

        fun observeState(observer: Observer.Indexed)
    }
}

fun <T> Observable.observeStateGeneric(observer: T) where T : Observer.Simple, T : Observer.Indexed {
    when (this) {
        is Observable.Atom -> observeState(observer)
        is Observable.Composite -> observeState(observer)
    }
}
