package st.evening.mc.prelude.api.tileentity.prefab

import io.netty.buffer.Unpooled
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.observeStateGeneric
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.data.sync.SyncDispatcher
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.network.sendToAllTracking
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.data.DelegateSuperFactory
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.buildArrayFromPacketBuffer
import st.evening.mc.prelude.api.util.data.getAndBind
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.TileEntityStateObserver
import st.evening.mc.prelude.api.util.game.assertPhysicalClient
import st.evening.mc.prelude.api.util.game.getTileEntityWeakValidity
import st.evening.mc.prelude.api.util.game.onServer
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.api.util.world.onClient
import st.evening.mc.prelude.api.util.world.onServer
import java.lang.invoke.MethodHandle

interface TileEntitySyncedIfc<TE> : ShadowDelegate<TE>, SyncDispatcher where TE : TileEntity, TE : SyncHost {
    companion object {
        protected const val SER_SYNC_ID: String = $$"p$sid"
        protected const val SER_DATA: String = $$"p$data"
    }

    fun getUpdatePacket(): SPacketUpdateTileEntity

    fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity)

    fun getUpdateTag(): NBTTagCompound

    fun handleUpdateTag(tag: NBTTagCompound)

    @OptIn(PreludeInternal::class)
    class Delegate<TE>(override val self: TE) : TileEntitySyncedIfc<TE> where TE : TileEntity, TE : SyncHost {
        companion object {
            private val factoryGetUpdateTag: DelegateSuperFactory =
                superFactory("getUpdateTag", "func_189517_E_", NBTTagCompound::class.java)
            private val factoryHandleUpdateTag: DelegateSuperFactory =
                superFactory("handleUpdateTag", null, Void.TYPE, NBTTagCompound::class.java)

            private fun superFactory(
                name: String,
                deobfName: String?,
                returnType: Class<*>,
                vararg paramTypes: Class<*>
            ): DelegateSuperFactory = DelegateSuperFactory(
                TileEntitySyncedIfc::class.java, TileEntity::class.java, name, deobfName, returnType, *paramTypes
            )
        }

        private val superGetUpdateTag: MethodHandle = factoryGetUpdateTag.getAndBind(self)!!
        private val superHandleUpdateTag: MethodHandle = factoryHandleUpdateTag.getAndBind(self)!!

        override val weakValidity: WeakValidity
            get() = self.getTileEntityWeakValidity()

        @ServerSide
        override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
            packetType.sendToAllTracking(message, self)
        }

        override fun getUpdatePacket(): SPacketUpdateTileEntity = SPacketUpdateTileEntity(self.pos, 0, self.updateTag)

        @OptIn(SidednessAssertion::class)
        override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
            assertPhysicalClient {
                self.handleUpdateTag(pkt.nbtCompound)
            }
        }

        override fun getUpdateTag(): NBTTagCompound {
            val tag = superGetUpdateTag() as NBTTagCompound
            self.world.onServer {
                val syncMgr = self.syncManager.getServer()
                tag.runAction {
                    SER_SYNC_ID int syncMgr.hostId
                    SER_DATA byteArray buildArrayFromPacketBuffer { syncMgr.writeFullState(it) }
                }
            }
            return tag
        }

        override fun handleUpdateTag(tag: NBTTagCompound) {
            superHandleUpdateTag(tag)
            self.world.onClient {
                val syncMgr = self.syncManager.getClient()
                syncMgr.registerClientSide(tag.getInteger(SER_SYNC_ID))
                syncMgr.readFullState(PacketBuffer(Unpooled.wrappedBuffer(tag.getByteArray(SER_DATA))))
            }
        }
    }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER", "DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
abstract class PrefabTileEntitySynced<T, S, C> : TileEntity(), SyncHost, TileEntitySided<S, C>,
    TileEntitySidedIfc<S, C, PrefabTileEntitySynced<T, S, C>> by TileEntitySidedIfc.Delegate(Magic.getThis()),
    TileEntitySyncedIfc<PrefabTileEntitySynced<T, S, C>> by TileEntitySyncedIfc.Delegate(Magic.getThis())
    where T : SyncContainer, S : T, S : ServerSideSerializable, S : Observable, C : T {

    override val data: Sided<T, S, C> = initData()
    override val syncManager: SyncManager = SyncManager.create(this, data.get().syncData)

    override val self: PrefabTileEntitySynced<T, S, C>
        get() = this

    init {
        onServer {
            data.getServer().observeStateGeneric(TileEntityStateObserver(this))
        }
    }

    protected abstract fun initData(): Sided<T, S, C>
}
