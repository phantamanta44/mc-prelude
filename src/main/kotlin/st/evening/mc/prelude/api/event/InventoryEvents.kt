package st.evening.mc.prelude.api.event

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraftforge.event.entity.player.PlayerContainerEvent
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class SlotClickEvent(
    player: EntityPlayer,
    container: Container,
    val slotIndex: Int,
    val clickType: ClickType,
    val mouseButton: Int
) : PlayerContainerEvent(player, container)
