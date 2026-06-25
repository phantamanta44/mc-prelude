package st.evening.mc.prelude.api.capability

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

interface ItemStoreHandler : IItemHandler {
    fun getSlotHandler(index: Int): ItemStore

    override fun getStackInSlot(slot: Int): ItemStack = getSlotHandler(slot).getContentsAsStack()

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        getSlotHandler(slot).insertItem(stack, simulate)

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
        getSlotHandler(slot).extractItem(amount, simulate)

    override fun getSlotLimit(slot: Int): Int = getSlotHandler(slot).capacity

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = getSlotHandler(slot).isItemValid(stack)
}
