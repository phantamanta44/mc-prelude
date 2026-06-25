package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler
import st.evening.mc.prelude.api.capability.ItemStore

class ItemStoreItemHandler(private val store: ItemStore) : IItemHandler {
    companion object {
        const val INSERT_SLOT: Int = 0
        const val EXTRACT_SLOT: Int = 1
    }

    override fun getSlots(): Int = 2

    override fun getStackInSlot(slot: Int): ItemStack = when (slot) {
        INSERT_SLOT -> ItemStack.EMPTY
        // count could be way bigger than the max stack size; hopefully other mods can handle it
        EXTRACT_SLOT -> store.getContentsAsStack()
        else -> throw IndexOutOfBoundsException(slot.toString())
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = when (slot) {
        INSERT_SLOT -> store.insertItem(stack, simulate)
        EXTRACT_SLOT -> stack
        else -> throw IndexOutOfBoundsException(slot.toString())
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = when (slot) {
        INSERT_SLOT -> ItemStack.EMPTY
        EXTRACT_SLOT -> store.extractItem(amount, simulate)
        else -> throw IndexOutOfBoundsException(slot.toString())
    }

    override fun getSlotLimit(slot: Int): Int = store.capacity
}
