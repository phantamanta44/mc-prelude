package st.evening.mc.prelude.api.registration

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.event.FMLEvent
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.data.UnboundGenericProperty
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.game.ClientSide

interface ModRegistrar {
    val modId: String

    @PreludeInternal
    fun <S> getState(target: RegistrationTarget<S, *, *>): S

    @OptIn(PreludeInternal::class)
    fun <S, E : TT, T : Any, U : T> register(
        target: RegistrationTarget<S, E, T>,
        entry: Ap<E, U>
    ): RegistrationHolder<U> = target.register(getState(target), entry)

    fun <E : FMLEvent> registerFmlListener(eventType: Class<E>, listener: (E) -> Unit)

    @ClientSide.Physical
    fun setConfigGuiFactory(factory: IModGuiFactory)
}

inline fun <reified E : FMLEvent> ModRegistrar.on(noinline listener: (E) -> Unit) {
    registerFmlListener(E::class.java, listener)
}

fun ModRegistrar.creativeTab(iconFactory: () -> ItemStack): CreativeTabs = object : CreativeTabs(modId) {
    @ClientSide.Physical
    override fun createIcon(): ItemStack = iconFactory()
}

interface RegistrationTarget<S, E : TT, T : Any> {
    fun init(mod: ModContainer, reg: ModRegistrar): S

    fun <U : T> register(state: S, entry: Ap<E, U>): RegistrationHolder<U>
}

interface RegistrationHolder<T : Any> : UnboundGenericProperty<T> {
    fun resolve(): T?

    override fun getPropertyValue(): T =
        resolve() ?: throw IllegalStateException("Registration holder accessed before binding: $this")

    override fun toString(): String

    class Immediate<T : Any>(private val value: T) : RegistrationHolder<T> {
        override fun resolve(): T = value

        override fun toString(): String = value.toString()
    }

    class FromForgeRegistry<T : IForgeRegistryEntry<T>, U : T>(
        private val registry: IForgeRegistry<T>,
        private val key: ResourceLocation
    ) : RegistrationHolder<U> {
        private var cached: U? = null

        @Suppress("UNCHECKED_CAST")
        override fun resolve(): U? = cached ?: orNull(registry.containsKey(key)) {
            (registry.getValue(key) as U).also { cached = it }
        }

        override fun toString(): String = "${registry.registrySuperType.simpleName} $key"
    }
}
