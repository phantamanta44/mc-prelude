package st.evening.mc.prelude.api.component.fluid

import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import st.evening.mc.prelude.api.util.game.forceCopyWithAmount

class RatedFluidTank(
    private val delegate: IFluidTank,
    private val inputRate: Int = -1,
    private val outputRate: Int = -1
) : IFluidTank by delegate {
    override fun fill(resource: FluidStack?, doFill: Boolean): Int = when {
        inputRate < 0 -> delegate.fill(resource, doFill)
        inputRate == 0 -> 0
        else -> delegate.fill(resource?.let { it.forceCopyWithAmount(it.amount.coerceAtMost(inputRate)) }, doFill)
    }

    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? =
        delegate.drain(if (outputRate < 0) maxDrain else maxDrain.coerceAtMost(outputRate), doDrain)
}
