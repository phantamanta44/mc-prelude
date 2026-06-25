package st.evening.mc.prelude.grs

import com.cleanroommc.groovyscript.api.INamed
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.SoundEvent
import net.minecraft.world.World
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.C2SMessageHandler
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.network.S2CMessageHandler
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.RegistrationHolder
import st.evening.mc.prelude.api.registration.TileEntityRegistrar
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.block
import st.evening.mc.prelude.api.registration.c2sPacket
import st.evening.mc.prelude.api.registration.item
import st.evening.mc.prelude.api.registration.s2cPacket
import st.evening.mc.prelude.api.registration.soundEvent
import st.evening.mc.prelude.api.registration.tileEntity

class GrSRegistrarWrapper(private val reg: ModRegistrar) : INamed {
    override fun getAliases(): Collection<String> = listOf("registrar")

    fun <B : Block> block(name: String, factory: () -> B): RegistrationHolder<B> = reg.block(name, factory)

    fun <I : Item> item(name: String, factory: () -> I): RegistrationHolder<I> = reg.item(name, factory)

    fun <T : TileEntity> tileEntity(
        name: String,
        teClass: Class<T>,
        factory: (World, IBlockState) -> T = TileEntityRegistrar.getDefaultFactory(teClass)
    ): RegistrationHolder<TileEntityType<T>> = reg.tileEntity(name, teClass, factory)

    fun soundEvent(soundName: String): RegistrationHolder<SoundEvent> = reg.soundEvent(soundName)

    fun <M> s2cPacket(
        serializer: NetworkSerializer<M>,
        handler: S2CMessageHandler<M>
    ): RegistrationHolder<PacketType.S2C<M>> = reg.s2cPacket(serializer, handler)

    fun <M> c2sPacket(
        serializer: NetworkSerializer<M>,
        handler: C2SMessageHandler<M>
    ): RegistrationHolder<PacketType.C2S<M>> = reg.c2sPacket(serializer, handler)
}
