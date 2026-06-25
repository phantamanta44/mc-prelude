package st.evening.mc.prelude.api.component.energy

import net.minecraftforge.energy.IEnergyStorage

class RatedEnergyStorage(
    private val delegate: IEnergyStorage,
    private val inputRate: Int = -1,
    private val outputRate: Int = -1
) : IEnergyStorage by delegate {
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int =
        delegate.receiveEnergy(if (inputRate < 0) maxReceive else maxReceive.coerceAtMost(inputRate), simulate)

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int =
        delegate.extractEnergy(if (outputRate < 0) maxExtract else maxExtract.coerceAtMost(outputRate), simulate)

    override fun canExtract(): Boolean = outputRate != 0
    override fun canReceive(): Boolean = inputRate != 0
}
