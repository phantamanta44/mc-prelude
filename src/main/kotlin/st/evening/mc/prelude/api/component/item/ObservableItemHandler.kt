package st.evening.mc.prelude.api.component.item

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.PacketBuffer
import net.minecraftforge.items.IItemHandlerModifiable
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.capability.ItemStoreHandler
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.observeFromComposite
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.data.BitVector
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.forEachCompound
import st.evening.mc.prelude.api.util.data.plusAssign
import st.evening.mc.prelude.api.util.data.readAndIterateBitVector
import st.evening.mc.prelude.api.util.math.ceilDivPos

open class ObservableItemHandler(protected val slots: List<ObservableItemSlot>) :
    ItemStoreHandler, IItemHandlerModifiable, Piecewise.Composite, GameSerializable<NBTTagList> {
    companion object {
        private const val SER_INDEX: String = "index"
        private const val SER_STACK: String = "stack"

        inline fun nSlots(
            slotCount: Int,
            slotFactory: (Int) -> ObservableItemSlot = { ObservableItemSlot() }
        ): ObservableItemHandler = ObservableItemHandler(List(slotCount) { slotFactory(it) })
    }

    override val stateSize: Int
        get() = slots.size
    protected val stateObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()
    protected val syncObservers: MutableSet<Observer.Indexed> = WeakValidityMap.newSet()

    init {
        slots.forEachIndexed { index, slot ->
            slot.observeFromComposite(index, stateObservers, syncObservers)
        }
    }

    constructor(vararg slots: ObservableItemSlot) : this(slots.toList())

    protected open fun initSlot(index: Int): ObservableItemSlot = ObservableItemSlot()

    override fun getSlots(): Int = slots.size

    override fun getSlotHandler(index: Int): ItemStore = slots[index]

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        slots[slot].setItem(stack)
    }

    override fun getStateAtom(index: Int): Piecewise.Atom = slots[index]

    override fun observeState(observer: Observer.Indexed) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Indexed) {
        syncObservers += observer
    }

    override fun writeToNbt(): NBTTagList = NBTTagList().also { tag ->
        slots.forEachIndexed { index, slot ->
            if (!slot.isEmpty) {
                tag += NbtDsl.compound {
                    SER_INDEX int index
                    SER_STACK tag slot.writeToNbt()
                }
            }
        }
    }

    @OptIn(PreludeInternal::class)
    private fun readEmptySlots(seen: BooleanArray) {
        slots.forEachIndexed { index, slot ->
            if (!seen[index]) {
                slot.readEmpty()
            }
        }
    }

    override fun readFromNbt(dto: NBTTagList) {
        val seen = BooleanArray(slots.size)
        dto.forEachCompound {
            val slotIndex = it.getInteger(SER_INDEX)
            slots[slotIndex].readFromNbt(it.getCompoundTag(SER_STACK))
            seen[slotIndex] = true
        }
        readEmptySlots(seen)
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        val neSlotCount = slots.count { !it.isEmpty }
        if (neSlotCount == 0) {
            buf.writeByte(0)
        } else if (neSlotCount + 1 <= slots.size ceilDivPos 8) {
            buf.writeByte(1).writeByte(neSlotCount)
            slots.forEachIndexed { slotIndex, slot ->
                if (slot.isEmpty) return@forEachIndexed
                buf.writeByte(slotIndex)
                slot.writeToNetwork(buf)
            }
        } else {
            buf.writeByte(2)
            val mask = BitVector(slots.size)
            slots.forEachIndexed { slotIndex, stack ->
                if (!stack.isEmpty) {
                    mask.set(slotIndex)
                }
            }
            mask.writeToNetwork(buf)
            slots.forEach {
                if (!it.isEmpty) {
                    it.writeToNetwork(buf)
                }
            }
        }
    }

    @OptIn(PreludeInternal::class)
    override fun readFromNetwork(buf: PacketBuffer) {
        when (val ord = buf.readByte().toInt()) {
            0 -> slots.forEach { it.readEmpty() }

            1 -> {
                val seen = BooleanArray(slots.size)
                val count = buf.readByte().toUByte().toInt()
                repeat(count) {
                    val slotIndex = buf.readByte().toInt()
                    slots[slotIndex].readFromNetwork(buf)
                    seen[slotIndex] = true
                }
                readEmptySlots(seen)
            }

            2 -> {
                val seen = BooleanArray(slots.size)
                buf.readAndIterateBitVector(slots.size) { slotIndex ->
                    slots[slotIndex].readFromNetwork(buf)
                    seen[slotIndex] = true
                }
                readEmptySlots(seen)
            }

            else -> throw SerializationException(ord.toString())
        }
    }
}
