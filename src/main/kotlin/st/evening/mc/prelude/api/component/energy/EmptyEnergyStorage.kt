package st.evening.mc.prelude.api.component.energy

import net.minecraftforge.energy.IEnergyStorage

object EmptyEnergyStorage : IEnergyStorage {
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int = 0
    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0
    override fun getEnergyStored(): Int = 0
    override fun getMaxEnergyStored(): Int = 0
    override fun canExtract(): Boolean = false
    override fun canReceive(): Boolean = false
}
