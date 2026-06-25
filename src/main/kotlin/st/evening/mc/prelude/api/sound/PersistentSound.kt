package st.evening.mc.prelude.api.sound

import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.ITickableSound
import net.minecraft.client.audio.MovingSound
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import st.evening.mc.prelude.api.util.collection.WeakValidityReference
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
open class PersistentSound(
    host: WeaklyValid,
    soundEvent: SoundEvent,
    category: SoundCategory,
    pitch: Float = 1F,
    attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
) : MovingSound(soundEvent, category), ITickableSound {
    private val hostRef: WeakValidityReference<WeaklyValid> = WeakValidityReference(host)
    private var age: Int = 0

    init {
        // minecraft ignores the sound if it has zero initial volume, so we don't set it here
        this.pitch = pitch
        this.attenuationType = attenuationType
        this.repeat = true
        this.repeatDelay = 0
    }

    constructor(
        host: WeaklyValid,
        posX: Float,
        posY: Float,
        posZ: Float,
        soundEvent: SoundEvent,
        category: SoundCategory,
        pitch: Float = 1F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(host, soundEvent, category, pitch, attenuationType) {
        setPosition(posX, posY, posZ)
    }

    constructor(
        host: WeaklyValid,
        pos: Vec3d,
        soundEvent: SoundEvent,
        category: SoundCategory,
        pitch: Float = 1F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(
        host, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), soundEvent, category, pitch, attenuationType
    )

    constructor(
        host: WeaklyValid,
        pos: BlockPos,
        soundEvent: SoundEvent,
        category: SoundCategory,
        pitch: Float = 1F,
        attenuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR
    ) : this(host, pos.x + 0.5F, pos.y + 0.5F, pos.z + 0.5F, soundEvent, category, pitch, attenuationType)

    open fun setPosition(x: Float, y: Float, z: Float) {
        this.xPosF = x
        this.yPosF = y
        this.zPosF = z
    }

    fun setPosition(pos: Vec3d) {
        setPosition(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
    }

    fun setPosition(pos: BlockPos) {
        setPosition(pos.x + 0.5F, pos.y + 0.5F, pos.z + 0.5F)
    }

    open fun setVolume(volume: Float) {
        this.volume = volume
    }

    open fun setPitch(pitch: Float) {
        this.pitch = pitch
    }

    open fun kill() {
        donePlaying = true
    }

    override fun update() {
        if (donePlaying) return
        age++
        if (age % 20 == 0 && !hostRef.isPresent()) {
            kill()
        } else {
            tick()
        }
    }

    open fun tick() {}
}
