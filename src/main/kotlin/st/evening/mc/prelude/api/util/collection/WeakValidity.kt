package st.evening.mc.prelude.api.util.collection

import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.util.data.UnboundGenericWritableProperty
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.function.BiFunction

enum class WeakValidity {
    INVALID,
    WEAK_VALID, // must not return to valid state if only weak references are remaining
    VALID
}

interface WeaklyValid {
    val weakValidity: WeakValidity
}

class WeakValidityReference<T : WeaklyValid>() : UnboundGenericWritableProperty<T?> {
    private var weakRef: WeakReference<T>? = null
    private var strongRef: T? = null

    constructor(initialValue: T?) : this() {
        value = initialValue
    }

    var value: T?
        get() {
            val value = weakRef?.get() ?: return null
            when (value.weakValidity) {
                WeakValidity.INVALID -> {
                    weakRef = null
                    strongRef = null
                    return null
                }

                WeakValidity.WEAK_VALID -> {
                    strongRef = null
                    return value
                }

                WeakValidity.VALID -> {
                    strongRef = value
                    return value
                }
            }
        }
        set(newValue) {
            if (newValue == null) {
                weakRef = null
                strongRef = null
            } else {
                when (newValue.weakValidity) {
                    WeakValidity.INVALID -> {
                        weakRef = null
                        strongRef = null
                    }

                    WeakValidity.WEAK_VALID -> {
                        weakRef = WeakReference(newValue)
                        strongRef = null
                    }

                    WeakValidity.VALID -> {
                        weakRef = WeakReference(newValue)
                        strongRef = newValue
                    }
                }
            }
        }

    fun isPresent(): Boolean = value != null

    override fun getPropertyValue(): T? = value

    override fun setPropertyValue(newValue: T?) {
        value = newValue
    }
}

class WeakValidityProxy(parent: WeaklyValid) : WeaklyValid {
    private val parent: WeaklyValid? by WeakValidityReference(parent)

    override val weakValidity: WeakValidity
        get() = if ((parent ?: return WeakValidity.INVALID).weakValidity == WeakValidity.INVALID) {
            WeakValidity.INVALID
        } else {
            WeakValidity.VALID
        }
}

