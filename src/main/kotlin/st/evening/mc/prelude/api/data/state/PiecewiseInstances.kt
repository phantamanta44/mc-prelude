package st.evening.mc.prelude.api.data.state

import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.util.collection.WeakValidityMap

open class ValueStateAtom<T>(initialValue: T, private val serializer: NetworkSerializer<T>) :
    ObservableValue<T>(initialValue), Piecewise.Atom {

    private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    fun setValueFromSync(newValue: T) {
        storedValue = newValue
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        serializer.serializeToNetwork(buf, storedValue)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        setValueFromSync(serializer.deserializeFromNetwork(buf))
    }
}

open class ListStateComposite(private val atoms: List<Piecewise.Atom>) : Piecewise.Composite {
    private val stateObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()
    private val syncObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()

    constructor(vararg atoms: Piecewise.Atom) : this(atoms.toList())

    companion object {
        fun fromStates(children: Iterable<Piecewise>): ListStateComposite {
            val atoms = mutableListOf<Piecewise.Atom>()
            children.forEach { atoms.addAtoms(it) }
            return ListStateComposite(atoms)
        }

        fun fromStates(vararg children: Piecewise): ListStateComposite {
            val atoms = mutableListOf<Piecewise.Atom>()
            children.forEach { atoms.addAtoms(it) }
            return ListStateComposite(atoms)
        }
    }

    init {
        install(0)
    }

    override val stateSize: Int
        get() = atoms.size

    private fun install(fromIndex: Int) {
        var i = fromIndex
        while (i < atoms.size) {
            atoms[i].observeFromComposite(i, stateObservers, syncObservers)
            i++
        }
    }

    override fun getStateAtom(index: Int): Piecewise.Atom = atoms[index]

    override fun observeState(observer: Observer.Indexed) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Indexed) {
        syncObservers += observer
    }

    class Builder {
        private val atoms: MutableList<Piecewise.Atom> = mutableListOf()
        private val composite: ListStateComposite = ListStateComposite(atoms)

        infix fun <T : Piecewise> part(state: T): T {
            val fromIndex = atoms.size
            atoms.addAtoms(state)
            composite.install(fromIndex)
            return state
        }

        fun build(): ListStateComposite = composite
    }
}

object TrivialStateComposite : Piecewise.Composite {
    override val stateSize: Int
        get() = 0

    override fun getStateAtom(index: Int): Piecewise.Atom = throw IndexOutOfBoundsException(index.toString())

    override fun observeSync(observer: Observer.Indexed) {}

    override fun observeState(observer: Observer.Indexed) {}
}

class SingletonStateComposite(private val atom: Piecewise.Atom) : Piecewise.Composite {
    override val stateSize: Int
        get() = 1

    override fun getStateAtom(index: Int): Piecewise.Atom = atom

    override fun observeState(observer: Observer.Indexed) {
        atom.observeState(Observer.Simple.proxy(observer) {
            observer.onObservableUpdate(0)
        })
    }

    override fun observeSync(observer: Observer.Indexed) {
        atom.observeSync(Observer.Simple.proxy(observer) {
            observer.onObservableUpdate(0)
        })
    }
}

fun Piecewise.asComposite(): Piecewise.Composite = when (this) {
    is Piecewise.Atom -> SingletonStateComposite(this)
    is Piecewise.Composite -> this
}
