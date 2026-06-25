package st.evening.mc.prelude.api.util.game

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.audio.SoundHandler
import net.minecraft.init.SoundEvents

@ClientSide.Physical
fun SoundHandler.playUiClick(pitch: Float = 1F) {
    playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, pitch))
}