class WeakValidityMap<K, V>(
    initialCapacity: Int = 16,
    private val loadFactor: Float = 0.75F
) : AbstractMutableMap<K, V>()
    where K : Any, K : WeaklyValid {
    companion object {
        fun <T : WeaklyValid> newSet(initialCapacity: Int = 16, loadFactor: Float = 0.75F): MutableSet<T> =
            Collections.newSetFromMap(WeakValidityMap(initialCapacity, loadFactor))
    }

    private var bins: Array<Entry?> = arrayOfNulls(initialCapacity)
    override var size: Int = 0
        private set
    private var modClock: Int = 0

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = EntrySet()

    private fun appendNode(binIndex: Int, tip: Entry?, key: K, hash: Int, value: V) {
        if (tip != null) {
            tip.next = Entry(tip, key, hash, value)
        } else {
            bins[binIndex] = Entry(null, key, hash, value)
        }
    }

    private inline fun appendNodeToChain(binIndex: Int, newEntry: (Entry?) -> Entry) {
        var tip = bins[binIndex] ?: run {
            bins[binIndex] = newEntry(null)
            return
        }
        while (true) {
            tip = tip.next ?: break
        }
        tip.next = newEntry(tip)
    }

    private fun deleteNode(binIndex: Int, entry: Entry) {
        val prev = entry.prev
        val next = entry.next
        if (prev != null) {
            prev.next = next
        } else {
            bins[binIndex] = next
        }
        if (next != null) {
            next.prev = prev
        }
    }

    private inline fun iterateBin(binIndex: Int, root: Entry?, f: (K, Entry) -> Unit): Entry? {
        var entry = root ?: return null
        while (true) {
            val key = entry.updateValidity()
            if (key == null) {
                deleteNode(binIndex, entry)
                size--
            } else {
                f(key, entry)
            }
            entry = entry.next ?: return entry
        }
    }

    private inline fun iterateBin(binIndex: Int, f: (K, Entry) -> Unit): Entry? =
        iterateBin(binIndex, bins[binIndex], f)

    private fun resize(newSize: Int) {
        val oldBins = bins
        bins = arrayOfNulls(newSize)
        oldBins.forEach { bin ->
            var nextEntry = bin
            while (nextEntry != null) {
                val entry = nextEntry
                nextEntry = entry.next
                if (entry.updateValidity() != null) {
                    appendNodeToChain(getBinIndex(entry.hash)) {
                        entry.prev = it
                        entry.next = null
                        return@appendNodeToChain entry
                    }
                } else {
                    size--
                }
            }
        }
    }

    private fun resizeIfOverloaded() {
        if (size > bins.size * loadFactor) {
            resize(bins.size * 2)
        }
    }

    private fun getHash(key: K): Int = System.identityHashCode(key)

    private fun getBinIndex(hash: Int): Int = hash.mod(bins.size)

    private fun getBinIndex(key: K): Int = getBinIndex(getHash(key))

    override fun get(key: K): V? {
        iterateBin(getBinIndex(key)) { entryKey, entry ->
            if (entryKey === key) return entry.value
        }
        return null
    }

    override fun containsKey(key: K): Boolean {
        iterateBin(getBinIndex(key)) { entryKey, _ ->
            if (entryKey === key) return true
        }
        return false
    }

    override fun put(key: K, value: V): V? {
        val hash = getHash(key)
        val binIndex = getBinIndex(hash)
        val tip = iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                val oldValue = entry.value
                entry.value = value
                return oldValue
            }
        }
        appendNode(binIndex, tip, key, hash, value)
        size++
        resizeIfOverloaded()
        modClock++
        return null
    }

    override fun putIfAbsent(key: K, value: V): V? {
        val hash = getHash(key)
        val binIndex = getBinIndex(hash)
        val tip = iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                return entry.value
            }
        }
        appendNode(binIndex, tip, key, hash, value)
        size++
        resizeIfOverloaded()
        modClock++
        return null
    }

    override fun remove(key: K): V? {
        val binIndex = getBinIndex(key)
        iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                deleteNode(binIndex, entry)
                size--
                modClock++
                return entry.value
            }
        }
        return null
    }

    override fun remove(key: K, value: V): Boolean {
        val binIndex = getBinIndex(key)
        iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                if (entry.value == value) {
                    deleteNode(binIndex, entry)
                    size--
                    modClock++
                    return true
                }
                return false
            }
        }
        return false
    }

    override fun clear() {
        bins.fill(null)
        size = 0
        modClock++
    }

    override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? {
        val hash = getHash(key)
        val binIndex = getBinIndex(hash)
        val tip = iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                remappingFunction.apply(key, entry.value)?.let {
                    entry.value = it
                    return it
                }
                deleteNode(binIndex, entry)
                size--
                modClock++
                return null
            }
        }
        remappingFunction.apply(key, null)?.let {
            appendNode(binIndex, tip, key, hash, it)
            size++
            resizeIfOverloaded()
            modClock++
            return it
        }
        return null
    }

    override fun computeIfPresent(key: K, remappingFunction: BiFunction<in K, in V & Any, out V?>): V? {
        val binIndex = getBinIndex(key)
        iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                entry.value?.let { value ->
                    remappingFunction.apply(key, value)?.let {
                        entry.value = it
                        return it
                    }
                    deleteNode(binIndex, entry)
                    size--
                    modClock++
                }
                return null
            }
        }
        return null
    }

    override fun computeIfAbsent(key: K, mappingFunction: java.util.function.Function<in K, out V>): V {
        val hash = getHash(key)
        val binIndex = getBinIndex(hash)
        val tip = iterateBin(binIndex) { entryKey, entry ->
            if (entryKey === key) {
                entry.value?.let { return it }
                mappingFunction.apply(key)?.let {
                    entry.value = it
                    return it
                }
                @Suppress("UNCHECKED_CAST")
                return null as V // return type of computeIfAbsent is intentionally incorrect in kotlin; see KT-10982
            }
        }
        mappingFunction.apply(key)?.let {
            appendNode(binIndex, tip, key, hash, it)
            size++
            resizeIfOverloaded()
            modClock++
            return it
        }
        @Suppress("UNCHECKED_CAST")
        return null as V // ditto^
    }

    override fun replace(key: K, value: V): V? {
        iterateBin(getBinIndex(key)) { entryKey, entry ->
            if (entryKey === key) {
                val oldValue = entry.value
                entry.value = value
                return oldValue
            }
        }
        return null
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        iterateBin(getBinIndex(key)) { entryKey, entry ->
            if (entryKey === key) {
                if (entry.value == oldValue) {
                    entry.value = newValue
                    return true
                }
                return false
            }
        }
        return false
    }

    override fun replaceAll(function: BiFunction<in K, in V, out V>) {
        bins.forEachIndexed { index, root ->
            iterateBin(index, root) { key, entry ->
                entry.value = function.apply(key, entry.value)
            }
        }
    }

    private inner class Entry(
        var prev: Entry?,
        key: K,
        val hash: Int,
        override var value: V
    ) : MutableMap.MutableEntry<K, V> {
        var next: Entry? = null
        val keyRef: WeakReference<K> = WeakReference(key)
        var strongKeyRef: K? = key

        override val key: K
            get() = keyRef.get() ?: throw IllegalStateException("Key already invalidated!")

        override fun setValue(newValue: V): V = value.also { value = newValue }

        fun updateValidity(): K? {
            keyRef.get()?.let {
                when (it.weakValidity) {
                    WeakValidity.INVALID -> {
                        strongKeyRef = null
                        return null
                    }

                    WeakValidity.WEAK_VALID -> {
                        strongKeyRef = null
                        return it
                    }

                    WeakValidity.VALID -> {
                        strongKeyRef = it
                        return it
                    }
                }
            }
            return null
        }
    }

    private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int
            get() = this@WeakValidityMap.size

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()

        private fun containsEntry(entry: Entry): Boolean {
            iterateBin(getBinIndex(entry.hash)) { _, entryHere ->
                if (entryHere === entry) return true
            }
            return false
        }

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
            element is WeakValidityMap<K, V>.Entry && containsEntry(element)

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            if (element !is WeakValidityMap<K, V>.Entry || !containsEntry(element)) return false
            val key = element.keyRef.get() ?: return false
            deleteNode(getBinIndex(key), element)
            this@WeakValidityMap.size--
            modClock++
            return true
        }

        override fun clear() = this@WeakValidityMap.clear()

        override fun isEmpty(): Boolean = this@WeakValidityMap.isEmpty()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = IteratorImpl()

        private inner class IteratorImpl : MutableIterator<MutableMap.MutableEntry<K, V>> {
            private var iterBin: Int = -1
            private var iterEntry: Entry? = null
            private var next: Pair<K, Entry>? = null // must maintain a strong reference to the key to prevent it from
            private var current: Pair<K, Entry>? = null // getting evicted before the next next() call
            private var expectedModClock: Int = modClock

            override fun hasNext(): Boolean {
                if (next != null) return true
                if (modClock != expectedModClock) throw ConcurrentModificationException()
                if (iterBin >= bins.size) return false
                var binIndex = iterBin
                var entry = iterEntry
                if (entry != null) {
                    entry = entry.next
                }
                while (true) {
                    while (entry == null) {
                        binIndex++
                        if (binIndex >= bins.size) {
                            iterBin = binIndex
                            return false
                        }
                        entry = bins[binIndex]
                    }
                    while (entry != null) {
                        val key = entry.updateValidity()
                        if (key != null) {
                            iterBin = binIndex
                            iterEntry = entry
                            next = key to entry
                            return true
                        }
                        deleteNode(binIndex, entry) // is it safe to mutate the map here?
                        this@WeakValidityMap.size--
                        entry = entry.next
                    }
                }
            }

            override fun next(): MutableMap.MutableEntry<K, V> {
                if (!hasNext()) throw NoSuchElementException()
                val result = next ?: throw IllegalStateException()
                current = result
                next = null
                return result.second
            }

            override fun remove() {
                if (modClock != expectedModClock) throw ConcurrentModificationException()
                val (key, entry) = current ?: throw IllegalStateException()
                deleteNode(getBinIndex(key), entry)
                this@WeakValidityMap.size--
                current = null // this destroys the reference to the key, so the entry should not be used afterwards!
                modClock++
                expectedModClock++
            }
        }
    }
}

