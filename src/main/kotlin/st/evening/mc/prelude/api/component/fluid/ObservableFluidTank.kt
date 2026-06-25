package st.evening.mc.prelude.api.component.fluid

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTankInfo
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties
import st.evening.mc.prelude.api.data.ser.FluidStackSerializer
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.game.forceCopyWithAmount
import kotlin.math.min

open class ObservableFluidTank(private val _capacity: Int) :
    IFluidTank, IFluidTankProperties, IFluidHandler, Piecewise.Atom, GameSerializable<NBTTagCompound> {
    protected var storedFluid: FluidStack? = null
    private val tankProperties: Array<IFluidTankProperties> = arrayOf(this)

    protected val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
    protected val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    override fun getFluid(): FluidStack? = storedFluid
    override fun getContents(): FluidStack? = storedFluid
    override fun getFluidAmount(): Int = storedFluid?.amount ?: 0
    override fun getCapacity(): Int = _capacity
    override fun getInfo(): FluidTankInfo = FluidTankInfo(this)

    override fun canFill(): Boolean = true
    override fun canDrain(): Boolean = true
    override fun canFillFluidType(fluidStack: FluidStack?): Boolean = canFill()
    override fun canDrainFluidType(fluidStack: FluidStack?): Boolean = canDrain()

    fun setFluid(fluidStack: FluidStack?) {
        storedFluid = if (fluidStack == null || fluidStack.amount <= 0) null else fluidStack
        stateObservers.onObservableUpdate()
    }

    override fun fill(resource: FluidStack?, doFill: Boolean): Int {
        if (resource == null) return 0

        storedFluid?.let { stored ->
            if (!stored.isFluidEqual(resource)) return 0
            val toTransfer = min(resource.amount, _capacity - stored.amount)
            if (toTransfer <= 0) return 0
            if (doFill) {
                stored.amount += toTransfer
                stateObservers.onObservableUpdate()
            }
            return toTransfer
        }

        val toTransfer = min(resource.amount, _capacity)
        if (toTransfer <= 0) return 0
        if (doFill) {
            storedFluid = resource.forceCopyWithAmount(toTransfer)
            stateObservers.onObservableUpdate()
        }
        return toTransfer
    }

    private fun doDrain(stored: FluidStack, maxDrain: Int, doDrain: Boolean): FluidStack {
        if (maxDrain < stored.amount) {
            if (doDrain) {
                stored.amount -= maxDrain
                stateObservers.onObservableUpdate()
            }
            return stored.forceCopyWithAmount(maxDrain)
        } else if (doDrain) {
            storedFluid = null
            stateObservers.onObservableUpdate()
            return stored
        } else {
            return stored.copy()
        }
    }

    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? {
        if (maxDrain <= 0) return null
        storedFluid?.let { return doDrain(it, maxDrain, doDrain) }
        return null
    }

    override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? {
        if (resource == null || resource.amount <= 0) return null
        storedFluid?.let { stored ->
            if (stored.isFluidEqual(resource)) {
                return doDrain(stored, resource.amount, doDrain)
            }
        }
        return null
    }

    override fun getTankProperties(): Array<out IFluidTankProperties> = tankProperties

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    override fun writeToNbt(): NBTTagCompound = FluidStackSerializer.serializeToNbt(storedFluid)

    override fun readFromNbt(dto: NBTTagCompound) {
        storedFluid = FluidStackSerializer.deserializeFromNbt(dto)
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        FluidStackSerializer.serializeToNetwork(buf, storedFluid)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        storedFluid = FluidStackSerializer.deserializeFromNetwork(buf)
        syncObservers.onObservableUpdate()
    }
}
