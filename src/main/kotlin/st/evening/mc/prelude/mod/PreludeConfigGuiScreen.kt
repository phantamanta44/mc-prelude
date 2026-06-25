package st.evening.mc.prelude.mod

import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.GuiScreen
import net.minecraft.init.SoundEvents
import net.minecraft.util.SoundEvent
import net.minecraftforge.fml.client.IModGuiFactory
import org.lwjgl.input.Keyboard
import st.evening.mc.prelude.PreludeConsts
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.JsonConfigGuiScreen
import st.evening.mc.prelude.api.util.collection.RingBuffer
import st.evening.mc.prelude.api.util.collection.containsIndex
import st.evening.mc.prelude.api.util.game.ClientSide
import kotlin.math.pow

@ClientSide.Physical
class PreludeConfigGuiScreen(parentScreen: GuiScreen?, config: JsonConfig.ConfigObject) :
    JsonConfigGuiScreen(parentScreen, PreludeConsts.MOD_ID, null, "", config) {
    companion object {
        private val NOTES: IntArray = intArrayOf(
            Keyboard.KEY_UP, Keyboard.KEY_RIGHT, Keyboard.KEY_UP, Keyboard.KEY_RIGHT, Keyboard.KEY_LEFT, Keyboard.KEY_UP
        )
        private val PRELUDE: List<B?> = listOf(
            B(24, 7, 4, 0), B(19), null, null, B(24), B(19), B(21), B(24),
            B(-1, 9, 5, 0), null, null, null, null, null, null, null,
            B(24, 7, 4, 0), B(19), null, null, B(24), B(19), B(21), B(24),
            B(-1, 9, 5, 0), null, null, null, null, null, null, null,
            B(-1, 12, 5, 2), null, null, null, null, null, null, null,
            B(-1, 12, 5, 1), null, null, null, null, null, null, null,
            B(-1, 11, 4, 0), null, B(-1, -1, 0, -1), B(-1, -1, 4, -1), B(-1, -1, 7, -1), B(0), B(4), B(7),
            B(12), B(16), B(19), B(23), null, B(24)
        )
    }

    private val played: RingBuffer<Int> = RingBuffer(6)
    private var seq: Int = -1
    private var lastSeq: Long = 0L
    private var offBeat: Boolean = false

    private fun match() {
        if (!played.isFull()) return
        played.forEachIndexed { index, note ->
            if (note != NOTES[index]) return
        }
        seq = 0
    }

    override fun keyTyped(eventChar: Char, eventKey: Int) {
        super.keyTyped(eventChar, eventKey)
        if (seq == -1) {
            when (eventKey) {
                Keyboard.KEY_UP, Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT, Keyboard.KEY_DOWN -> {
                    played.offer(eventKey)
                    match()
                }
            }
        }
    }

    override fun updateScreen() {
        super.updateScreen()
        if (PRELUDE.containsIndex(seq)) {
            val now = Minecraft.getSystemTime()
            if (now - lastSeq >= 90L) {
                lastSeq = now
                if (offBeat) {
                    play(SoundEvents.BLOCK_NOTE_BELL, 21)
                    offBeat = false
                } else {
                    PRELUDE[seq]?.play()
                    play(SoundEvents.BLOCK_NOTE_BELL, 19)
                    seq++
                    offBeat = true
                }
            }
        }
    }

    fun B.play() {
        if (mel >= 0) {
            play(SoundEvents.BLOCK_NOTE_HARP, mel)
        }
        if (hi >= 0) {
            play(SoundEvents.BLOCK_NOTE_FLUTE, hi)
        }
        if (mid >= 0) {
            play(SoundEvents.BLOCK_NOTE_BASS, mid)
        }
        if (lo >= 0) {
            play(SoundEvents.BLOCK_NOTE_FLUTE, lo)
        }
    }

    fun play(instrument: SoundEvent, pitch: Int) {
        mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(instrument, 2F.pow((pitch - 12F) / 12F)))
    }

    class B(val mel: Int, val hi: Int, val mid: Int, val lo: Int) {
        constructor(mel: Int) : this(mel, -1, -1, -1)
    }

    class Factory(private val config: JsonConfig.ConfigObject) : IModGuiFactory {
        override fun initialize(minecraftInstance: Minecraft) {}

        override fun hasConfigGui(): Boolean = true

        override fun createConfigGui(parentScreen: GuiScreen?): GuiScreen =
            PreludeConfigGuiScreen(parentScreen, config)

        override fun runtimeGuiCategories(): Set<IModGuiFactory.RuntimeOptionCategoryElement>? = null
    }
}
