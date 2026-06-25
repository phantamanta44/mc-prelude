package st.evening.mc.prelude.api.util.data

import kotlin.concurrent.getOrSet
import kotlin.reflect.KProperty

interface GenericProperty<out T> {
    operator fun <R> getValue(thisRef: R, property: KProperty<*>): T
}

interface UnboundGenericProperty<out T> : GenericProperty<T> {
    fun getPropertyValue(): T

    override fun <R> getValue(thisRef: R, property: KProperty<*>): T = getPropertyValue()
}

interface GenericWritableProperty<T> : GenericProperty<T> {
    operator fun <R> setValue(thisRef: R, property: KProperty<*>, newValue: T)
}

interface UnboundGenericWritableProperty<T> : GenericWritableProperty<T>, UnboundGenericProperty<T> {
    fun setPropertyValue(newValue: T)

    override fun <R> setValue(thisRef: R, property: KProperty<*>, newValue: T) {
        setPropertyValue(newValue)
    }
}

interface GenericDelegateProvider<D> {
    operator fun <R> provideDelegate(thisRef: R, property: KProperty<*>): D
}

typealias GenericPropertyProvider<T> = GenericDelegateProvider<GenericProperty<T>>
typealias UnboundGenericPropertyProvider<T> = GenericDelegateProvider<UnboundGenericProperty<T>>

class ValueProperty<T>(private var value: T) : UnboundGenericWritableProperty<T> {
    override fun getPropertyValue(): T = value

    override fun setPropertyValue(newValue: T) {
        value = newValue
    }
}

class LazyProperty<T : Any>(private val factory: () -> T) : UnboundGenericProperty<T> {
    private var instance: T? = null

    override fun getPropertyValue(): T = instance ?: factory().also { instance = it }
}

class CapturePropertyName<D>(private val factory: (String) -> D) : GenericDelegateProvider<D> {
    override fun <R> provideDelegate(thisRef: R, property: KProperty<*>): D = factory(property.name)
}

class ThreadLocalProperty<T : Any>(private val factory: () -> T) : UnboundGenericWritableProperty<T> {
    private val holder: ThreadLocal<T> = ThreadLocal()

    override fun getPropertyValue(): T = holder.getOrSet { factory() }

    override fun setPropertyValue(newValue: T) {
        holder.set(newValue)
    }
}
