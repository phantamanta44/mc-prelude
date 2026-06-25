package st.evening.mc.prelude.api.registration

import net.minecraftforge.fml.common.ModContainer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.C2SMessageHandler
import st.evening.mc.prelude.api.network.NetworkWrapper
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.network.S2CMessageHandler
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT

class PacketRegistrar(mod: ModContainer, reg: ModRegistrar) {
    companion object {
        val TARGET: RegistrationTarget<PacketRegistrar, Entry.T, PacketType<*>> =
            object : RegistrationTarget<PacketRegistrar, Entry.T, PacketType<*>> {
                override fun init(mod: ModContainer, reg: ModRegistrar): PacketRegistrar = PacketRegistrar(mod, reg)

                override fun <U : PacketType<*>> register(
                    state: PacketRegistrar,
                    entry: Ap<Entry.T, U>
                ): RegistrationHolder<U> = state.register(entry.unap())
            }
    }

    private val modId: String = mod.modId

    @OptIn(PreludeInternal::class)
    private val networkWrapper: NetworkWrapper by lazy { NetworkWrapper(mod, reg, modId) }

    @Suppress("UNCHECKED_CAST")
    fun <T : PacketType<*>> register(entry: Entry<T>): RegistrationHolder<T> {
        Prelude.logger.trace("Registering {} packet handler: {}", modId, entry.handler)
        return RegistrationHolder.Immediate(
            when (entry) { // relies on deterministic registration order
                is Entry.S2C<*> -> entry.register(networkWrapper) as T // sound by narrowing on T
                is Entry.C2S<*> -> entry.register(networkWrapper) as T
            }
        )
    }

    sealed interface Entry<T : PacketType<*>> : Ap<Entry.T, T> {
        object T : TT

        val handler: Any

        class S2C<M>(
            val serializer: NetworkSerializer<M>,
            override val handler: S2CMessageHandler<M>
        ) : Entry<PacketType.S2C<M>> {
            internal fun register(networkWrapper: NetworkWrapper): PacketType.S2C<M> =
                networkWrapper.registerS2CMessage(serializer, handler)
        }

        class C2S<M>(
            val serializer: NetworkSerializer<M>,
            override val handler: C2SMessageHandler<M>
        ) : Entry<PacketType.C2S<M>> {
            internal fun register(networkWrapper: NetworkWrapper): PacketType.C2S<M> =
                networkWrapper.registerC2SMessage(serializer, handler)
        }
    }
}

fun <T : PacketType<*>> Ap<PacketRegistrar.Entry.T, T>.unap(): PacketRegistrar.Entry<T> =
    this as PacketRegistrar.Entry<T>

fun <M> ModRegistrar.s2cPacket(
    serializer: NetworkSerializer<M>,
    handler: S2CMessageHandler<M>
): RegistrationHolder<PacketType.S2C<M>> = register(
    PacketRegistrar.TARGET,
    PacketRegistrar.Entry.S2C(serializer, handler)
)

fun <M> ModRegistrar.c2sPacket(
    serializer: NetworkSerializer<M>,
    handler: C2SMessageHandler<M>
): RegistrationHolder<PacketType.C2S<M>> = register(
    PacketRegistrar.TARGET,
    PacketRegistrar.Entry.C2S(serializer, handler)
)
