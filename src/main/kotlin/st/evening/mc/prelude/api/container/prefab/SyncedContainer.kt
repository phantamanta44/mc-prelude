package st.evening.mc.prelude.api.container.prefab

import io.netty.buffer.Unpooled
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraft.inventory.IContainerListener
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.data.sync.SyncDispatcher
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.data.DelegateSuperFactory
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getAndBind
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.mergeStacksSafe
import st.evening.mc.prelude.api.util.game.onServer
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.mod.network.S2CBindSyncedContainer
import java.lang.invoke.MethodHandle

interface SyncedContainerIfc<C> : ShadowDelegate<C>, SyncDispatcher where C : Container, C : SyncHost {
    fun addListener(listener: IContainerListener)

    fun onContainerClosed(player: EntityPlayer)

    @OptIn(PreludeInternal::class)
    class Delegate<C>(override val self: C) : SyncedContainerIfc<C> where C : Container, C : SyncHost {
        companion object {
            private val factoryAddListener: DelegateSuperFactory = DelegateSuperFactory(
                SyncedContainerIfc::class.java,
                Container::class.java,
                "addListener",
                "func_75132_a",
                Void.TYPE,
                IContainerListener::class.java
            )
            private val factoryOnContainerClosed: DelegateSuperFactory = DelegateSuperFactory(
                SyncedContainerIfc::class.java,
                Container::class.java,
                "onContainerClosed",
                "func_75134_a",
                Void.TYPE,
                EntityPlayer::class.java
            )
        }

        private val superAddListener: MethodHandle = factoryAddListener.getAndBind(self)!!
        private val superOnContainerClosed: MethodHandle = factoryOnContainerClosed.getAndBind(self)!!

        @ServerSide
        private val listeningPlayers: MutableList<EntityPlayerMP> = mutableListOf()

        override var weakValidity: WeakValidity = WeakValidity.WEAK_VALID
            private set

        override fun addListener(listener: IContainerListener) {
            superAddListener(listener)
            onServer {
                if (listener is EntityPlayerMP) {
                    listeningPlayers += listener
                    val syncMgr = self.syncManager.getServer()
                    val data = PacketBuffer(Unpooled.buffer())
                    syncMgr.writeFullState(data)
                    Prelude.defns.s2cBindSyncedContainer.sendTo(
                        S2CBindSyncedContainer(self.windowId, syncMgr.hostId, data),
                        listener
                    )
                }
            }
        }

        override fun onContainerClosed(player: EntityPlayer) {
            superOnContainerClosed(player)
            onServer {
                if (listeningPlayers.remove(player) && listeningPlayers.isEmpty()) {
                    weakValidity = WeakValidity.INVALID
                }
            }
        }

        @ServerSide
        override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
            listeningPlayers.forEach { packetType.sendTo(message, it) }
        }
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
abstract class PrefabSyncedContainer<S : Piecewise>(override val syncData: S) : Container(), SyncHost, SyncContainer,
    SyncedContainerIfc<PrefabSyncedContainer<S>> by SyncedContainerIfc.Delegate(Magic.getThis()) {
    override val syncManager: SyncManager = SyncManager.create(this, syncData)

    override fun mergeItemStack(stack: ItemStack, startIndex: Int, endIndex: Int, reverseDirection: Boolean): Boolean =
        mergeStacksSafe(stack, startIndex, endIndex, reverseDirection)
}
