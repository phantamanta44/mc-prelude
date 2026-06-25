package st.evening.mc.prelude.api.sound

import net.minecraft.client.audio.ISound
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class LerpingPersistentSound(
    host: WeaklyValid,
    soundEvent: SoundEvent,
    category: SoundCategory,
    volume: Float = 0F,
    var volumeChangeRate: Float = 0.12F,
    pitch: Float = 1F,
    var pitchChangeRate: Float = 0.12F,
    attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
) : PersistentSound(host, soundEvent, category, pitch, attenuationType) {
    var targetVolume: Float = volume
    var targetPitch: Float = pitch

    constructor(
        host: WeaklyValid,
        posX: Float,
        posY: Float,
        posZ: Float,
        soundEvent: SoundEvent,
        category: SoundCategory,
        volume: Float = 0F,
        volumeChangeRate: Float = 0.12F,
        pitch: Float = 1F,
        pitchChangeRate: Float = 0.12F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(host, soundEvent, category, volume, volumeChangeRate, pitch, pitchChangeRate, attenuationType) {
        setPosition(posX, posY, posZ)
    }

    constructor(
        host: WeaklyValid,
        pos: Vec3d,
        soundEvent: SoundEvent,
        category: SoundCategory,
        volume: Float = 0F,
        volumeChangeRate: Float = 0.12F,
        pitch: Float = 1F,
        pitchChangeRate: Float = 0.12F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(
        host, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), soundEvent, category,
        volume, volumeChangeRate, pitch, pitchChangeRate, attenuationType
    )

    constructor(
        host: WeaklyValid,
        pos: BlockPos,
        soundEvent: SoundEvent,
        category: SoundCategory,
        volume: Float = 0F,
        volumeChangeRate: Float = 0.12F,
        pitch: Float = 1F,
        pitchChangeRate: Float = 0.12F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(
        host, pos.x + 0.5F, pos.y + 0.5F, pos.z + 0.5F, soundEvent, category,
        volume, volumeChangeRate, pitch, pitchChangeRate, attenuationType
    )

    init {
        this.volume = volume.coerceAtLeast(0.01F)
    }

    override fun setVolume(volume: Float) {
        super.setVolume(volume)
        targetVolume = volume
    }

    override fun setPitch(pitch: Float) {
        super.setPitch(pitch)
        targetPitch = pitch
    }

    override fun tick() {
        step(volume, targetVolume, volumeChangeRate) { volume = it }
        step(pitch, targetPitch, pitchChangeRate) { pitch = it }
    }

    private inline fun step(currentValue: Float, targetValue: Float, changeRate: Float, update: (Float) -> Unit) {
        if (currentValue < targetValue) {
            update((currentValue + changeRate).coerceAtMost(targetValue))
        } else if (currentValue > targetValue) {
            update((currentValue - changeRate).coerceAtLeast(targetValue))
        }
    }
}
