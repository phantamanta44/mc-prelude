package st.evening.mc.prelude.api

import net.minecraftforge.fml.common.gameevent.TickEvent
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class ClientApi internal constructor(reg: ModRegistrar) {
    var clientTick: Long = 0
        private set

    init {
        reg.on<TickEvent.ClientTickEvent> { event ->
            if (event.phase == TickEvent.Phase.END) {
                clientTick++
            }
        }
    }
}
