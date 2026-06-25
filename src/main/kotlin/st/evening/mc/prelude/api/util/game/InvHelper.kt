package st.evening.mc.prelude.api.util.game

import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.util.system.asAccessible
import kotlin.math.min

object InvHelper { // avoid name clash with net.minecraft.inventory.InventoryHelper
    val emptyInventory: IInventory =
        SlotItemHandler::class.java.getDeclaredField("emptyInventory").asAccessible().get(null) as IInventory

    fun dropItem(stack: ItemStack, world: World, x: Double, y: Double, z: Double) {
        val entity = EntityItem(
            world,
            x - 0.25 + world.rand.nextDouble() * 0.5,
            y - 0.25 + world.rand.nextDouble() * 0.5,
            z - 0.25 + world.rand.nextDouble() * 0.5,
            stack
        )
        entity.motionX = world.rand.nextGaussian() * 0.05
        entity.motionY = world.rand.nextGaussian() * 0.05 + 0.2
        entity.motionZ = world.rand.nextGaussian() * 0.05
        entity.setPickupDelay(8)
        world.spawnEntity(entity)
    }

    fun dropItem(stack: ItemStack, world: World, pos: Vec3d) {
        dropItem(stack, world, pos.x, pos.y, pos.z)
    }

    fun dropItem(stack: ItemStack, world: World, pos: BlockPos) {
        dropItem(stack, world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    }

    fun dropItems(inventory: IItemHandler, world: World, x: Double, y: Double, z: Double) {
        inventory.forEachSlot {
            dropItem(inventory.getStackInSlot(it), world, x, y, z)
        }
    }

    fun dropItems(inventory: IItemHandler, world: World, pos: Vec3d) {
        dropItems(inventory, world, pos.x, pos.y, pos.z)
    }

    fun dropItems(inventory: IItemHandler, world: World, pos: BlockPos) {
        dropItems(inventory, world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    }

    fun dropItems(inventory: ItemStore, world: World, x: Double, y: Double, z: Double) {
        var count = inventory.count
        if (count <= 0) return
        val stack = (inventory.storedItem ?: return).newStack(1)
        val maxStackSize = stack.maxStackSize
        stack.count = maxStackSize
        while (count >= maxStackSize) {
            dropItem(stack.copy(), world, x, y, z)
            count -= maxStackSize
        }
        if (count > 0) {
            stack.count = count
            dropItem(stack, world, x, y, z)
        }
    }

    fun dropItems(inventory: ItemStore, world: World, pos: Vec3d) {
        dropItems(inventory, world, pos.x, pos.y, pos.z)
    }

    fun dropItems(inventory: ItemStore, world: World, pos: BlockPos) {
        dropItems(inventory, world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    }

    inline fun transferStacks(
        clickedSlot: Slot,
        player: EntityPlayer,
        tryMergeStack: (ItemStack) -> Boolean
    ): ItemStack {
        if (!clickedSlot.hasStack) return ItemStack.EMPTY

        val stack = clickedSlot.stack
        val orig = stack.copy()
        if (!tryMergeStack(stack)) return ItemStack.EMPTY

        if (stack.isEmpty) {
            clickedSlot.putStack(ItemStack.EMPTY)
        } else {
            clickedSlot.onSlotChanged()
        }
        if (orig.count == stack.count) return ItemStack.EMPTY
        clickedSlot.onTake(player, stack)
        return orig
    }
}

fun Container.mergeStacksSafe(stack: ItemStack, startIndex: Int, endIndex: Int, reverse: Boolean): Boolean {
    if (stack.isEmpty) return false
    var didWork = false

    val initialIndex: Int
    val stopIndex: Int
    val di: Int
    if (reverse) {
        initialIndex = endIndex - 1
        stopIndex = startIndex - 1
        di = -1
    } else {
        initialIndex = startIndex
        stopIndex = endIndex
        di = 1
    }
    var i = initialIndex

    // first pass: merge into existing stacks
    if (stack.isStackable) {
        while (i != stopIndex) {
            val slot = inventorySlots[i]
            val slotStack = slot.stack
            if (!slotStack.isEmpty && stack.stacksWith(slotStack) && slot.isItemValid(stack)) {
                val toTransfer = stack.count
                    .coerceAtMost(min(slot.slotStackLimit, slotStack.maxStackSize) - slotStack.count)
                if (toTransfer > 0) {
                    stack.shrink(toTransfer)
                    slot.putStack(slotStack.copyWithSize(slotStack.count + toTransfer))
                    if (stack.isEmpty) return true
                    didWork = true
                }
            }
            i += di
        }
        i = initialIndex
    }

    // second pass: merge into empty slots
    while (i != stopIndex) {
        val slot = inventorySlots[i]
        if (slot.stack.isEmpty && slot.isItemValid(stack)) {
            val slotLimit = slot.slotStackLimit
            if (stack.count > slotLimit) { // note: allows transfer of entire oversized stacks
                slot.putStack(stack.splitStack(slotLimit))
                didWork = true
            } else {
                slot.putStack(stack.copy())
                stack.count = 0
                return true
            }
        }
        i += di
    }
    return didWork
}

val IItemHandler.slotIndices: IntRange
    get() = 0..<slots

inline fun IItemHandler.forEachSlot(f: (Int) -> Unit) {
    for (i in 0..<slots) {
        f(i)
    }
}

fun IItemHandler.isEmpty(): Boolean {
    forEachSlot {
        if (!getStackInSlot(it).isEmpty) return false
    }
    return true
}

fun IInventory.computeComparatorValue(): Int = Container.calcRedstoneFromInventory(this)

fun IItemHandler.computeComparatorValue(): Int = ItemHandlerHelper.calcRedstoneFromInventory(this)

fun ItemStore.computeComparatorValue(): Int {
    val n = count
    return if (n > 0) 1 + 14 * n / capacity else 0
}
