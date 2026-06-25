package st.evening.mc.prelude.mod.feature

import net.minecraft.util.EnumHand
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on

internal object Unclunkify {
    fun init(reg: ModRegistrar) {
        reg.on<PlayerEvent.BreakSpeed>(priority = EventPriority.LOW) { event ->
            val config = Prelude.config.unclunkify
            if (!config.enabled) return@on
            val player = event.entityPlayer
            val held = player.getHeldItem(EnumHand.MAIN_HAND)
            if (!held.isEmpty) {
                val toolSpeed = held.getDestroySpeed(event.state)
                if (toolSpeed > 1F) {
                    val diff = config.toolSpeedThreshold - toolSpeed
                    if (diff > 0F) {
                        event.newSpeed += event.originalSpeed * (config.globalMiningSpeedMultiplier - 1F) +
                            diff * config.toolSpeedModifier
                        return@on
                    }
                }
            }
            event.newSpeed += event.originalSpeed * (config.globalMiningSpeedMultiplier - 1F)
        }
    }
}
