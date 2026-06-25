package st.evening.mc.prelude.api.network

import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.NetworkRegistry
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide

sealed interface PacketType<M> {
    interface S2C<M> : PacketType<M> {
        @ServerSide
        fun sendToAll(message: M)

        @ServerSide
        fun sendTo(message: M, target: EntityPlayerMP)

        @ServerSide
        fun sendToAllAround(message: M, area: NetworkRegistry.TargetPoint)

        @ServerSide
        fun sendToAllTracking(message: M, point: NetworkRegistry.TargetPoint)

        @ServerSide
        fun sendToAllTracking(message: M, tracked: Entity)

        @ServerSide
        fun sendToDimension(message: M, dimensionId: Int)
    }

    interface C2S<M> : PacketType<M> {
        @ClientSide
        fun sendToServer(message: M)
    }
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllAround(message: M, world: World, x: Double, y: Double, z: Double, radius: Double) {
    sendToAllAround(message, NetworkRegistry.TargetPoint(world.provider.dimension, x, y, z, radius))
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllAround(message: M, world: World, point: Vec3d, radius: Double) {
    sendToAllAround(message, world, point.x, point.y, point.z, radius)
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllTracking(message: M, world: World, x: Double, y: Double, z: Double) {
    sendToAllTracking(message, NetworkRegistry.TargetPoint(world.provider.dimension, x, y, z, 0.0))
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllTracking(message: M, world: World, x: Int, y: Int, z: Int) {
    sendToAllTracking(message, world, x.toDouble(), y.toDouble(), z.toDouble())
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllTracking(message: M, world: World, pos: BlockPos) {
    sendToAllTracking(message, world, pos.x, pos.y, pos.z)
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllTracking(message: M, te: TileEntity) {
    sendToAllTracking(message, te.world, te.pos) // hopefully nobody calls this during TE init, when world = null
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToAllAround(message: M, world: World, pos: BlockPos, radius: Double) {
    sendToAllAround(message, world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, radius)
}

@ServerSide
fun <M> PacketType.S2C<M>.sendToDimension(message: M, world: World) {
    sendToDimension(message, world.provider.dimension)
}

fun interface S2CMessageHandler<M> {
    @ClientSide.Strong
    fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: M)
}

fun interface C2SMessageHandler<M> {
    @ServerSide
    fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: M)
}
