package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.ser.ItemStackSerializer
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.game.ItemHelper
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.copyWithSize
import st.evening.mc.prelude.api.util.game.stacksWith
import kotlin.math.min

open class ObservableItemSlot(override val capacity: Int = ItemHelper.defaultStackCapacity) :
    ItemStore, Piecewise.Atom, GameSerializable<NBTTagCompound> {

    private var storedStack: ItemStack = ItemStack.EMPTY
        set(newStack) {
            field = newStack
            storedItem = ItemKey.fromStack(newStack)
        }

    override var storedItem: ItemKey? = null
        protected set
    override val count: Int
        get() = storedStack.count
    override val isEmpty: Boolean
        get() = storedStack.isEmpty

    private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
    private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    override fun getContentsAsStack(): ItemStack = storedStack

    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) return stack
        val slotLimit = min(stack.maxStackSize, capacity)

        val inSlot = storedStack
        if (inSlot.isEmpty) {
            val toTransfer = min(stack.count, slotLimit)
            if (toTransfer <= 0) return stack
            if (!simulate) {
                val newInSlot = stack.copyWithSize(toTransfer)
                storedStack = newInSlot
                stateObservers.onObservableUpdate()
            }
            return stack.copyWithSize(stack.count - toTransfer)
        }

        if (!inSlot.stacksWith(stack)) return stack
        val toTransfer = min(stack.count, slotLimit - inSlot.count)
        if (toTransfer <= 0) return stack
        if (!simulate) {
            inSlot.grow(toTransfer)
            stateObservers.onObservableUpdate()
        }
        return stack.copyWithSize(stack.count - toTransfer)
    }

    override fun extractItem(amount: Int, simulate: Boolean): ItemStack {
        if (amount <= 0) return ItemStack.EMPTY

        val inSlot = storedStack
        if (inSlot.isEmpty) return ItemStack.EMPTY
        if (amount < inSlot.count) {
            if (!simulate) {
                inSlot.shrink(amount)
                stateObservers.onObservableUpdate()
            }
            return inSlot.copyWithSize(amount)
        } else if (simulate) {
            return inSlot.copy()
        } else {
            storedStack = ItemStack.EMPTY
            stateObservers.onObservableUpdate()
            return inSlot
        }
    }

    fun setItem(stack: ItemStack) {
        storedStack = stack
        stateObservers.onObservableUpdate()
    }

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    @PreludeInternal
    fun readEmpty() {
        storedStack = ItemStack.EMPTY
        syncObservers.onObservableUpdate()
    }

    override fun writeToNbt(): NBTTagCompound = ItemStackSerializer.serializeToNbt(storedStack)

    override fun readFromNbt(dto: NBTTagCompound) {
        storedStack = ItemStackSerializer.deserializeFromNbt(dto)
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        ItemStackSerializer.serializeToNetwork(buf, storedStack)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        storedStack = ItemStackSerializer.deserializeFromNetwork(buf)
        syncObservers.onObservableUpdate()
    }
}
