package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.util.game.ItemKey

class RestrictedItemStore(
    private val delegate: ItemStore,
    private val allowInsert: Boolean = true,
    private val allowExtract: Boolean = true
) : ItemStore by delegate {
    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack =
        if (allowInsert) delegate.insertItem(stack, simulate) else stack

    override fun extractItem(amount: Int, simulate: Boolean): ItemStack =
        if (allowExtract) delegate.extractItem(amount, simulate) else ItemStack.EMPTY

    override fun isItemValid(key: ItemKey?): Boolean = allowInsert && delegate.isItemValid(key)

    override fun isItemValid(stack: ItemStack): Boolean = allowInsert && delegate.isItemValid(stack)
}
