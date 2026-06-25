package st.evening.mc.prelude.api.component.energy

import net.minecraftforge.energy.IEnergyStorage

class ConcatEnergyStorage(private val stores: List<IEnergyStorage>) : IEnergyStorage {
    companion object {
        fun of(vararg stores: IEnergyStorage): ConcatEnergyStorage = ConcatEnergyStorage(stores.asList())
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        var rem = maxReceive
        stores.forEach { store ->
            rem -= store.receiveEnergy(rem, simulate)
            if (rem <= 0) return maxReceive
        }
        return maxReceive - rem
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        var rem = maxExtract
        stores.forEach { store ->
            rem -= store.extractEnergy(rem, simulate)
            if (rem <= 0) return maxExtract
        }
        return maxExtract - rem
    }

    override fun getEnergyStored(): Int = stores.sumOf { it.energyStored }

    override fun getMaxEnergyStored(): Int = stores.sumOf { it.maxEnergyStored }

    override fun canExtract(): Boolean = stores.any { it.canExtract() }

    override fun canReceive(): Boolean = stores.any { it.canReceive() }
}
