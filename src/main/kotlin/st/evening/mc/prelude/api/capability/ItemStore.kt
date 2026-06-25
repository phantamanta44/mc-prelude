package st.evening.mc.prelude.api.capability

import net.minecraft.item.ItemStack
import st.evening.mc.prelude.api.util.game.ItemKey

interface ItemStore {
    val storedItem: ItemKey?
    val count: Int
    val capacity: Int

    val isEmpty: Boolean
        get() = count <= 0

    fun getContentsAsStack(): ItemStack = storedItem?.newStack(count) ?: ItemStack.EMPTY

    fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack

    fun extractItem(amount: Int, simulate: Boolean): ItemStack

    fun isItemValid(key: ItemKey?): Boolean = isItemValid(key?.newStack(1) ?: ItemStack.EMPTY)

    fun isItemValid(stack: ItemStack): Boolean = true // likely to be the more-used overload

    object Empty : ModifiableItemStore {
        override val storedItem: ItemKey?
            get() = null
        override val count: Int
            get() = 0
        override val capacity: Int
            get() = 0
        override val isEmpty: Boolean
            get() = true

        override fun getContentsAsStack(): ItemStack = ItemStack.EMPTY

        override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack = stack

        override fun extractItem(amount: Int, simulate: Boolean): ItemStack = ItemStack.EMPTY

        override fun setItem(item: ItemKey?) {}

        override fun setItemCount(newCount: Int) {}

        override fun setContents(item: ItemKey?, amount: Int) {}

        override fun setContentsFromStack(stack: ItemStack) {}

        override fun isItemValid(key: ItemKey?): Boolean = false

        override fun isItemValid(stack: ItemStack): Boolean = false
    }
}

interface ModifiableItemStore : ItemStore {
    fun setItem(item: ItemKey?)

    fun setItemCount(newCount: Int)

    fun setContents(item: ItemKey?, amount: Int) {
        if (item == null || amount <= 0) {
            setItem(null)
        } else {
            setItem(item)
            setItemCount(amount)
        }
    }

    fun setContentsFromStack(stack: ItemStack) {
        if (stack.isEmpty) {
            setItem(null)
        } else {
            setContents(ItemKey.fromStack(stack), stack.count)
        }
    }
}
