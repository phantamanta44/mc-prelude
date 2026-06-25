package st.evening.mc.prelude.api.registration

import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.coremod.mixin.SoundEventAccessor

class SoundEventRegistrar(private val modId: String) {
    companion object {
        val TARGET: RegistrationTarget<SoundEventRegistrar, Entry.T, SoundEvent> =
            object : RegistrationTarget<SoundEventRegistrar, Entry.T, SoundEvent> {
                override fun init(mod: ModContainer, reg: ModRegistrar): SoundEventRegistrar =
                    SoundEventRegistrar(mod.modId).also { soundEventReg ->
                        reg.on<RegistryEvent.Register<SoundEvent>>(listener = soundEventReg::dispatchSoundEvents)
                    }

                override fun <U : SoundEvent> register(
                    state: SoundEventRegistrar,
                    entry: Ap<Entry.T, U>
                ): RegistrationHolder<U> = state.register(entry.unap())
            }
    }

    private val soundEventRegQueue: MutableList<SoundEvent> = mutableListOf()

    private fun <T : SoundEvent> register(entry: Entry<T>): RegistrationHolder<T> {
        val soundName = entry.soundName
        val key = ResourceLocation(modId, soundName)
        Prelude.logger.trace("Registering sound event: {}", key)
        soundEventRegQueue += SoundEvent(key) // this will cause problems if T != SoundEvent, but oh well
        return RegistrationHolder.FromForgeRegistry(ForgeRegistries.SOUND_EVENTS, key)
    }

    private fun dispatchSoundEvents(event: RegistryEvent.Register<SoundEvent>) {
        Prelude.logger.debug("Dispatching {} {} sound event registrations", soundEventRegQueue.size, modId)
        soundEventRegQueue.forEach { soundEvent ->
            val soundName = (soundEvent as SoundEventAccessor).`prelude$soundName`()
            Prelude.logger.trace("Dispatching sound event registration: {}", soundName)
            soundEvent.registryName = soundName
            event.registry.register(soundEvent)
        }
        soundEventRegQueue.clear()
    }

    class Entry<T : SoundEvent>(val soundName: String) : Ap<Entry.T, T> {
        object T : TT
    }
}

fun <T : SoundEvent> Ap<SoundEventRegistrar.Entry.T, T>.unap(): SoundEventRegistrar.Entry<T> =
    this as SoundEventRegistrar.Entry<T>

fun ModRegistrar.soundEvent(soundName: String): RegistrationHolder<SoundEvent> =
    register(SoundEventRegistrar.TARGET, SoundEventRegistrar.Entry(soundName))
