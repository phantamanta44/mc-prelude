package st.evening.mc.prelude.api.util.collection

import java.util.Collections

fun <T> Collection<T>.asOrToMutableList(): MutableList<T> = this as? MutableList<T> ?: toMutableList()
fun <T> Collection<T>.asOrToMutableSet(): MutableSet<T> = this as? MutableSet<T> ?: toMutableSet()
fun <K, V> Map<K, V>.asOrToMutableMap(): MutableMap<K, V> = this as? MutableMap<K, V> ?: toMutableMap()

fun <T> Collection<T>.asUnmodifiable(): Collection<T> = Collections.unmodifiableCollection(this)
fun <T> List<T>.asUnmodifiableList(): List<T> = Collections.unmodifiableList(this)
fun <T> Set<T>.asUnmodifiableSet(): Set<T> = Collections.unmodifiableSet(this)
fun <K, V> Map<K, V>.asUnmodifiableMap(): Map<K, V> = Collections.unmodifiableMap(this)

fun <T> Array<T>.getCyclic(index: Int): T = this[index.mod(size)]
fun <T> List<T>.getCyclic(index: Int): T = this[index.mod(size)]

fun <T> List<T>.containsIndex(index: Int): Boolean = index >= 0 && index < this.size
fun <T> Array<T>.containsIndex(index: Int): Boolean = index >= 0 && index < this.size

fun <T> iteratorOf(item: T): Iterator<T> = object : Iterator<T> {
    private var hasNext: Boolean = true

    override fun hasNext(): Boolean = hasNext

    override fun next(): T {
        if (!hasNext) throw NoSuchElementException()
        hasNext = false
        return item
    }
}

fun <T> Iterator<T>.asFakeMutable(): MutableIterator<T> = this as? MutableIterator<T> ?: object : MutableIterator<T> {
    override fun hasNext(): Boolean = this@asFakeMutable.hasNext()
    override fun next(): T = this@asFakeMutable.next()
    override fun remove() = throw UnsupportedOperationException()
}

fun <A, B> Iterator<A>.map(f: (A) -> B): Iterator<B> = object : Iterator<B> {
    override fun hasNext(): Boolean = this@map.hasNext()
    override fun next(): B = f(this@map.next())
}

fun <A, B> Iterator<A>.mapFakeMutable(f: (A) -> B): MutableIterator<B> = object : MutableIterator<B> {
    override fun hasNext(): Boolean = this@mapFakeMutable.hasNext()
    override fun next(): B = f(this@mapFakeMutable.next())
    override fun remove() = throw UnsupportedOperationException()
}

fun <A, B> MutableIterator<A>.mapMutable(f: (A) -> B): MutableIterator<B> = object : MutableIterator<B> {
    override fun hasNext(): Boolean = this@mapMutable.hasNext()
    override fun next(): B = f(this@mapMutable.next())
    override fun remove() = this@mapMutable.remove()
}

inline fun <A, reified B> Collection<A>.mapToTypedArray(f: (A) -> B): Array<B> {
    val iter = iterator()
    @Suppress("UNCHECKED_CAST") // technically not sound, but should work under erasure
    return Array(size) { f(iter.next()) } // assumes that the array is initialized in ascending order
}

fun Iterable<Byte>.collectToByteArray(): ByteArray =
    if (this is Collection<Byte>) toByteArray() else toList().toByteArray()

fun Iterable<Char>.collectToCharArray(): CharArray =
    if (this is Collection<Char>) toCharArray() else toList().toCharArray()

fun Iterable<Boolean>.collectToBoolArray(): BooleanArray =
    if (this is Collection<Boolean>) toBooleanArray() else toList().toBooleanArray()

fun Iterable<Short>.collectToShortArray(): ShortArray =
    if (this is Collection<Short>) toShortArray() else toList().toShortArray()

fun Iterable<Int>.collectToIntArray(): IntArray =
    if (this is Collection<Int>) toIntArray() else toList().toIntArray()

fun Iterable<Long>.collectToLongArray(): LongArray =
    if (this is Collection<Long>) toLongArray() else toList().toLongArray()

fun Iterable<Float>.collectToFloatArray(): FloatArray =
    if (this is Collection<Float>) toFloatArray() else toList().toFloatArray()

fun Iterable<Double>.collectToDoubleArray(): DoubleArray =
    if (this is Collection<Double>) toDoubleArray() else toList().toDoubleArray()

inline fun <reified T> Iterable<T>.collectToArray(): Array<T> =
    if (this is Collection<T>) toTypedArray() else toList().toTypedArray()

inline fun <T> Iterable<T>.sumOf(f: (T) -> Float): Float { // not in kotlin stdlib for some reason as of 2.3.20
    var sum = 0F
    forEach {
        sum += f(it)
    }
    return sum
}
