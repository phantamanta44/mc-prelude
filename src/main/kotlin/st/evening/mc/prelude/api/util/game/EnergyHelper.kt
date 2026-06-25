package st.evening.mc.prelude.api.util.game

import net.minecraftforge.energy.IEnergyStorage
import st.evening.mc.prelude.api.util.collection.HoleBuffer
import st.evening.mc.prelude.api.util.math.floorDiv

object EnergyHelper {
    fun distribute(amount: Int, receivers: Collection<IEnergyStorage>): Int {
        if (amount <= 0 || receivers.isEmpty()) return 0
        val remReceivers = HoleBuffer(receivers)
        var remEnergy = amount
        remReceivers.removeIf { !it.canReceive() }

        // run rounds of even distribution until none of the receivers accept any more energy
        while (remEnergy > 0 && remReceivers.isNotEmpty()) {
            var stalled = true
            val portion = remEnergy floorDiv remReceivers.size
            if (portion <= 0) { // not enough left to distribute evenly; distribute the rest first-come-first-serve
                remReceivers.forEach {
                    remEnergy -= it.receiveEnergy(remEnergy, false)
                    if (remEnergy <= 0) return amount
                }
                break
            }
            val iter = remReceivers.iterator()
            while (iter.hasNext()) {
                val transferred = iter.next().receiveEnergy(portion, false)
                if (transferred < 0) { // it didn't accept any energy, so it's probably full; skip it in future rounds
                    iter.remove()
                    continue
                }
                stalled = false
                remEnergy -= transferred
            }
            if (stalled) break
        }
        return amount - remEnergy
    }
}

fun IEnergyStorage.isEmpty(): Boolean = energyStored <= 0

fun IEnergyStorage.computeComparatorValue(): Int {
    val n = energyStored
    return if (n > 0) 1 + 14 * n / maxEnergyStored else 0
}
