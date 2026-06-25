package st.evening.mc.prelude.api.util.network

import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.NetHandlerPlayServer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide

inline fun <reified REQ : IMessage, REPLY : IMessage> SimpleNetworkWrapper.registerMessage(
    handler: IMessageHandler<REQ, REPLY>,
    discrim: Int,
    recvSide: Side
) {
    registerMessage(handler, REQ::class.java, discrim, recvSide)
}

inline fun <reified REQ : IMessage, REPLY : IMessage> SimpleNetworkWrapper.registerS2C(
    handler: IMessageHandler<REQ, REPLY>,
    discrim: Int
) {
    registerMessage<REQ, REPLY>(handler, discrim, Side.CLIENT)
}

inline fun <reified REQ : IMessage, REPLY : IMessage> SimpleNetworkWrapper.registerC2S(
    handler: IMessageHandler<REQ, REPLY>,
    discrim: Int
) {
    registerMessage<REQ, REPLY>(handler, discrim, Side.SERVER)
}

@Suppress("UnusedReceiverParameter")
@ClientSide.Strong
fun NetHandlerPlayClient.onClientThread(action: () -> Unit) {
    Minecraft.getMinecraft().addScheduledTask(action)
}

@ServerSide
fun NetHandlerPlayServer.onServerThread(action: () -> Unit) {
    player.serverWorld.addScheduledTask(action)
}
