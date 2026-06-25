package st.evening.mc.prelude.api.registration

import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.fml.common.ModContainer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.capability.TrivialCapabilityStorage
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.system.asAccessible
import java.lang.reflect.Field
import java.util.IdentityHashMap
import java.util.concurrent.Callable

interface CapabilityRegistrationVisitor<T> {
    fun <C> accept(type: Class<C>, storage: Capability.IStorage<C>, factory: Callable<C>): T
}

object CapabilityRegistrar {
    val TARGET: RegistrationTarget<CapabilityRegistrar, Entry.T, Capability<*>> =
        object : RegistrationTarget<CapabilityRegistrar, Entry.T, Capability<*>> {
            override fun init(mod: ModContainer, reg: ModRegistrar): CapabilityRegistrar = CapabilityRegistrar

            override fun <U : Capability<*>> register(
                state: CapabilityRegistrar,
                entry: Ap<Entry.T, U>
            ): RegistrationHolder<U> = state.register(entry.unap())
        }

    private val fCapabilityManager_providers: Field =
        CapabilityManager::class.java.getDeclaredField(("providers")).asAccessible()

    @Suppress("UNCHECKED_CAST")
    private val providers: IdentityHashMap<String, Capability<*>> =
        fCapabilityManager_providers.get(CapabilityManager.INSTANCE) as IdentityHashMap<String, Capability<*>>

    private fun <C : Capability<*>> register(entry: Entry<C>): RegistrationHolder<C> {
        Prelude.logger.trace("Registering capability: {}", entry.capabilityType.name)
        val realName = entry.visit(object : CapabilityRegistrationVisitor<String> {
            override fun <C> accept(
                type: Class<C>,
                storage: Capability.IStorage<C>,
                factory: Callable<C>
            ): String {
                CapabilityManager.INSTANCE.register(type, storage, factory)
                return type.name.intern()
            }
        })
        // technically unsound, but nobody should be extending Capability anyways
        // ideally we would use a type family for the registration "output", but the higher kinds trick doesn't work for
        // third-party classes since we can't make them implement Ap
        @Suppress("UNCHECKED_CAST")
        return RegistrationHolder.Immediate(providers.getValue(realName) as C)
    }

    interface Entry<C : Capability<*>> : Ap<Entry.T, C> {
        object T : TT

        val capabilityType: Class<*>

        fun <T> visit(visitor: CapabilityRegistrationVisitor<T>): T
    }
}

fun <C : Capability<*>> Ap<CapabilityRegistrar.Entry.T, C>.unap(): CapabilityRegistrar.Entry<C> =
    this as CapabilityRegistrar.Entry<C>

fun <C : Any> ModRegistrar.capability(
    capabilityType: Class<C>,
    storage: Capability.IStorage<C>,
    factory: () -> C
): RegistrationHolder<Capability<C>> = register(
    CapabilityRegistrar.TARGET,
    object : CapabilityRegistrar.Entry<Capability<C>> {
        override val capabilityType: Class<*>
            get() = capabilityType

        override fun <T> visit(visitor: CapabilityRegistrationVisitor<T>): T =
            visitor.accept(capabilityType, storage, factory)
    }
)

inline fun <reified C : Any> ModRegistrar.capability(
    storage: Capability.IStorage<C>,
    noinline factory: () -> C
): RegistrationHolder<Capability<C>> = capability(C::class.java, storage, factory)

inline fun <reified C : Any> ModRegistrar.capability(
    noinline factory: () -> C
): RegistrationHolder<Capability<C>> = capability(TrivialCapabilityStorage(), factory)
