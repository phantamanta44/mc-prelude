package st.evening.mc.prelude.api.data.state

import st.evening.mc.prelude.api.util.collection.WeakValidityMap

open class VirtualObservableAtom : Observable.Atom {
    private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    open fun notifyObservers() {
        stateObservers.onObservableUpdate()
    }

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }
}

class VirtualQueuedObservableAtom : VirtualObservableAtom() {
    private var dirty: Boolean = false

    override fun notifyObservers() {
        dirty = true
    }

    fun flush() {
        if (dirty) {
            super.notifyObservers()
            dirty = false
        }
    }
}

open class VirtualObservableComposite(override val stateSize: Int) : Observable.Composite {
    private val stateObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()

    open fun notifyObservers(index: Int) {
        stateObservers.onObservableUpdate(index)
    }

    override fun observeState(observer: Observer.Indexed) {
        stateObservers += observer
    }
}

class VirtualQueuedObservableComposite(stateSize: Int) : VirtualObservableComposite(stateSize) {
    private val dirtyState: BooleanArray = BooleanArray(stateSize)

    override fun notifyObservers(index: Int) {
        dirtyState[index] = true
    }

    fun flush() {
        dirtyState.forEachIndexed { index, dirty ->
            if (dirty) {
                super.notifyObservers(index)
                dirtyState[index] = false
            }
        }
    }
}

open class ObservableValue<T>(initialValue: T) : Observable.Atom {
    protected var storedValue: T = initialValue

    private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    var value: T
        get() = storedValue
        set(newValue) {
            storedValue = newValue
            stateObservers.onObservableUpdate()
        }

    fun update(newValue: T): Boolean {
        if (storedValue == newValue) return false
        value = newValue
        return true
    }

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }
}

class ListObservableComposite(private val children: List<Observable>) : Observable.Composite {
    private val stateObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()

    constructor(vararg children: Observable) : this(children.toList())

    init {
        children.forEachIndexed { index, child -> install(index, child) }
    }

    override val stateSize: Int
        get() = children.size

    private fun install(index: Int, child: Observable) {
        when (child) {
            is Observable.Atom -> child.observeState(Observer.Simple.bindIndex(index, stateObservers))
            is Observable.Composite -> child.observeState(Observer.Indexed.fixed { _ ->
                stateObservers.onObservableUpdate(index)
            })
        }
    }

    override fun observeState(observer: Observer.Indexed) {
        stateObservers += observer
    }

    class Builder {
        private val children: MutableList<Observable> = mutableListOf()
        private val composite: ListObservableComposite = ListObservableComposite(children)

        infix fun <T : Observable> part(state: T): T {
            composite.install(children.size, state)
            children += state
            return state
        }

        fun build(): ListObservableComposite = composite
    }
}

object TrivialObservableComposite : Observable.Composite {
    override val stateSize: Int
        get() = 0

    override fun observeState(observer: Observer.Indexed) {}
}
