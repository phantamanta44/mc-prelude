package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.capability.ItemStoreHandler
import st.evening.mc.prelude.api.util.game.ItemKey

class ItemHandlerSlotStore(private val handler: IItemHandler, private val slotIndex: Int) : ItemStore {
    override val storedItem: ItemKey?
        get() = ItemKey.fromStack(getContentsAsStack())

    override val count: Int
        get() = getContentsAsStack().count

    override val capacity: Int
        get() = handler.getSlotLimit(slotIndex)

    override val isEmpty: Boolean
        get() = getContentsAsStack().isEmpty

    override fun getContentsAsStack(): ItemStack = handler.getStackInSlot(slotIndex)

    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack =
        handler.insertItem(slotIndex, stack, simulate)

    override fun extractItem(amount: Int, simulate: Boolean): ItemStack =
        handler.extractItem(slotIndex, amount, simulate)
}

fun IItemHandler.getOrWrapSlot(index: Int): ItemStore =
    (this as? ItemStoreHandler)?.getSlotHandler(index) ?: ItemHandlerSlotStore(this, index)
