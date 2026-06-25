package st.evening.mc.prelude.api.util.collection

import st.evening.mc.prelude.api.util.data.mapEnumConstants

interface TotalMap<K, V> {
    val size: Int

    val entries: Set<Map.Entry<K, V>>

    val values: Collection<V>

    operator fun get(key: K): V
}

interface MutableTotalMap<K, V> : TotalMap<K, V> {
    override val entries: Set<MutableMap.MutableEntry<K, V>>

    operator fun set(key: K, value: V): V
}

abstract class AbstractMutableTotalMap<K, V> : MutableTotalMap<K, V> {
    override val values: Collection<V> = ValueView()

    private inner class ValueView : Collection<V> {
        override val size: Int
            get() = this@AbstractMutableTotalMap.size

        override fun contains(element: V): Boolean = entries.any { it.value == element }

        override fun containsAll(elements: Collection<V>): Boolean = elements.all { contains(it) }

        override fun isEmpty(): Boolean = entries.isEmpty()

        override fun iterator(): Iterator<V> = entries.iterator().map { it.value }
    }
}

class TotalEnumMap<K : Enum<K>, V>(val keyType: Class<K>, initializer: (K) -> V) : AbstractMutableTotalMap<K, V>() {
    companion object {
        inline operator fun <reified K : Enum<K>, V> invoke(noinline initializer: (K) -> V): TotalEnumMap<K, V> =
            TotalEnumMap(K::class.java, initializer)
    }

    private val _entries: List<Entry> = keyType.mapEnumConstants { Entry(it, initializer(it)) }

    override val size: Int
        get() = _entries.size

    override val entries: Set<MutableMap.MutableEntry<K, V>> = EntrySet()

    override fun get(key: K): V = _entries[key.ordinal].value

    override fun set(key: K, value: V): V = _entries[key.ordinal].setValue(value)

    private inner class Entry(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = value.also { value = newValue }
    }

    private inner class EntrySet : Set<MutableMap.MutableEntry<K, V>> {
        override val size: Int
            get() = this@TotalEnumMap.size

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean = _entries.contains(element)

        override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean =
            _entries.containsAll(elements)

        override fun isEmpty(): Boolean = _entries.isEmpty()

        override fun iterator(): Iterator<MutableMap.MutableEntry<K, V>> = _entries.iterator()
    }
}
