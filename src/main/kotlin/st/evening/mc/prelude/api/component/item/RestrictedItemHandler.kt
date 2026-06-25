package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandlerModifiable

class RestrictedItemHandler(private val delegate: IItemHandlerModifiable) : IItemHandlerModifiable by delegate {
    private val insertBlacklist: BooleanArray = BooleanArray(delegate.slots)
    private val extractBlacklist: BooleanArray = BooleanArray(delegate.slots)

    fun noInsert(slotIndex: Int): RestrictedItemHandler {
        insertBlacklist[slotIndex] = true
        return this
    }

    fun noExtract(slotIndex: Int): RestrictedItemHandler {
        extractBlacklist[slotIndex] = true
        return this
    }

    fun noAccess(slotIndex: Int): RestrictedItemHandler {
        insertBlacklist[slotIndex] = true
        extractBlacklist[slotIndex] = true
        return this
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        if (insertBlacklist[slot]) stack else delegate.insertItem(slot, stack, simulate)

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
        if (extractBlacklist[slot]) ItemStack.EMPTY else delegate.extractItem(slot, amount, simulate)

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean =
        !insertBlacklist[slot] && delegate.isItemValid(slot, stack)
}
