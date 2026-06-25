package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.copyWithSize
import kotlin.math.min

open class ObservableItemStore(override val capacity: Int) :
    ItemStore, Piecewise.Atom, GameSerializable<NBTTagCompound> {
    companion object {
        private const val SER_ITEM: String = "item"
        private const val SER_COUNT: String = "count"
    }

    override var storedItem: ItemKey? = null
        protected set
    override var count: Int = 0
        protected set

    private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
    private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) return stack

        storedItem?.let { stored ->
            if (!stored.matches(stack)) return stack
            val toTransfer = min(stack.count, capacity - count)
            if (toTransfer <= 0) return stack
            if (!simulate) {
                count += toTransfer
                stateObservers.onObservableUpdate()
            }
            return stack.copyWithSize(stack.count - toTransfer)
        }

        val toTransfer = min(stack.count, capacity)
        if (toTransfer <= 0) return stack // better safe than sorry
        if (!simulate) {
            storedItem = ItemKey.fromStack(stack)!!
            count = stack.count
            stateObservers.onObservableUpdate()
        }
        return stack.copyWithSize(stack.count - toTransfer)
    }

    override fun extractItem(amount: Int, simulate: Boolean): ItemStack {
        val currentCount = count
        if (amount <= 0 || currentCount <= 0) return ItemStack.EMPTY

        if (amount < currentCount) {
            if (!simulate) {
                count -= amount
                stateObservers.onObservableUpdate()
            }
            return storedItem!!.newStack(amount)
        }

        val stack = storedItem!!.newStack(currentCount)
        if (!simulate) {
            clearItem()
        }
        return stack
    }

    fun clearItem() {
        storedItem = null
        count = 0
        stateObservers.onObservableUpdate()
    }

    fun setItem(key: ItemKey, amount: Int) {
        require(amount >= 0) { "Invalid negative item count: $amount" }
        storedItem = key
        count = amount
        stateObservers.onObservableUpdate()
    }

    fun setItem(stack: ItemStack) {
        if (stack.isEmpty) {
            clearItem()
        } else {
            storedItem = ItemKey.fromStack(stack)
            count = stack.count
            stateObservers.onObservableUpdate()
        }
    }

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    override fun writeToNbt(): NBTTagCompound = NbtDsl.compound {
        storedItem?.let {
            SER_ITEM tag ItemKey.Serializer.serializeToNbt(it)
            SER_COUNT int count
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        val itemDto = dto.getCompoundOrNull(SER_ITEM)
        if (itemDto != null) {
            storedItem = ItemKey.Serializer.deserializeFromNbt(itemDto)
            count = dto.getInteger(SER_COUNT)
        } else {
            storedItem = null
            count = 0
        }
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        storedItem?.let {
            ItemKey.Serializer.serializeToNetwork(buf, it)
            buf.writeInt(count)
        } ?: run {
            buf.writeInt(0)
        }
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        if (buf.getInt(buf.readerIndex()) == 0) {
            buf.skipBytes(Int.SIZE_BYTES)
            storedItem = null
            count = 0
        } else {
            storedItem = ItemKey.Serializer.deserializeFromNetwork(buf)
            count = buf.readInt()
        }
        syncObservers.onObservableUpdate()
    }
}
