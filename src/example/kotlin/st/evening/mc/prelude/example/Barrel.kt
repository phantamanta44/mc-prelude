package st.evening.mc.prelude.example

import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.EmptyHandler
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.block.prefab.BlockFacingIfc
import st.evening.mc.prelude.api.block.prefab.PrefabBlockFacing
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.component.item.ItemStoreItemHandler
import st.evening.mc.prelude.api.component.item.ObservableItemStore
import st.evening.mc.prelude.api.data.ser.IntSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.state.observeAll
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.tileentity.prefab.PrefabTileEntitySynced
import st.evening.mc.prelude.api.util.data.set
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ItemHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.sided
import st.evening.mc.prelude.api.util.math.ceilDivPos
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity

class BlockBarrel : PrefabBlockFacing(Material.WOOD), ITileEntityProvider {
    companion object {
        const val MAX_FILL_LEVEL: Int = 4

        val PROP_FILL: PropertyInteger = PropertyInteger.create("fill", 0, MAX_FILL_LEVEL)
    }

    init {
        setHardness(4F)
        setSoundType(SoundType.WOOD)
        defaultState = blockState.baseState
            .withProperty(BlockFacingIfc.PROP_FACING, EnumFacing.NORTH)
            .withProperty(PROP_FILL, 0)
    }

    override fun createBlockState(): BlockStateContainer =
        BlockStateContainer(this, BlockFacingIfc.PROP_FACING, PROP_FILL)

    @Deprecated("internal")
    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        world.useTileEntity<TileEntityBarrel>(pos) {
            return state.withProperty(PROP_FILL, it.fillLevel)
        }
        return state
    }


    override fun createNewTileEntity(world: World, meta: Int): TileEntity = TileEntityBarrel()

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (state.getValue(BlockFacingIfc.PROP_FACING) != facing) return false
        world.onServer {
            world.useTileEntity<TileEntityBarrel>(pos) { it.handleInsert(player, hand) }
        }
        return true
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        world.onServer {
            world.useTileEntity<TileEntityBarrel>(pos) { it.dropContents(world, pos) }
        }
        super.breakBlock(world, pos, state)
    }
}

class TileEntityBarrel :
    PrefabTileEntitySynced<TileEntityBarrel.State, TileEntityBarrel.Server, TileEntityBarrel.State>() {
    companion object {
        private const val SER_INVENTORY: String = "inventory"
    }

    open class State : SyncContainer {
        override val syncData: ValueStateAtom<Int> = ValueStateAtom(0, IntSerializer)
    }

    @ServerSide
    class Server : State(), NbtCompoundSerializable, Observable.Atom {
        val inventory: ObservableItemStore = ObservableItemStore(PreludeExample.config.barrelCapacity)
        val inventoryHandler: IItemHandler = ItemStoreItemHandler(inventory)

        init {
            inventory.observeAll(Observer.Simple.fixed {
                syncData.update(inventory.count)
            })
        }

        override fun observeState(observer: Observer.Simple) {
            inventory.observeState(observer)
        }

        override fun writeToNbt(dto: NBTTagCompound) {
            dto[SER_INVENTORY] = inventory.writeToNbt()
        }

        override fun readFromNbt(dto: NBTTagCompound) {
            inventory.readFromNbt(dto.getCompoundTag(SER_INVENTORY))
        }
    }

    override fun initData(): Sided<State, Server, State> = Sided.of({ Server() }, { State() })

    init {
        data.get().syncData.observeSync(Observer.Simple.fixed {
            world.markBlockRangeForRenderUpdate(pos, pos)
        })
    }

    val fillLevel: Int
        get() = (data.get().syncData.value * BlockBarrel.MAX_FILL_LEVEL ceilDivPos PreludeExample.config.barrelCapacity)
            .coerceIn(0, BlockBarrel.MAX_FILL_LEVEL)

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = when (capability) {
        Prelude.defns.capItemStore, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> true
        else -> super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(capability: Capability<T?>, facing: EnumFacing?): T? = when (capability) {
        Prelude.defns.capItemStore -> Prelude.defns.capItemStore.cast(
            sided({ data.getServer().inventory }, { ItemStore.Empty })
        )

        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(
            sided({ data.getServer().inventoryHandler }, { EmptyHandler.INSTANCE })
        )

        else -> super.getCapability(capability, facing)
    }

    @ServerSide
    fun handleInsert(player: EntityPlayer, hand: EnumHand) {
        val heldStack = player.getHeldItem(hand)
        if (heldStack.isEmpty) return
        val inventory = data.getServer().inventory
        val remStack = inventory.insertItem(heldStack, false)
        if (remStack === heldStack) return // equality of reference!
        player.setHeldItem(hand, remStack)
    }

    @ServerSide
    fun handleExtract(player: EntityPlayer) {
        val inventory = data.getServer().inventory
        if (inventory.isEmpty) return
        val stack = inventory.extractItem(if (player.isSneaking) ItemHelper.defaultStackCapacity else 1, false)
        if (stack.isEmpty) return
        player.inventory.placeItemBackInInventory(world, stack)
    }

    @ServerSide
    fun dropContents(world: World, pos: BlockPos) {
        InvHelper.dropItems(data.getServer().inventory, world, pos)
    }
}
