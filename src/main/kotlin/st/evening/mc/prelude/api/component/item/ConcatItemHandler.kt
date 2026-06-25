package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable

open class InternalConcatItemHandler<T : IItemHandler> internal constructor(handlers: Iterable<T>) : IItemHandler {
    protected val delegateSlots: List<Pair<T, Int>> = handlers.flatMap { handler ->
        (0..<handler.slots).map { handler to it }
    }

    override fun getSlots(): Int = delegateSlots.size

    override fun getStackInSlot(slot: Int): ItemStack =
        delegateSlots[slot].let { (handler, i) -> handler.getStackInSlot(i) }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        delegateSlots[slot].let { (handler, i) -> handler.insertItem(i, stack, simulate) }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
        delegateSlots[slot].let { (handler, i) -> handler.extractItem(i, amount, simulate) }

    override fun getSlotLimit(slot: Int): Int =
        delegateSlots[slot].let { (handler, i) -> handler.getSlotLimit(i) }
}

class ConcatItemHandler(handlers: Iterable<IItemHandler>) : InternalConcatItemHandler<IItemHandler>(handlers) {
    companion object {
        fun of(vararg handlers: IItemHandler): ConcatItemHandler = ConcatItemHandler(handlers.asIterable())

        fun wrapMaybeModifiable(handlers: Iterable<IItemHandler>): IItemHandler {
            val handlerList = handlers.toList()
            return if (handlerList.all { it is IItemHandlerModifiable }) {
                // technically not sound, but should work under erasure
                @Suppress("UNCHECKED_CAST")
                ConcatItemHandlerModifiable(handlerList as List<IItemHandlerModifiable>)
            } else {
                ConcatItemHandler(handlerList)
            }
        }
    }
}

class ConcatItemHandlerModifiable(
    handlers: Iterable<IItemHandlerModifiable>
) : InternalConcatItemHandler<IItemHandlerModifiable>(handlers), IItemHandlerModifiable {
    companion object {
        fun of(vararg handlers: IItemHandlerModifiable): ConcatItemHandlerModifiable =
            ConcatItemHandlerModifiable(handlers.asIterable())
    }

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        delegateSlots[slot].let { (handler, i) -> handler.setStackInSlot(i, stack) }
    }
}
