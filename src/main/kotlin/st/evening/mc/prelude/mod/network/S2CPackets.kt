package st.evening.mc.prelude.mod.network

import io.netty.util.ReferenceCounted
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.event.ConfigSyncEvent
import st.evening.mc.prelude.api.network.S2CMessageHandler
import st.evening.mc.prelude.api.util.data.readString
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.network.onClientThread
import st.evening.mc.prelude.coremod.addons.NetworkManagerAddons
import st.evening.mc.prelude.mod.PreludeStrings

class S2CSyncConfig(private val configs: Map<String, Map<String, NBTTagCompound>>) {
    object Serializer : NetworkSerializer<S2CSyncConfig> {
        override fun serializeToNetwork(buf: PacketBuffer, x: S2CSyncConfig) { // TODO use generic map serializer?
            buf.writeVarInt(x.configs.size)
            x.configs.forEach { (modId, modConfigs) ->
                buf.writeString(modId)
                buf.writeVarInt(modConfigs.size)
                modConfigs.forEach { (configId, dto) ->
                    buf.writeString(configId)
                    buf.writeCompoundTag(dto)
                }
            }
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): S2CSyncConfig {
            val configs = mutableMapOf<String, MutableMap<String, NBTTagCompound>>()
            repeat(buf.readVarInt()) {
                val modConfigs = configs.getOrPut(buf.readString()) { mutableMapOf() }
                repeat(buf.readVarInt()) {
                    val configId = buf.readString()
                    modConfigs[configId] = buf.readCompoundTag()!!
                }
            }
            return S2CSyncConfig(configs)
        }
    }

    object Handler : S2CMessageHandler<S2CSyncConfig> {
        @ClientSide.Strong
        override fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: S2CSyncConfig) {
            netHandler.onClientThread {
                val event = ConfigSyncEvent.Receive(message.configs)
                MinecraftForge.EVENT_BUS.post(event)
                if (event.result == Event.Result.DENY) {
                    (netHandler.networkManager as NetworkManagerAddons).`prelude$closeChannelAsync`(
                        TextComponentTranslation(PreludeStrings.GUI_CONFIG_SYNC_FAIL)
                    )
                }
            }
        }
    }
}

class S2CSyncData(private val hostId: Int, private val data: PacketBuffer) : ReferenceCounted by data {
    object Serializer : NetworkSerializer<S2CSyncData> {
        override fun serializeToNetwork(buf: PacketBuffer, x: S2CSyncData) {
            val data = x.data
            buf.writeVarInt(x.hostId).writeBytes(data, data.readerIndex(), data.readableBytes())
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): S2CSyncData {
            buf.retain()
            return S2CSyncData(buf.readVarInt(), buf)
        }
    }

    object Handler : S2CMessageHandler<S2CSyncData> {
        @ClientSide.Strong
        @OptIn(PreludeInternal::class)
        override fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: S2CSyncData) {
            message.data.retain()
            netHandler.onClientThread {
                try {
                    Prelude.defns.syncService.handleUpdateFromServer(message.hostId, message.data)
                } finally {
                    message.data.release()
                }
            }
        }
    }
}

class S2CBindSyncedContainer(private val windowId: Int, private val hostId: Int, private val data: PacketBuffer) :
    ReferenceCounted by data {
    object Serializer : NetworkSerializer<S2CBindSyncedContainer> {
        override fun serializeToNetwork(buf: PacketBuffer, x: S2CBindSyncedContainer) {
            buf.writeByte(x.windowId)
            buf.writeVarInt(x.hostId)
            val data = x.data
            buf.writeBytes(data, data.readerIndex(), data.readableBytes())
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): S2CBindSyncedContainer {
            buf.retain()
            return S2CBindSyncedContainer(buf.readByte().toInt(), buf.readVarInt(), buf)
        }
    }

    object Handler : S2CMessageHandler<S2CBindSyncedContainer> {
        @ClientSide.Strong
        override fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: S2CBindSyncedContainer) {
            message.data.retain()
            netHandler.onClientThread {
                try {
                    val gui = Minecraft.getMinecraft().currentScreen
                    if (gui !is GuiContainer) return@onClientThread
                    val container = gui.inventorySlots
                    if (container.windowId != message.windowId) return@onClientThread
                    if (container !is SyncHost) {
                        Prelude.logger.warn(
                            "Received container sync message for non-sync-host container: {}",
                            container.javaClass.simpleName
                        )
                        return@onClientThread
                    }
                    val syncMgr = container.syncManager.getClient()
                    syncMgr.registerClientSide(message.hostId)
                    syncMgr.readFullState(message.data)
                } finally {
                    message.data.release()
                }
            }
        }
    }
}
