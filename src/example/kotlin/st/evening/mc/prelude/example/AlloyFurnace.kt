package st.evening.mc.prelude.example

import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.SlotItemHandler
import net.minecraftforge.items.wrapper.EmptyHandler
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.block.prefab.PrefabBlockSided
import st.evening.mc.prelude.api.component.energy.EmptyEnergyStorage
import st.evening.mc.prelude.api.component.energy.ObservableEnergyStorage
import st.evening.mc.prelude.api.component.energy.RatedEnergyStorage
import st.evening.mc.prelude.api.component.item.ObservableItemHandler
import st.evening.mc.prelude.api.component.item.RestrictedItemHandler
import st.evening.mc.prelude.api.container.prefab.PrefabEngineGuiContainer
import st.evening.mc.prelude.api.container.prefab.PrefabSyncedContainer
import st.evening.mc.prelude.api.data.ser.BoolSerializer
import st.evening.mc.prelude.api.data.ser.IntSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.ser.NbtSerializer
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.state.ListObservableComposite
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.state.VirtualQueuedObservableAtom
import st.evening.mc.prelude.api.data.state.observeAll
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableRect
import st.evening.mc.prelude.api.gui.drawable.prefab.PreludeDrawables
import st.evening.mc.prelude.api.gui.engine.LayoutEngine
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.BackgroundBox
import st.evening.mc.prelude.api.gui.engine.prefab.ColumnLayout
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.PaddingBox
import st.evening.mc.prelude.api.gui.engine.prefab.ProgressBarDisplay
import st.evening.mc.prelude.api.gui.engine.prefab.RowLayout
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TableLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TextDisplay
import st.evening.mc.prelude.api.gui.engine.prefab.TooltipBox
import st.evening.mc.prelude.api.gui.engine.setChild
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.registration.openContainer
import st.evening.mc.prelude.api.sound.LerpingPersistentSound
import st.evening.mc.prelude.api.tileentity.prefab.PrefabTileEntitySynced
import st.evening.mc.prelude.api.util.data.BitwiseHelper
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.hasCompound
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.mergeStacksSafe
import st.evening.mc.prelude.api.util.game.sided
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawJustification
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import st.evening.mc.prelude.api.util.text.toStringSI
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.findTileEntity
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity

class BlockAlloyFurnace : PrefabBlockSided(Material.IRON), ITileEntityProvider {
    companion object {
        val PROP_ACTIVE: PropertyBool = PropertyBool.create("active")
    }

    init {
        setHardness(4F)
        setSoundType(SoundType.METAL)
        defaultState = blockState.baseState
            .withProperty(BlockSidedIfc.PROP_FACING, BlockSide.NORTH)
            .withProperty(PROP_ACTIVE, false)
    }

    override fun createBlockState(): BlockStateContainer =
        BlockStateContainer(this, BlockSidedIfc.PROP_FACING, PROP_ACTIVE)

    @Deprecated("internal")
    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        world.useTileEntity<TileEntityAlloyFurnace>(pos) {
            return state.withProperty(PROP_ACTIVE, it.active)
        }
        return state
    }

    override fun createNewTileEntity(world: World, meta: Int): TileEntity = TileEntityAlloyFurnace()

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        world.onServer {
            player.openContainer(PreludeExample.defns.containers.alloyFurnace, world, pos)
        }
        return true
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        world.onServer {
            world.useTileEntity<TileEntityAlloyFurnace>(pos) { it.dropContents(world, pos) }
        }
        super.breakBlock(world, pos, state)
    }
}

class AlloyFurnaceRecipe(val ing1: Ingredient, val ing2: Ingredient, val output: ItemStack, val duration: Int) {
    companion object {
        private val registry: MutableCollection<AlloyFurnaceRecipe> = mutableSetOf()

        fun register(recipe: AlloyFurnaceRecipe) {
            registry += recipe
        }

        fun findRecipe(input1: ItemStack, input2: ItemStack): AlloyFurnaceRecipe? =
            registry.firstOrNull { it.matches(input1, input2) }
    }

    fun matches(input1: ItemStack, input2: ItemStack): Boolean =
        (ing1.apply(input1) && ing2.apply(input2)) || (ing2.apply(input1) && ing1.apply(input2))
}

