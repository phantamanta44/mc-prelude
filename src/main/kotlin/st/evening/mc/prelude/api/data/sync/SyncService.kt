package st.evening.mc.prelude.api.data.sync

import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.collection.WeakValidityCache
import st.evening.mc.prelude.api.util.collection.forEachInline
import st.evening.mc.prelude.api.util.collection.getOrPut
import st.evening.mc.prelude.api.util.data.readableToArray
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.absurdLogicalSide
import st.evening.mc.prelude.api.util.game.assertLogicalClient
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.game.physicalSided

sealed class SyncService(reg: ModRegistrar) {
    @ServerSide
    private val localHosts: WeakValidityCache<Int, SyncManager.Server> = WeakValidityCache(Int2ObjectOpenHashMap(), 11)

    @ServerSide
    private var nextLocalHostId: Int = 0

    @ServerSide
    private val syncQueue: Int2ObjectMap<SyncManager.Server> = Int2ObjectOpenHashMap()

    init {
        reg.on<TickEvent.ServerTickEvent> @OptIn(SidednessAssertion::class) { event ->
            if (event.phase != TickEvent.Phase.END) return@on
            assertLogicalServer {
                syncQueue.forEachInline { _, manager ->
                    if (manager.weakValidity != WeakValidity.INVALID) {
                        manager.dispatchSync()
                    }
                }
                syncQueue.clear()
            }
        }
        reg.on<FMLServerStoppedEvent> @OptIn(SidednessAssertion::class) {
            assertLogicalServer {
                resetServerState()
            }
        }
    }

    @ServerSide
    private fun resetServerState() {
        localHosts.clear()
        nextLocalHostId = 0
        syncQueue.clear()
    }

    @PreludeInternal
    @ServerSide
    fun registerServerSide(manager: SyncManager.Server): Int {
        while (nextLocalHostId in localHosts) {
            nextLocalHostId++
        }
        val hostId = nextLocalHostId++
        localHosts[hostId] = manager
        return hostId
    }

    @ServerSide
    fun enqueueForSync(manager: SyncManager.Server) {
        syncQueue.putIfAbsent(manager.hostId, manager)
    }

    @PreludeInternal
    @ClientSide
    abstract fun registerClientSide(manager: SyncManager.Client, hostId: Int)

    @PreludeInternal
    @ClientSide
    abstract fun handleUpdateFromServer(hostId: Int, data: PacketBuffer)

    companion object {
        internal fun create(reg: ModRegistrar): SyncService = physicalSided({ Server(reg) }, { Client(reg) })
    }

    // in principle there should be one such server sync service per server instance, but every existing implementation
    // of the game can only run one server instance simultaneously, so having just one is fine
    @ServerSide.Physical
    private class Server(reg: ModRegistrar) : SyncService(reg) {
        @PreludeInternal
        @ClientSide
        override fun registerClientSide(manager: SyncManager.Client, hostId: Int) {
            absurdLogicalSide()
        }

        @PreludeInternal
        @ClientSide
        override fun handleUpdateFromServer(hostId: Int, data: PacketBuffer) {
            absurdLogicalSide()
        }
    }

    @ClientSide.Physical
    private class Client(reg: ModRegistrar) : SyncService(reg) {
        @ClientSide
        private val remoteHosts: WeakValidityCache<Int, SyncManager.Client> =
            WeakValidityCache(Int2ObjectOpenHashMap(), 11)

        @ClientSide
        private val deliverBuffer: Int2ObjectMap<ArrayDeque<BufferedUpdate>> = Int2ObjectOpenHashMap()

        init {
            reg.on<FMLNetworkEvent.ClientDisconnectionFromServerEvent> @OptIn(SidednessAssertion::class) {
                assertLogicalClient {
                    resetClientState()
                }
            }
        }

        @ClientSide
        private fun resetClientState() {
            remoteHosts.clear()
            deliverBuffer.clear()
        }

        @ClientSide
        private fun scrubDeliverBuffer(cutoff: Long) {
            val iter = deliverBuffer.values.iterator()
            while (iter.hasNext()) {
                val updates = iter.next()
                while (true) {
                    if (updates.isEmpty()) {
                        iter.remove()
                        break
                    }
                    val update = updates.first()
                    if (update.receiveTime < cutoff) {
                        updates.removeFirst()
                    } else {
                        break // buffer should be monotonically increasing in receive time
                    }
                }
            }
        }

        @PreludeInternal
        @ClientSide
        override fun registerClientSide(manager: SyncManager.Client, hostId: Int) {
            remoteHosts[hostId] = manager
            deliverBuffer[hostId]?.let { updates ->
                updates.forEach {
                    manager.handleUpdateFromServer(PacketBuffer(Unpooled.wrappedBuffer(it.data)))
                }
                deliverBuffer.remove(hostId)
            }
        }

        @PreludeInternal
        @ClientSide
        override fun handleUpdateFromServer(hostId: Int, data: PacketBuffer) {
            remoteHosts[hostId]?.let {
                it.handleUpdateFromServer(data)
                return
            }
            val ttl = Prelude.config.networking.syncDeliverBufferTtl
            if (ttl == 0L) return
            val now = System.currentTimeMillis()
            scrubDeliverBuffer(now - ttl)
            deliverBuffer.getOrPut(hostId) { ArrayDeque() }.addLast(BufferedUpdate(data.readableToArray(), now))
        }

        @ClientSide
        private class BufferedUpdate(val data: ByteArray, val receiveTime: Long)
    }
}
