package st.evening.mc.prelude.loader

import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.event.FMLEvent
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.RegistrationTarget
import st.evening.mc.prelude.api.util.game.ClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide

internal class ModRegistrarImpl(private val mod: ModContainer) : ModRegistrar {
    override val modId: String
        get() = mod.modId
    private val regStates: MutableMap<RegistrationTarget<*, *, *>, Any?> = mutableMapOf()
    private val fmlListeners: MutableMap<Class<out FMLEvent>, FmlListenerList<*>> = mutableMapOf()

    @PreludeInternal
    @Suppress("UNCHECKED_CAST")
    override fun <S> getState(target: RegistrationTarget<S, *, *>): S =
        regStates.getOrPut(target) { target.init(mod, this) } as S

    @Suppress("UNCHECKED_CAST")
    private fun <E : FMLEvent> getListeners(eventType: Class<E>): FmlListenerList<E> =
        fmlListeners.getOrPut(eventType) { FmlListenerList<E>() } as FmlListenerList<E>

    override fun <E : FMLEvent> registerFmlListener(eventType: Class<E>, listener: (E) -> Unit) {
        getListeners(eventType).listeners += listener
    }

    @ClientSide.Physical
    override fun setConfigGuiFactory(factory: IModGuiFactory) {
        ClientHelper.setConfigGuiFactory(mod, factory)
    }

    internal fun handleModStateEvent(event: FMLEvent) {
        postFmlEvent(event)
    }

    private fun <E : FMLEvent> postFmlEvent(event: E) {
        getListeners(event.javaClass).listeners.forEach { it(event) }
    }

    private class FmlListenerList<E : FMLEvent> {
        val listeners: MutableList<(E) -> Unit> = mutableListOf()
    }
}
