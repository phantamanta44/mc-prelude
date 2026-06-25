package st.evening.mc.prelude.api.component.fluid

import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties

class FluidTankHandler(
    private val fluidTank: IFluidTank,
    private val canFill: Boolean,
    private val canDrain: Boolean
) : IFluidHandler {
    private val tankProps: Array<IFluidTankProperties> = arrayOf(TankProperties())

    override fun getTankProperties(): Array<out IFluidTankProperties> = tankProps

    override fun fill(resource: FluidStack?, doFill: Boolean): Int = fluidTank.fill(resource, doFill)

    override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? {
        if (resource == null) return null
        val stored = fluidTank.fluid
        if (!resource.isFluidEqual(stored)) return null
        return fluidTank.drain(resource.amount, doDrain)
    }

    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? = fluidTank.drain(maxDrain, doDrain)

    private inner class TankProperties : IFluidTankProperties {
        override fun getContents(): FluidStack? = fluidTank.fluid
        override fun getCapacity(): Int = fluidTank.capacity
        override fun canFill(): Boolean = canFill
        override fun canDrain(): Boolean = canDrain
        override fun canFillFluidType(fluidStack: FluidStack?): Boolean = canFill
        override fun canDrainFluidType(fluidStack: FluidStack?): Boolean = canDrain
    }
}
