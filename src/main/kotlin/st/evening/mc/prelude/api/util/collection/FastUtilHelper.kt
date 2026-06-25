package st.evening.mc.prelude.api.util.collection

import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.ByteCollection
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.LongCollection
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap
import it.unimi.dsi.fastutil.shorts.ShortCollection
import st.evening.mc.prelude.api.util.data.void

object IdentityHashStrategy : Hash.Strategy<Any> {
    // technically not sound, but should work under erasure
    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    inline operator fun <T> invoke(): Hash.Strategy<T> = this as Hash.Strategy<T>

    override fun hashCode(o: Any?): Int = System.identityHashCode(o)

    override fun equals(a: Any?, b: Any?): Boolean = a === b
}

// BYTE ----------------------------------------------------------------------------------------------------------------

operator fun <T> Byte2ObjectMap<T>.set(key: Byte, value: T): Unit = void { put(key, value) }
operator fun <T> Byte2ObjectMap<T>.get(key: Byte): T? = get(key)
operator fun <T> Byte2ObjectMap<T>.contains(key: Byte): Boolean = containsKey(key)
operator fun <T> Byte2ObjectMap<T>.minusAssign(key: Byte): Unit = void { remove(key) }
fun <T : Any> Byte2ObjectMap<T>.getOrPutNonNull(key: Byte, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }
fun <T> Byte2ObjectMap<T>.getOrPut(key: Byte, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> Byte2ObjectMap<T>.forEachInline(f: (Byte, T) -> Unit) {
    byte2ObjectEntrySet().forEach {
        f(it.byteKey, it.value)
    }
}

operator fun ByteCollection.plusAssign(value: Byte): Unit = void { add(value) }
operator fun ByteCollection.contains(value: Byte): Boolean = contains(value)
operator fun ByteCollection.minusAssign(value: Byte): Unit = void { remove(value) }

inline fun ByteCollection.forEachInline(f: (Byte) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.nextByte())
    }
}

// SHORT ---------------------------------------------------------------------------------------------------------------

operator fun <T> Short2ObjectMap<T>.set(key: Short, value: T): Unit = void { put(key, value) }
operator fun <T> Short2ObjectMap<T>.get(key: Short): T? = get(key)
operator fun <T> Short2ObjectMap<T>.contains(key: Short): Boolean = containsKey(key)
operator fun <T> Short2ObjectMap<T>.minusAssign(key: Short): Unit = void { remove(key) }
fun <T : Any> Short2ObjectMap<T>.getOrPutNonNull(key: Short, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }
fun <T> Short2ObjectMap<T>.getOrPut(key: Short, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> Short2ObjectMap<T>.forEachInline(f: (Short, T) -> Unit) {
    short2ObjectEntrySet().forEach {
        f(it.shortKey, it.value)
    }
}

operator fun ShortCollection.plusAssign(value: Short): Unit = void { add(value) }
operator fun ShortCollection.contains(value: Short): Boolean = contains(value)
operator fun ShortCollection.minusAssign(value: Short): Unit = void { remove(value) }

inline fun ShortCollection.forEachInline(f: (Short) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.nextShort())
    }
}

// INT -----------------------------------------------------------------------------------------------------------------

operator fun <T> Int2ObjectMap<T>.set(key: Int, value: T): Unit = void { put(key, value) }
operator fun <T> Int2ObjectMap<T>.get(key: Int): T? = get(key)
operator fun <T> Int2ObjectMap<T>.contains(key: Int): Boolean = containsKey(key)
operator fun <T> Int2ObjectMap<T>.minusAssign(key: Int): Unit = void { remove(key) }
fun <T : Any> Int2ObjectMap<T>.getOrPutNonNull(key: Int, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }
fun <T> Int2ObjectMap<T>.getOrPut(key: Int, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> Int2ObjectMap<T>.forEachInline(f: (Int, T) -> Unit) {
    int2ObjectEntrySet().forEach {
        f(it.intKey, it.value)
    }
}

operator fun IntCollection.plusAssign(value: Int): Unit = void { add(value) }
operator fun IntCollection.contains(value: Int): Boolean = contains(value)
operator fun IntCollection.minusAssign(value: Int): Unit = void { remove(value) }

inline fun IntCollection.forEachInline(f: (Int) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.nextInt())
    }
}

// LONG ----------------------------------------------------------------------------------------------------------------

operator fun <T> Long2ObjectMap<T>.set(key: Long, value: T): Unit = void { put(key, value) }
operator fun <T> Long2ObjectMap<T>.get(key: Long): T? = get(key)
operator fun <T> Long2ObjectMap<T>.contains(key: Long): Boolean = containsKey(key)
operator fun <T> Long2ObjectMap<T>.minusAssign(key: Long): Unit = void { remove(key) }
fun <T : Any> Long2ObjectMap<T>.getOrPutNonNull(key: Long, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }
fun <T> Long2ObjectMap<T>.getOrPut(key: Long, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> Long2ObjectMap<T>.forEachInline(f: (Long, T) -> Unit) {
    long2ObjectEntrySet().forEach {
        f(it.longKey, it.value)
    }
}

operator fun LongCollection.plusAssign(value: Long): Unit = void { add(value) }
operator fun LongCollection.contains(value: Long): Boolean = contains(value)
operator fun LongCollection.minusAssign(value: Long): Unit = void { remove(value) }

inline fun LongCollection.forEachInline(f: (Long) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.nextLong())
    }
}
