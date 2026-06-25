package st.evening.mc.prelude.api.util.collection

import gnu.trove.TByteCollection
import gnu.trove.TIntCollection
import gnu.trove.TLongCollection
import gnu.trove.TShortCollection
import gnu.trove.list.TByteList
import gnu.trove.list.TIntList
import gnu.trove.list.TLongList
import gnu.trove.list.TShortList
import gnu.trove.map.TByteObjectMap
import gnu.trove.map.TIntObjectMap
import gnu.trove.map.TLongObjectMap
import gnu.trove.map.TShortObjectMap
import st.evening.mc.prelude.api.util.data.void

// BYTE ----------------------------------------------------------------------------------------------------------------

operator fun <T> TByteObjectMap<T>.set(key: Byte, value: T): Unit = void { put(key, value) }
operator fun <T> TByteObjectMap<T>.get(key: Byte): T? = get(key)
operator fun <T> TByteObjectMap<T>.contains(key: Byte): Boolean = containsKey(key)
operator fun <T> TByteObjectMap<T>.minusAssign(key: Byte): Unit = void { remove(key) }

fun <T : Any> TByteObjectMap<T>.getOrPutNonNull(key: Byte, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }

fun <T> TByteObjectMap<T>.getOrPut(key: Byte, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> TByteObjectMap<T>.forEachInline(f: (Byte, T) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        iter.advance()
        f(iter.key(), iter.value())
    }
}

fun <T> TByteObjectMap<T>.minKey(): Byte? {
    var best: Byte? = null
    forEachInline { key, _ ->
        if (best?.let { key < it } ?: true) {
            best = key
        }
    }
    return best
}

fun <T> TByteObjectMap<T>.maxKey(): Byte? {
    var best: Byte? = null
    forEachInline { key, _ ->
        if (best?.let { key > it } ?: true) {
            best = key
        }
    }
    return best
}

operator fun TByteCollection.plusAssign(value: Byte): Unit = void { add(value) }
operator fun TByteCollection.contains(value: Byte): Boolean = contains(value)
operator fun TByteCollection.minusAssign(value: Byte): Unit = void { remove(value) }

inline fun TByteCollection.forEachInline(f: (Byte) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.next())
    }
}

fun TByteCollection.min(): Byte? {
    var best: Byte? = null
    forEachInline { value ->
        if (best?.let { value < it } ?: true) {
            best = value
        }
    }
    return best
}

fun TByteCollection.max(): Byte? {
    var best: Byte? = null
    forEachInline { value ->
        if (best?.let { value > it } ?: true) {
            best = value
        }
    }
    return best
}

operator fun TByteList.get(index: Int): Byte = get(index)

// SHORT ---------------------------------------------------------------------------------------------------------------

operator fun <T> TShortObjectMap<T>.set(key: Short, value: T): Unit = void { put(key, value) }
operator fun <T> TShortObjectMap<T>.get(key: Short): T? = get(key)
operator fun <T> TShortObjectMap<T>.contains(key: Short): Boolean = containsKey(key)
operator fun <T> TShortObjectMap<T>.minusAssign(key: Short): Unit = void { remove(key) }

fun <T : Any> TShortObjectMap<T>.getOrPutNonNull(key: Short, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }

fun <T> TShortObjectMap<T>.getOrPut(key: Short, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> TShortObjectMap<T>.forEachInline(f: (Short, T) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        iter.advance()
        f(iter.key(), iter.value())
    }
}

fun <T> TShortObjectMap<T>.minKey(): Short? {
    var best: Short? = null
    forEachInline { key, _ ->
        if (best?.let { key < it } ?: true) {
            best = key
        }
    }
    return best
}

fun <T> TShortObjectMap<T>.maxKey(): Short? {
    var best: Short? = null
    forEachInline { key, _ ->
        if (best?.let { key > it } ?: true) {
            best = key
        }
    }
    return best
}

operator fun TShortCollection.plusAssign(value: Short): Unit = void { add(value) }
operator fun TShortCollection.contains(value: Short): Boolean = contains(value)
operator fun TShortCollection.minusAssign(value: Short): Unit = void { remove(value) }

inline fun TShortCollection.forEachInline(f: (Short) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.next())
    }
}

fun TShortCollection.min(): Short? {
    var best: Short? = null
    forEachInline { value ->
        if (best?.let { value < it } ?: true) {
            best = value
        }
    }
    return best
}