class TileEntityAlloyFurnace :
    PrefabTileEntitySynced<TileEntityAlloyFurnace.State, TileEntityAlloyFurnace.Server, TileEntityAlloyFurnace.Client>(),
    ITickable {
    companion object {
        const val SLOT_IN1: Int = 0
        const val SLOT_IN2: Int = 1
        const val SLOT_OUT: Int = 2
        const val SLOT_COUNT: Int = 3

        fun newInventory(): ObservableItemHandler = ObservableItemHandler.nSlots(SLOT_COUNT)

        fun restrictInventory(inventory: IItemHandlerModifiable): IItemHandlerModifiable =
            RestrictedItemHandler(inventory).noInsert(SLOT_OUT)

        private const val SER_INVENTORY: String = "inventory"
        private const val SER_ENERGY: String = "energy"
        private const val SER_RECIPE: String = "recipe"
    }

    sealed class State : SyncContainer {
        override val syncData: ValueStateAtom<Boolean> = ValueStateAtom(false, BoolSerializer)

        open fun setPos(pos: BlockPos) {}
        open fun onInvalidate() {}
        open fun tick() {}
    }

    @ServerSide
    class Server(state: ListObservableComposite.Builder = ListObservableComposite.Builder()) :
        State(), NbtCompoundSerializable, Observable.Composite by state.build() {

        val inventoryInternal: ObservableItemHandler = state part newInventory()
        val inventory: IItemHandler = restrictInventory(inventoryInternal)
        val energyInternal: ObservableEnergyStorage =
            state part ObservableEnergyStorage(PreludeExample.config.alloyFurnaceEnergyCapacity)
        val energy: IEnergyStorage =
            RatedEnergyStorage(energyInternal, PreludeExample.config.alloyFurnaceEnergyInputRate, 0)
        private var runningRecipe: RunningRecipe? = null
        val recipeState: VirtualQueuedObservableAtom = state part VirtualQueuedObservableAtom()

        val currentWork: Int
            get() = runningRecipe?.progress ?: 0
        val maxWork: Int
            get() = runningRecipe?.recipe?.duration ?: 0

        override fun tick() {
            val running = runningRecipe
            if (running != null) {
                tickRecipe(running)
            } else {
                trySetRecipe(null)
            }
            recipeState.flush()
        }

        private fun trySetRecipe(checkFirst: AlloyFurnaceRecipe?): Boolean {
            val inStack1 = inventoryInternal.getStackInSlot(SLOT_IN1)
            if (inStack1.isEmpty) return false
            val inStack2 = inventoryInternal.getStackInSlot(SLOT_IN2)
            if (inStack2.isEmpty) return false
            if (checkFirst != null && checkFirst.matches(inStack1, inStack2)) {
                setRecipe(checkFirst)
                return true
            }
            val recipe = AlloyFurnaceRecipe.findRecipe(inStack1, inStack2) ?: return false
            setRecipe(recipe)
            return true
        }

        private fun setRecipe(recipe: AlloyFurnaceRecipe) {
            runningRecipe = RunningRecipe(
                recipe,
                inventoryInternal.extractItem(SLOT_IN1, 1, false),
                inventoryInternal.extractItem(SLOT_IN2, 1, false)
            )
            recipeState.notifyObservers()
        }

        private fun tickRecipe(running: RunningRecipe) {
            val recipe = running.recipe
            if (running.progress >= recipe.duration) {
                tryFinishRecipe(recipe)
                return
            }

            val cost = PreludeExample.config.alloyFurnaceEnergyCost
            if (energyInternal.energyStored < cost || energyInternal.extractEnergy(cost, false) < cost) {
                syncData.update(false)
                return
            }

            recipeState.notifyObservers()
            running.progress++
            if (running.progress >= recipe.duration) {
                syncData.update(tryFinishRecipe(recipe))
            } else {
                syncData.update(true)
            }
        }

        private fun tryFinishRecipe(recipe: AlloyFurnaceRecipe): Boolean {
            if (!inventoryInternal.insertItem(SLOT_OUT, recipe.output, true).isEmpty) return false
            inventoryInternal.insertItem(SLOT_OUT, recipe.output, false)
            runningRecipe = null
            recipeState.notifyObservers()
            return trySetRecipe(recipe)
        }

        fun dropContents(world: World, pos: BlockPos) {
            InvHelper.dropItems(inventoryInternal, world, pos)
            runningRecipe?.let {
                InvHelper.dropItem(it.input1, world, pos)
                InvHelper.dropItem(it.input2, world, pos)
            }
        }

        override fun writeToNbt(dto: NBTTagCompound) {
            dto.runAction {
                SER_INVENTORY tag inventoryInternal.writeToNbt()
                SER_ENERGY tag energyInternal.writeToNbt()
                runningRecipe?.let { SER_RECIPE tag RunningRecipe.Serializer.serializeToNbt(it) }
            }
        }

        override fun readFromNbt(dto: NBTTagCompound) {
            inventoryInternal.readFromNbt(dto.getTagList(SER_INVENTORY, Constants.NBT.TAG_COMPOUND))
            energyInternal.readFromNbt(dto.getCompoundTag(SER_ENERGY))
            runningRecipe = orNull(dto.hasCompound(SER_RECIPE)) {
                RunningRecipe.Serializer.deserializeFromNbt(dto.getCompoundTag(SER_RECIPE))
            }
        }

        private class RunningRecipe(val recipe: AlloyFurnaceRecipe, val input1: ItemStack, val input2: ItemStack) {
            companion object {
                private const val SER_INPUT_1: String = "input1"
                private const val SER_INPUT_2: String = "input2"
                private const val SER_PROGRESS: String = "progress"
            }

            var progress: Int = 0

            object Serializer : NbtSerializer<RunningRecipe, NBTTagCompound> {
                override fun serializeToNbt(x: RunningRecipe): NBTTagCompound = NbtDsl.compound {
                    SER_INPUT_1 tag x.input1.serializeNBT()
                    SER_INPUT_2 tag x.input2.serializeNBT()
                    SER_PROGRESS int x.progress
                }

                override fun deserializeFromNbt(dto: NBTTagCompound): RunningRecipe {
                    val input1 = ItemStack(dto.getCompoundTag(SER_INPUT_1))
                    val input2 = ItemStack(dto.getCompoundTag(SER_INPUT_2))
                    val recipe = AlloyFurnaceRecipe.findRecipe(input1, input2)
                        ?: throw SerializationException("No recipe for inputs: $input1 + $input2")
                    return RunningRecipe(recipe, input1, input2).also { it.progress = dto.getInteger(SER_PROGRESS) }
                }
            }
        }
    }

    @ClientSide.Strong
    class Client(host: TileEntityAlloyFurnace) : State() {
        val workSound: LerpingPersistentSound = LerpingPersistentSound(
            host,
            PreludeExample.defns.sounds.alloyFurnaceWorking,
            SoundCategory.BLOCKS
        )

        init {
            Minecraft.getMinecraft().soundHandler.playSound(workSound)
            syncData.observeAll(Observer.Simple.fixed {
                if (syncData.value) {
                    workSound.targetVolume = 1F
                    workSound.targetPitch = 1F
                } else {
                    workSound.targetVolume = 0F
                    workSound.targetPitch = 0.5F
                }
            })
        }

        override fun setPos(pos: BlockPos) {
            workSound.setPosition(pos)
        }

        override fun onInvalidate() {
            workSound.volume = 0F
        }
    }

    override fun initData(): Sided<State, Server, Client> = Sided.ofStrong({ Server() }, { Client(this) })

    init {
        data.get().syncData.observeSync(Observer.Simple.fixed {
            world.markBlockRangeForRenderUpdate(pos, pos)
        })
    }

    val active: Boolean
        get() = data.get().syncData.value

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, CapabilityEnergy.ENERGY -> true
        else -> super.hasCapability(capability, facing)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(
            sided({ data.getServer().inventory }, { EmptyHandler.INSTANCE })
        )

        CapabilityEnergy.ENERGY -> CapabilityEnergy.ENERGY.cast(
            sided({ data.getServer().energy }, { EmptyEnergyStorage })
        )

        else -> super.getCapability(capability, facing)
    }

    override fun setPos(posIn: BlockPos) {
        super.setPos(posIn)
        data.get().setPos(pos)
    }

    override fun invalidate() {
        super.invalidate()
        data.get().onInvalidate()
    }

    override fun update() {
        data.get().tick()
    }

    @ServerSide
    fun dropContents(world: World, pos: BlockPos) {
        data.getServer().dropContents(world, pos)
    }
}

