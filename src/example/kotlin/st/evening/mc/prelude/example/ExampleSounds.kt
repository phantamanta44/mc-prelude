package st.evening.mc.prelude.example

import net.minecraft.util.SoundEvent
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.soundEvent

class ExampleSounds(reg: ModRegistrar) {
    val alloyFurnaceWorking: SoundEvent by reg.soundEvent("block.alloy_furnace.working")
}
