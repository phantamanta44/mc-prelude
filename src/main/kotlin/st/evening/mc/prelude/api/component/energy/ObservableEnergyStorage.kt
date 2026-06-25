package st.evening.mc.prelude.api.component.energy

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraftforge.energy.IEnergyStorage
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.data.NbtDsl
import kotlin.math.min

open class ObservableEnergyStorage(protected val capacity: Int) :
    IEnergyStorage, Piecewise.Atom, GameSerializable<NBTTagCompound> {
    companion object {
        private const val SER_ENERGY: String = "energy"
    }

    protected var energy: Int = 0

    protected val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
    protected val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    override fun getEnergyStored(): Int = energy
    override fun getMaxEnergyStored(): Int = capacity

    override fun canExtract(): Boolean = true
    override fun canReceive(): Boolean = true

    fun setEnergyStored(energy: Int): Boolean {
        require(energy >= 0) { "Invalid negative amount of energy: $energy" }
        if (this.energy == energy) return false
        this.energy = energy
        stateObservers.onObservableUpdate()
        return true
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        if (maxReceive <= 0) return 0
        val toTransfer = min(maxReceive, capacity - energy)
        if (toTransfer <= 0) return 0
        if (!simulate) {
            energy += toTransfer
            stateObservers.onObservableUpdate()
        }
        return toTransfer
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        if (maxExtract <= 0) return 0
        val toTransfer = min(maxExtract, energy)
        if (toTransfer <= 0) return 0
        if (!simulate) {
            energy -= toTransfer
            stateObservers.onObservableUpdate()
        }
        return toTransfer
    }

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    override fun writeToNbt(): NBTTagCompound = NbtDsl.compound {
        SER_ENERGY int energy
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        energy = dto.getInteger(SER_ENERGY)
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        buf.writeInt(energy)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        energy = buf.readInt()
        syncObservers.onObservableUpdate()
    }
}