sealed class ContainerAlloyFurnace(playerInv: InventoryPlayer, furnaceInventory: IItemHandler, syncData: State) :
    PrefabSyncedContainer<ContainerAlloyFurnace.State>(syncData) {
    init {
        addSlotToContainer(SlotItemHandler(furnaceInventory, TileEntityAlloyFurnace.SLOT_IN1, 0, 0))
        addSlotToContainer(SlotItemHandler(furnaceInventory, TileEntityAlloyFurnace.SLOT_IN2, 0, 0))
        addSlotToContainer(SlotItemHandler(furnaceInventory, TileEntityAlloyFurnace.SLOT_OUT, 0, 0))
        for (i in 0..<36) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
    }

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack =
        InvHelper.transferStacks(inventorySlots[index], player) { stack ->
            if (index < TileEntityAlloyFurnace.SLOT_COUNT) {
                mergeStacksSafe(stack, TileEntityAlloyFurnace.SLOT_COUNT, TileEntityAlloyFurnace.SLOT_COUNT + 36, true)
            } else {
                mergeStacksSafe(stack, 0, TileEntityAlloyFurnace.SLOT_COUNT - 1, false) // skip the output slot
            }
        }

    class State(
        currentWork: Int,
        maxWork: Int,
        energyStored: Int,
        state: ListStateComposite.Builder = ListStateComposite.Builder()
    ) : Piecewise.Composite by state.build() {
        val currentWork: ValueStateAtom<Int> = state part ValueStateAtom(currentWork, IntSerializer)
        val maxWork: ValueStateAtom<Int> = state part ValueStateAtom(maxWork, IntSerializer)
        val energyStored: ValueStateAtom<Int> = state part ValueStateAtom(energyStored, IntSerializer)
    }

    @ServerSide
    class Server(
        playerInv: InventoryPlayer,
        private val furnace: TileEntityAlloyFurnace,
        data: TileEntityAlloyFurnace.Server
    ) : ContainerAlloyFurnace(
        playerInv,
        data.inventory,
        State(data.currentWork, data.maxWork, data.energy.energyStored)
    ) {
        init {
            val data = furnace.data.getServer()
            data.recipeState.observeState(Observer.Simple.proxy(this) {
                syncData.currentWork.update(data.currentWork)
                syncData.maxWork.update(data.maxWork)
            })
            val energy = data.energyInternal
            energy.observeState(Observer.Simple.proxy(this) {
                syncData.energyStored.update(energy.energyStored)
            })
        }

        override fun canInteractWith(player: EntityPlayer): Boolean = player.getDistanceSq(furnace.pos) <= 64.0
    }

    @ClientSide
    class Client(playerInv: InventoryPlayer) : ContainerAlloyFurnace(
        playerInv,
        TileEntityAlloyFurnace.restrictInventory(TileEntityAlloyFurnace.newInventory()),
        State(0, 0, 0)
    ) {
        override fun canInteractWith(playerIn: EntityPlayer): Boolean = true
    }

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            world.findTileEntity<TileEntityAlloyFurnace>(x, y, z)?.let {
                Server(player.inventory, it, it.data.getServer())
            }

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer =
            GuiAlloyFurnace(player.inventory)
    }
}

