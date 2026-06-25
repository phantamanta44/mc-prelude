package st.evening.mc.prelude.api.registration

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.IContextSetter
import net.minecraftforge.fml.common.eventhandler.IEventListener
import net.minecraftforge.fml.common.eventhandler.IGenericEvent
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.system.asAccessible
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class EventListenerRegistrar(private val mod: ModContainer, bus: EventBus) {
    private class RegistrationTargetImpl(private val bus: EventBus) :
        RegistrationTarget<EventListenerRegistrar, Entry.T, IEventListener> {
        override fun init(mod: ModContainer, reg: ModRegistrar): EventListenerRegistrar =
            EventListenerRegistrar(mod, bus)

        override fun <U : IEventListener> register(
            state: EventListenerRegistrar,
            entry: Ap<Entry.T, U>
        ): RegistrationHolder<U> = state.register(entry.unap())
    }

    companion object {
        val TARGET: RegistrationTarget<EventListenerRegistrar, Entry.T, IEventListener> =
            RegistrationTargetImpl(MinecraftForge.EVENT_BUS)
        val TARGET_ORE_GEN: RegistrationTarget<EventListenerRegistrar, Entry.T, IEventListener> =
            RegistrationTargetImpl(MinecraftForge.ORE_GEN_BUS)
        val TARGET_TERRAIN_GEN: RegistrationTarget<EventListenerRegistrar, Entry.T, IEventListener> =
            RegistrationTargetImpl(MinecraftForge.TERRAIN_GEN_BUS)

        private val fEventBus_busID: Field =
            EventBus::class.java.getDeclaredField("busID").asAccessible()
        private val fEventBus_listeners: Field =
            EventBus::class.java.getDeclaredField("listeners").asAccessible()
        private val fEventBus_listenerOwners: Field =
            EventBus::class.java.getDeclaredField("listenerOwners").asAccessible()

        private val KType.java: Class<*>
            get() = (classifier as KClass<*>).java

        @PreludeInternal
        fun <E : Event> wrapListener(
            eventType: KType, // this is essentially unchecked!
            receiveCancelled: Boolean,
            listener: (E) -> Unit
        ): IEventListener {
            if (IGenericEvent::class.java.isAssignableFrom(eventType.java)) {
                val typeParam = eventType.arguments.firstOrNull()?.type?.java
                    ?: throw IllegalArgumentException("Generic event type has no type parameter!")
                return object : IEventListener {
                    override fun invoke(event: Event) {
                        if (
                            (receiveCancelled || !event.isCancelable || !event.isCanceled) &&
                            (event as IGenericEvent<*>).genericType == typeParam
                        ) {
                            @Suppress("UNCHECKED_CAST")
                            listener(event as E)
                        }
                    }

                    override fun toString(): String = listener.toString()
                }
            } else {
                return object : IEventListener {
                    override fun invoke(event: Event) {
                        if (receiveCancelled || !event.isCancelable || !event.isCanceled) {
                            @Suppress("UNCHECKED_CAST")
                            listener(event as E)
                        }
                    }

                    override fun toString(): String = listener.toString()
                }
            }
        }
    }

    private val busId: Int = fEventBus_busID.getInt(bus)

    @Suppress("UNCHECKED_CAST")
    private val listeners: MutableMap<Any, ArrayList<IEventListener>> =
        fEventBus_listeners.get(bus) as MutableMap<Any, ArrayList<IEventListener>>

    @Suppress("UNCHECKED_CAST")
    private val listenerOwners: MutableMap<Any, ModContainer> =
        fEventBus_listenerOwners.get(bus) as MutableMap<Any, ModContainer>

    private fun <L : IEventListener> register(entry: Entry<L>): RegistrationHolder<L> {
        Prelude.logger.trace("Registering {} {} listener: {}", mod.modId, entry.eventType.name, entry.listener)
        val underlyingListener = if (IContextSetter::class.java.isAssignableFrom(entry.eventType)) (IEventListener {
            val loader = Loader.instance()
            val oldMod = loader.activeModContainer()
            loader.setActiveModContainer(mod)
            (it as IContextSetter).setModContainer(mod)
            entry.listener(it)
            loader.setActiveModContainer(oldMod)
        }) else entry.listener

        listenerOwners[entry.listener] = mod
        listeners.getOrPut(entry.listener) { ArrayList() } += underlyingListener
        entry.eventType.getConstructor().newInstance().listenerList.register(busId, entry.priority, underlyingListener)

        return RegistrationHolder.Immediate(entry.listener)
    }

    class Entry<L : IEventListener>(
        val eventType: Class<out Event>,
        val listener: L,
        val priority: EventPriority
    ) : Ap<Entry.T, L> {
        object T : TT
    }
}

fun <L : IEventListener> Ap<EventListenerRegistrar.Entry.T, L>.unap(): EventListenerRegistrar.Entry<L> =
    this as EventListenerRegistrar.Entry<L>

@OptIn(PreludeInternal::class)
inline fun <reified E : Event> ModRegistrar.listen(
    target: RegistrationTarget<EventListenerRegistrar, EventListenerRegistrar.Entry.T, IEventListener>,
    priority: EventPriority = EventPriority.NORMAL,
    receiveCancelled: Boolean = false,
    noinline listener: (E) -> Unit
): RegistrationHolder<IEventListener> = register(
    target,
    EventListenerRegistrar.Entry(
        E::class.java,
        EventListenerRegistrar.wrapListener(typeOf<E>(), receiveCancelled, listener),
        priority
    )
)

inline fun <reified E : Event> ModRegistrar.on(
    priority: EventPriority = EventPriority.NORMAL,
    receiveCancelled: Boolean = false,
    noinline listener: (E) -> Unit
): RegistrationHolder<IEventListener> = listen(EventListenerRegistrar.TARGET, priority, receiveCancelled, listener)

inline fun <reified E : Event> ModRegistrar.onOreGen(
    priority: EventPriority = EventPriority.NORMAL,
    receiveCancelled: Boolean = false,
    noinline listener: (E) -> Unit
): RegistrationHolder<IEventListener> =
    listen(EventListenerRegistrar.TARGET_ORE_GEN, priority, receiveCancelled, listener)

inline fun <reified E : Event> ModRegistrar.onTerrainGen(
    priority: EventPriority = EventPriority.NORMAL,
    receiveCancelled: Boolean = false,
    noinline listener: (E) -> Unit
): RegistrationHolder<IEventListener> =
    listen(EventListenerRegistrar.TARGET_TERRAIN_GEN, priority, receiveCancelled, listener)
