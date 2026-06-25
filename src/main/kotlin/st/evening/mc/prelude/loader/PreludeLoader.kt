package st.evening.mc.prelude.loader

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.util.data.LazyProperty
import st.evening.mc.prelude.api.util.data.UnboundGenericProperty
import st.evening.mc.prelude.api.util.game.Sides
import kotlin.reflect.KProperty

interface ModDefinitionsDelegateProvider<T : Any> {
    operator fun <R : PreludeMod<T>> provideDelegate(thisRef: R, property: KProperty<*>): UnboundGenericProperty<T>
}

object PreludeLoader {
    internal val logger: Logger = LogManager.getLogger("prelude-loader")
    private val modsByClass: MutableMap<Class<out PreludeMod<*>>, PreludeModContainer<*>> = mutableMapOf()

    internal fun <T : Any> registerMod(modClass: Class<out PreludeMod<T>>, mod: PreludeModContainer<T>) {
        modsByClass[modClass] = mod
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getModDefinitions(modClass: Class<out PreludeMod<T>>): T? =
        modsByClass[modClass]?.getModDefinitions() as? T

    fun <T : Any> lazyModDefinitions(modClass: Class<out PreludeMod<T>>): UnboundGenericProperty<T> = LazyProperty {
        @Suppress("UNCHECKED_CAST")
        getModDefinitions(modClass)
            ?: throw NoSuchElementException("No Prelude mod registered for mod class: $modClass")
    }

    @Suppress("UNCHECKED_CAST") // technically not sound, but should work under erasure
    fun <T : Any> inject(): ModDefinitionsDelegateProvider<T> =
        ModDefinitionsDelegateProviderImpl as ModDefinitionsDelegateProvider<T>

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class Load(val value: String, val sideOnly: Sides = Sides.BOTH)

    private object ModDefinitionsDelegateProviderImpl : ModDefinitionsDelegateProvider<Any> {
        override fun <R : PreludeMod<Any>> provideDelegate(
            thisRef: R,
            property: KProperty<*>
        ): UnboundGenericProperty<Any> = lazyModDefinitions(thisRef.javaClass)
    }
}