@ClientSide.Strong
class GuiAlloyFurnace(playerInv: InventoryPlayer) :
    PrefabEngineGuiContainer<ContainerAlloyFurnace>(ContainerAlloyFurnace.Client(playerInv)) {
    override val layoutEngine: LayoutEngine = LayoutEngine.fromDsl(
        ColumnLayout(-1, -1),
        { BackgroundBox(PaddingBox.all(it, 7), PreludeDrawables.windowBg) }
    ) {
        addChild(TextDisplay.fromI18n("${PreludeExample.modId}.gui.alloy_furnace.name"))
        addChild(StackLayout()) {
            addChild(
                RowLayout(justifyX = DrawJustification.CENTER, alignY = DrawAlignment.CENTER, minGap = 4),
                wrapper = { PaddingBox.all(it, 24) }
            ) {
                addChild(InventorySlot(TileEntityAlloyFurnace.SLOT_IN1))
                addChild(InventorySlot(TileEntityAlloyFurnace.SLOT_IN2))
                addChild(
                    ProgressBarDisplay.fromMax(
                        PreludeDrawables.progressBg,
                        PreludeDrawables.progressFg,
                        { container.syncData.currentWork.value.toFloat() },
                        { container.syncData.maxWork.value.toFloat() }
                    ),
                    wrapper = {
                        TooltipBox(it) {
                            val data = container.syncData
                            return@TooltipBox listOf("${data.currentWork.value} / ${data.maxWork.value}")
                        }
                    }
                )
                addChild(InventorySlot(TileEntityAlloyFurnace.SLOT_OUT))
            }
            addChild(
                ProgressBarDisplay.fromMax(
                    PreludeDrawables.slot.resize(4, 54),
                    DrawableRect(2, 52, 0xFD0000 or BitwiseHelper.FF000000),
                    { container.syncData.energyStored.value.toFloat() },
                    { PreludeExample.config.alloyFurnaceEnergyCapacity.toFloat() },
                    DrawOrientation.BOTTOM_TO_TOP,
                    1, 1, 1, 1
                ),
                alignX = DrawAlignment.END,
                wrapper = {
                    TooltipBox(it) {
                        val current = container.syncData.energyStored.value.toStringSI("FE")
                        val max = PreludeExample.config.alloyFurnaceEnergyCapacity.toStringSI("FE")
                        return@TooltipBox listOf("$current / $max")
                    }
                }
            )
        }
        addChild(TextDisplay.constant(playerInv.displayName))
        addChild(TableLayout(4, 9, -1, -1)) {
            configure {
                configureRow(3, startPadding = 4)
            }
            for (i in 0..<3) {
                for (j in 0..<9) {
                    setChild(i, j, InventorySlot(TileEntityAlloyFurnace.SLOT_COUNT + 9 + i * 9 + j))
                }
            }
            for (i in 0..<9) {
                setChild(3, i, InventorySlot(TileEntityAlloyFurnace.SLOT_COUNT + i))
            }
        }
    }
}
