package st.evening.mc.prelude.api.network

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.util.ReferenceCounted
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.INetHandler
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.LoaderState
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel
import net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec
import net.minecraftforge.fml.common.network.FMLOutboundHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import net.minecraftforge.fml.relauncher.Side
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.game.assertStrongClient
import java.lang.ref.WeakReference

class NetworkWrapper @PreludeInternal constructor(mod: ModContainer, reg: ModRegistrar, name: String) {
    private val codec: MessageCodec = MessageCodec()
    private val packetTypes: MutableList<PacketTypeImpl<*>> = mutableListOf()
    private val s2cChannel: FMLEmbeddedChannel
    private val c2sChannel: FMLEmbeddedChannel
    private var writable: Boolean = true

    init {
        check(!Loader.instance().hasReachedState(LoaderState.AVAILABLE)) { "Network wrapper initialized too late!" }
        reg.on<FMLLoadCompleteEvent> { _ ->
            writable = false
        }
        val channels = NetworkRegistry.INSTANCE.newChannel(mod, name, codec)
        s2cChannel = channels.getValue(Side.SERVER).apply {
            pipeline().addAfter(
                findChannelHandlerNameForType(MessageCodec::class.java),
                "PreludeS2CMessageHandler#",
                WrappedMessageHandler
            )
        }
        c2sChannel = channels.getValue(Side.CLIENT).apply {
            pipeline().addAfter(
                findChannelHandlerNameForType(MessageCodec::class.java),
                "PreludeC2SMessageHandler#",
                WrappedMessageHandler
            )
        }
    }

    fun <M> registerS2CMessage(serializer: NetworkSerializer<M>, handler: S2CMessageHandler<M>): PacketType.S2C<M> {
        check(writable) { "Packet registry is frozen!" }
        return S2CPacketTypeImpl(packetTypes.size, serializer, handler).also { packetTypes += it }
    }

    fun <M> registerC2SMessage(serializer: NetworkSerializer<M>, handler: C2SMessageHandler<M>): PacketType.C2S<M> {
        check(writable) { "Packet registry is frozen!" }
        return C2SPacketTypeImpl(packetTypes.size, serializer, handler).also { packetTypes += it }
    }

    private sealed interface PacketTypeImpl<M> : PacketType<M> {
        val discrim: Int
        val serializer: NetworkSerializer<M>

        fun handle(netHandler: INetHandler, message: M)
    }

    private fun FMLEmbeddedChannel.setTarget(target: FMLOutboundHandler.OutboundTarget) {
        attr(FMLOutboundHandler.FML_MESSAGETARGET).set(target)
    }

    private fun FMLEmbeddedChannel.setTargetArgs(args: Any) {
        attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(args)
    }

    private fun FMLEmbeddedChannel.writeSafe(packet: WrappedPacket<*>) {
        writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    inner class S2CPacketTypeImpl<M>(
        override val discrim: Int,
        override val serializer: NetworkSerializer<M>,
        val handler: S2CMessageHandler<M>
    ) : PacketTypeImpl<M>, PacketType.S2C<M> {
        @OptIn(SidednessAssertion::class)
        override fun handle(netHandler: INetHandler, message: M) {
            assertStrongClient {
                handler.handleS2CMessage(netHandler as NetHandlerPlayClient, message)
            }
        }

        @ServerSide
        override fun sendToAll(message: M) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.ALL)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }

        @ServerSide
        override fun sendTo(message: M, target: EntityPlayerMP) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.PLAYER)
            s2cChannel.setTargetArgs(target)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }

        @ServerSide
        override fun sendToAllAround(message: M, area: NetworkRegistry.TargetPoint) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT)
            s2cChannel.setTargetArgs(area)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }

        @ServerSide
        override fun sendToAllTracking(message: M, point: NetworkRegistry.TargetPoint) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.TRACKING_POINT)
            s2cChannel.setTargetArgs(point)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }

        @ServerSide
        override fun sendToAllTracking(message: M, tracked: Entity) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.TRACKING_ENTITY)
            s2cChannel.setTargetArgs(tracked)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }

        @ServerSide
        override fun sendToDimension(message: M, dimensionId: Int) {
            s2cChannel.setTarget(FMLOutboundHandler.OutboundTarget.DIMENSION)
            s2cChannel.setTargetArgs(dimensionId)
            s2cChannel.writeSafe(WrappedPacket(this, message))
        }
    }

    inner class C2SPacketTypeImpl<M>(
        override val discrim: Int,
        override val serializer: NetworkSerializer<M>,
        val handler: C2SMessageHandler<M>
    ) : PacketTypeImpl<M>, PacketType.C2S<M> {
        @OptIn(SidednessAssertion::class)
        override fun handle(netHandler: INetHandler, message: M) {
            val serverHandler = netHandler as NetHandlerPlayServer
            assertLogicalServer {
                handler.handleC2SMessage(serverHandler, message)
            }
        }

        @ClientSide
        override fun sendToServer(message: M) {
            c2sChannel.setTarget(FMLOutboundHandler.OutboundTarget.TOSERVER)
            c2sChannel.writeSafe(WrappedPacket(this, message))
        }
    }

    private class WrappedPacket<M>(val type: PacketTypeImpl<M>, val message: M) : ReferenceCounted {
        companion object {
            fun <M> decode(type: PacketTypeImpl<M>, buf: PacketBuffer): WrappedPacket<M> =
                WrappedPacket(type, type.serializer.deserializeFromNetwork(buf))
        }

        override fun refCnt(): Int = (message as? ReferenceCounted)?.refCnt() ?: 1

        override fun retain(): ReferenceCounted = apply {
            if (message is ReferenceCounted) {
                message.retain()
            }
        }

        override fun retain(increment: Int): ReferenceCounted = apply {
            if (message is ReferenceCounted) {
                message.retain(increment)
            }
        }

        override fun touch(): ReferenceCounted = apply {
            if (message is ReferenceCounted) {
                message.touch()
            }
        }

        override fun touch(hint: Any?): ReferenceCounted = apply {
            if (message is ReferenceCounted) {
                message.touch(hint)
            }
        }

        override fun release(): Boolean = (message as? ReferenceCounted)?.release() ?: true

        override fun release(decrement: Int): Boolean = (message as? ReferenceCounted)?.release(decrement) ?: true

        fun handle(netHandler: INetHandler) = type.handle(netHandler, message)

        fun serialize(buf: PacketBuffer) = type.serializer.serializeToNetwork(buf, message)
    }

    @ChannelHandler.Sharable
    private inner class MessageCodec : MessageToMessageCodec<FMLProxyPacket, WrappedPacket<*>>() {
        override fun handlerAdded(ctx: ChannelHandlerContext) {
            super.handlerAdded(ctx)
            ctx.channel().attr(FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER).set(ThreadLocal())
        }

        override fun encode(ctx: ChannelHandlerContext, msg: WrappedPacket<*>, out: MutableList<Any>) {
            val payload = PacketBuffer(Unpooled.buffer())
            payload.writeByte(msg.type.discrim)
            msg.serialize(payload)
            val newMsg = FMLProxyPacket(payload, ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get())
            ctx.channel().attr(FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER).get().get()?.get()?.let {
                newMsg.dispatcher = it.dispatcher
            }
            out += newMsg
        }

        override fun decode(ctx: ChannelHandlerContext, msg: FMLProxyPacket, out: MutableList<Any>) {
            val payload = msg.payload().duplicate()
            if (payload.readableBytes() < 1) return
            val discrim = payload.readByte().toInt()
            if (discrim < 0 || discrim >= packetTypes.size) {
                Prelude.logger.warn(
                    "Received packet with unknown discriminator {} on channel {}", discrim, msg.channel()
                )
                return
            }
            val newMsg = WrappedPacket.decode(packetTypes[discrim], PacketBuffer(payload.slice()))
            payload.release()
            out += newMsg
            ctx.channel().attr(FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER).get().set(WeakReference(msg))
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            super.exceptionCaught(ctx, cause)
        }
    }

    @ChannelHandler.Sharable
    private object WrappedMessageHandler : SimpleChannelInboundHandler<WrappedPacket<*>>(WrappedPacket::class.java) {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: WrappedPacket<*>) {
            msg.handle(ctx.channel().attr(NetworkRegistry.NET_HANDLER).get())
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            super.exceptionCaught(ctx, cause)
        }
    }
}
