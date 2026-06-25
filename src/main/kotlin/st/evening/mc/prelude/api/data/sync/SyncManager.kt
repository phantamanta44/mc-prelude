package st.evening.mc.prelude.api.data.sync

import io.netty.buffer.Unpooled
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.asComposite
import st.evening.mc.prelude.api.data.state.forEachStateAtom
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.util.collection.WeakValidityProxy
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.data.BitVector
import st.evening.mc.prelude.api.util.data.readAndIterateBitVector
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.absurdLogicalSide
import st.evening.mc.prelude.api.util.game.sided
import st.evening.mc.prelude.mod.network.S2CSyncData

interface SyncDispatcher : WeaklyValid {
    @ServerSide
    fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M)
}

interface SyncHost : SyncDispatcher {
    val syncManager: SyncManager
}

interface SyncContainer {
    val syncData: Piecewise
}

sealed class SyncManager(protected val host: SyncHost, state: Piecewise) :
    Sided<SyncManager, SyncManager.Server, SyncManager.Client>, WeaklyValid by WeakValidityProxy(host) {

    protected val cells: Piecewise.Composite = state.asComposite()

    override fun get(): SyncManager = this

    companion object {
        fun create(host: SyncHost, state: Piecewise): SyncManager =
            sided({ Server(host, state) }, { Client(host, state) })
    }

    @ServerSide
    class Server(host: SyncHost, state: Piecewise) : SyncManager(host, state), Observer.Indexed {
        private val dirtyState: BooleanArray = BooleanArray(cells.stateSize)

        @OptIn(PreludeInternal::class)
        val hostId: Int = Prelude.defns.syncService.registerServerSide(this)

        init {
            cells.observeState(this)
        }

        @ServerSide
        override fun getServer(): Server = this

        @ClientSide
        override fun getClient(): Client = absurdLogicalSide()

        fun writeFullState(buf: PacketBuffer) {
            cells.forEachStateAtom { it.writeToNetwork(buf) }
        }

        override fun onObservableUpdate(index: Int) {
            dirtyState[index] = true
            Prelude.defns.syncService.enqueueForSync(this)
        }

        fun dispatchSync() {
            val mask = BitVector(cells.stateSize)
            val buf = PacketBuffer(Unpooled.buffer(mask.octetCount))
            dirtyState.forEachIndexed { index, dirty ->
                if (dirty) {
                    mask.set(index)
                }
            }
            mask.writeToNetwork(buf)
            dirtyState.forEachIndexed { index, dirty ->
                if (dirty) {
                    cells.getStateAtom(index).writeToNetwork(buf)
                }
            }
            host.dispatchSync(Prelude.defns.s2cSyncData, S2CSyncData(hostId, buf))
            dirtyState.fill(false)
        }
    }

    @ClientSide
    class Client(host: SyncHost, state: Piecewise) : SyncManager(host, state) {
        var hostId: Int? = null

        @ServerSide
        override fun getServer(): Server = absurdLogicalSide()

        @ClientSide
        override fun getClient(): Client = this

        @OptIn(PreludeInternal::class)
        fun registerClientSide(hostId: Int) {
            val currentHostId = this.hostId
            if (currentHostId == null) {
                this.hostId = hostId
                Prelude.defns.syncService.registerClientSide(this, hostId)
            } else if (currentHostId != hostId) {
                throw IllegalStateException("Sync host with ID $currentHostId re-registered with different ID: $hostId")
            }
        }

        fun readFullState(buf: PacketBuffer) {
            cells.forEachStateAtom { it.readFromNetwork(buf) }
        }

        fun handleUpdateFromServer(buf: PacketBuffer) {
            buf.readAndIterateBitVector(cells.stateSize) {
                cells.getStateAtom(it).readFromNetwork(buf)
            }
        }
    }
}