class WeakValidityCache<K, V>(
    private val backing: MutableMap<K, WeakValidityReference<V>>,
    private var scrubThreshold: Int
) where V : Any, V : WeaklyValid {
    companion object {
        fun <K, V : WeaklyValid> newCache(
            initialCapacity: Int = 16,
            loadFactor: Float = 0.75F,
            scrubThreshold: Int = ((initialCapacity * loadFactor).toInt() - 1).coerceAtLeast(1)
        ): WeakValidityCache<K, V> = WeakValidityCache(HashMap(initialCapacity, loadFactor), scrubThreshold)
    }

    val size: Int
        get() {
            scrub()
            return backing.size
        }

    operator fun get(key: K): V? {
        val ref = backing[key] ?: return null
        ref.value?.let { return it }
        backing.remove(key)
        return null
    }

    operator fun contains(key: K): Boolean {
        val ref = backing[key] ?: return false
        if (ref.value != null) return true
        backing.remove(key)
        return false
    }

    operator fun set(key: K, value: V) {
        val ref = backing[key]
        if (ref != null) {
            ref.value = value
        } else {
            backing[key] = WeakValidityReference(value)
            scrubIfNeeded()
        }
    }

    @PreludeInternal
    fun getRaw(key: K): WeakValidityReference<V>? = backing[key]

    @PreludeInternal
    fun putRaw(key: K, value: WeakValidityReference<V>) {
        backing[key] = value
    }

    @OptIn(PreludeInternal::class)
    inline fun getOrPut(key: K, factory: () -> V): V {
        val ref = getRaw(key)
        if (ref == null) {
            val newValue = factory()
            putRaw(key, WeakValidityReference(newValue))
            scrubIfNeeded()
            return newValue
        } else {
            ref.value?.let { return it }
            val newValue = factory()
            ref.value = newValue
            return newValue
        }
    }

    fun remove(key: K): V? {
        return backing.remove(key)?.value
    }

    fun clear() {
        backing.clear()
    }

    fun scrubIfNeeded() {
        if (backing.size > scrubThreshold) {
            scrub()
        }
    }

    fun scrub() {
        backing.values.removeIf { !it.isPresent() }
        if (backing.size >= scrubThreshold) {
            scrubThreshold *= 2
        }
    }
}
