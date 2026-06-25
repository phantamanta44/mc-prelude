package st.evening.mc.prelude.api.component.fluid

import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties
import st.evening.mc.prelude.api.util.game.forceCopyWithAmount

class RatedFluidHandler(
    private val delegate: IFluidHandler,
    private val inputRate: Int = -1,
    private val outputRate: Int = -1
) : IFluidHandler {
    private val tankProps: Array<IFluidTankProperties> =
        Array(delegate.tankProperties.size) { PropsWrapper(delegate.tankProperties[it]) }

    override fun getTankProperties(): Array<out IFluidTankProperties> = tankProps

    override fun fill(resource: FluidStack?, doFill: Boolean): Int = when {
        inputRate == 0 -> 0
        resource == null || inputRate < 0 || resource.amount <= inputRate -> delegate.fill(resource, doFill)
        else -> delegate.fill(resource.forceCopyWithAmount(inputRate), doFill)
    }

    override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? = when {
        outputRate == 0 -> null
        resource == null || outputRate < 0 || resource.amount <= outputRate -> delegate.drain(resource, doDrain)
        else -> delegate.drain(resource.forceCopyWithAmount(outputRate), doDrain)
    }

    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? =
        delegate.drain(if (outputRate < 0) maxDrain else maxDrain.coerceAtMost(outputRate), doDrain)

    private inner class PropsWrapper(private val delegateProps: IFluidTankProperties) :
        IFluidTankProperties by delegateProps {

        override fun canFill(): Boolean = inputRate != 0 && delegateProps.canFill()

        override fun canDrain(): Boolean = outputRate != 0 && delegateProps.canDrain()

        override fun canFillFluidType(fluidStack: FluidStack?): Boolean =
            inputRate != 0 && delegateProps.canFillFluidType(fluidStack)

        override fun canDrainFluidType(fluidStack: FluidStack?): Boolean =
            outputRate != 0 && delegateProps.canDrainFluidType(fluidStack)
    }
}
