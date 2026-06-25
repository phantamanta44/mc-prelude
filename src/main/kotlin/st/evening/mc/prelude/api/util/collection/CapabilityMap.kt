package st.evening.mc.prelude.api.util.collection

import net.minecraftforge.common.capabilities.Capability
import st.evening.mc.prelude.api.util.game.CapabilityCollectionVisitor
import st.evening.mc.prelude.api.util.game.CapabilityVisitor

class CapabilityMap(private val backing: MutableMap<Capability<*>, Any> = mutableMapOf()) : CapabilityVisitor {
    val size: Int
        get() = backing.size

    operator fun <T : Any> set(capability: Capability<T>, instance: T) {
        backing[capability] = instance
    }

    override fun <T : Any> visit(capability: Capability<T>, instance: T) {
        this[capability] = instance
    }

    fun putAll(capabilities: CapabilityMap) {
        backing.putAll(capabilities.backing)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(capability: Capability<T>): T? = backing[capability] as? T

    operator fun contains(capability: Capability<*>): Boolean = capability in backing

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(capability: Capability<T>): T? = backing.remove(capability) as? T

    fun clear() {
        backing.clear()
    }

    fun forEach(visitor: CapabilityVisitor) {
        backing.forEach { (capability, instance) ->
            @Suppress("UNCHECKED_CAST")
            visitor.visit(capability as Capability<Any>, instance)
        }
    }
}

class CapabilityMultimap(
    private val backing: MutableMap<Capability<*>, MutableCollection<Any>> = mutableMapOf(),
    private val valueCollectionFactory: ValueCollectionFactory = ValueCollectionFactory.ListFactory
) : CapabilityVisitor, CapabilityCollectionVisitor {
    val size: Int
        get() = backing.size

    fun <T : Any> put(capability: Capability<T>, instance: T) {
        backing.getOrPut(capability) { valueCollectionFactory.newValueCollection() } += instance
    }

    override fun <T : Any> visit(capability: Capability<T>, instance: T) {
        put(capability, instance)
    }

    fun <T : Any> putAll(capability: Capability<T>, instances: Iterable<T>) {
        backing.getOrPut(capability) { valueCollectionFactory.newValueCollection() }.addAll(instances)
    }

    override fun <T : Any> visit(capability: Capability<T>, instances: Collection<T>) {
        putAll(capability, instances)
    }

    fun <T : Any> putAll(capabilities: CapabilityMap) {
        capabilities.forEach(this)
    }

    fun <T : Any> putAll(capabilities: CapabilityMultimap) {
        capabilities.forEach(this)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(capability: Capability<T>): Collection<T>? = backing[capability] as? Collection<T>

    operator fun contains(capability: Capability<*>): Boolean = capability in backing

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(capability: Capability<T>): Collection<T>? = backing.remove(capability) as? Collection<T>

    fun clear() {
        backing.clear()
    }

    fun forEach(visitor: CapabilityCollectionVisitor) {
        backing.forEach { (capability, instances) ->
            @Suppress("UNCHECKED_CAST")
            visitor.visit(capability as Capability<Any>, instances)
        }
    }

    interface ValueCollectionFactory {
        fun <T> newValueCollection(): MutableCollection<T>

        object ListFactory : ValueCollectionFactory {
            override fun <T> newValueCollection(): MutableCollection<T> = mutableListOf()
        }

        object SetFactory : ValueCollectionFactory {
            override fun <T> newValueCollection(): MutableCollection<T> = mutableSetOf()
        }
    }
}