fun TShortCollection.max(): Short? {
    var best: Short? = null
    forEachInline { value ->
        if (best?.let { value > it } ?: true) {
            best = value
        }
    }
    return best
}

operator fun TShortList.get(index: Int): Short = get(index)

// INTEGER -------------------------------------------------------------------------------------------------------------

operator fun <T> TIntObjectMap<T>.set(key: Int, value: T): Unit = void { put(key, value) }
operator fun <T> TIntObjectMap<T>.get(key: Int): T? = get(key)
operator fun <T> TIntObjectMap<T>.contains(key: Int): Boolean = containsKey(key)
operator fun <T> TIntObjectMap<T>.minusAssign(key: Int): Unit = void { remove(key) }

fun <T : Any> TIntObjectMap<T>.getOrPutNonNull(key: Int, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }

fun <T> TIntObjectMap<T>.getOrPut(key: Int, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> TIntObjectMap<T>.forEachInline(f: (Int, T) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        iter.advance()
        f(iter.key(), iter.value())
    }
}

fun <T> TIntObjectMap<T>.minKey(): Int? {
    var best: Int? = null
    forEachInline { key, _ ->
        if (best?.let { key < it } ?: true) {
            best = key
        }
    }
    return best
}

fun <T> TIntObjectMap<T>.maxKey(): Int? {
    var best: Int? = null
    forEachInline { key, _ ->
        if (best?.let { key > it } ?: true) {
            best = key
        }
    }
    return best
}

operator fun TIntCollection.plusAssign(value: Int): Unit = void { add(value) }
operator fun TIntCollection.contains(value: Int): Boolean = contains(value)
operator fun TIntCollection.minusAssign(value: Int): Unit = void { remove(value) }

inline fun TIntCollection.forEachInline(f: (Int) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.next())
    }
}

fun TIntCollection.min(): Int? {
    var best: Int? = null
    forEachInline { value ->
        if (best?.let { value < it } ?: true) {
            best = value
        }
    }
    return best
}

fun TIntCollection.max(): Int? {
    var best: Int? = null
    forEachInline { value ->
        if (best?.let { value > it } ?: true) {
            best = value
        }
    }
    return best
}

operator fun TIntList.get(index: Int): Int = get(index)

// LONG ----------------------------------------------------------------------------------------------------------------

operator fun <T> TLongObjectMap<T>.set(key: Long, value: T): Unit = void { put(key, value) }
operator fun <T> TLongObjectMap<T>.get(key: Long): T? = get(key)
operator fun <T> TLongObjectMap<T>.contains(key: Long): Boolean = containsKey(key)
operator fun <T> TLongObjectMap<T>.minusAssign(key: Long): Unit = void { remove(key) }

fun <T : Any> TLongObjectMap<T>.getOrPutNonNull(key: Long, factory: () -> T): T =
    get(key) ?: factory().also { put(key, it) }

fun <T> TLongObjectMap<T>.getOrPut(key: Long, factory: () -> T): T =
    if (contains(key)) get(key) else factory().also { put(key, it) }

inline fun <T> TLongObjectMap<T>.forEachInline(f: (Long, T) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        iter.advance()
        f(iter.key(), iter.value())
    }
}

fun <T> TLongObjectMap<T>.minKey(): Long? {
    var best: Long? = null
    forEachInline { key, _ ->
        if (best?.let { key < it } ?: true) {
            best = key
        }
    }
    return best
}

fun <T> TLongObjectMap<T>.maxKey(): Long? {
    var best: Long? = null
    forEachInline { key, _ ->
        if (best?.let { key > it } ?: true) {
            best = key
        }
    }
    return best
}

operator fun TLongCollection.plusAssign(value: Long): Unit = void { add(value) }
operator fun TLongCollection.contains(value: Long): Boolean = contains(value)
operator fun TLongCollection.minusAssign(value: Long): Unit = void { remove(value) }

inline fun TLongCollection.forEachInline(f: (Long) -> Unit) {
    val iter = iterator()
    while (iter.hasNext()) {
        f(iter.next())
    }
}

fun TLongCollection.min(): Long? {
    var best: Long? = null
    forEachInline { value ->
        if (best?.let { value < it } ?: true) {
            best = value
        }
    }
    return best
}

fun TLongCollection.max(): Long? {
    var best: Long? = null
    forEachInline { value ->
        if (best?.let { value > it } ?: true) {
            best = value
        }
    }
    return best
}

operator fun TLongList.get(index: Int): Long = get(index)
