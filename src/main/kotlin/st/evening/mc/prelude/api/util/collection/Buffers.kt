package st.evening.mc.prelude.api.util.collection

import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.type.Maybe

class SingleBuffer<T> : AbstractList<T>() {
    override var size: Int = 0
        private set
    private var value: T? = null

    @PreludeInternal
    @Suppress("UNCHECKED_CAST")
    fun getUnchecked(): T = value as T

    @PreludeInternal
    fun setRaw(newValue: T) {
        value = newValue
    }

    @OptIn(PreludeInternal::class)
    fun peek(): Maybe<T> = if (isEmpty()) Maybe.Nothing() else Maybe.Just(getUnchecked())

    @OptIn(PreludeInternal::class)
    fun poll(): Maybe<T> {
        if (isEmpty()) return Maybe.Nothing()
        val value = getUnchecked()
        clear()
        return Maybe.Just(value)
    }

    @OptIn(PreludeInternal::class)
    fun get(): T {
        if (isEmpty()) throw NoSuchElementException()
        return getUnchecked()
    }

    @OptIn(PreludeInternal::class)
    override fun get(index: Int): T {
        if (index != 0 || isEmpty()) throw IndexOutOfBoundsException("Index out of bounds: $index")
        return getUnchecked()
    }

    fun set(newValue: T) {
        size = 1
        value = newValue
    }

    fun clear() {
        size = 0
        value = null
    }

    @OptIn(PreludeInternal::class)
    inline fun merge(newValue: T, f: (T, T) -> T) {
        if (isEmpty()) {
            set(newValue)
        } else {
            setRaw(f(getUnchecked(), newValue))
        }
    }
}

class RingBuffer<T>(capacity: Int) : AbstractList<T>() {
    @Suppress("UNCHECKED_CAST")
    private val elements: Array<T?> = arrayOfNulls<Any>(capacity) as Array<T?>
    private var writeHead: Int = 0
    private var readHead: Int = 0
    override var size: Int = 0
        private set
    private var modClock: Int = 0

    val capacity: Int
        get() = elements.size

    fun isFull(): Boolean = size >= capacity

    fun offer(value: T): Boolean {
        modClock++
        elements[writeHead] = value
        writeHead = (writeHead + 1) % capacity
        if (isFull()) {
            readHead = writeHead
            return true
        } else {
            size++
            return false
        }
    }

    @PreludeInternal
    @Suppress("UNCHECKED_CAST")
    fun getUnchecked(index: Int): T = elements[index] as T

    @OptIn(PreludeInternal::class)
    override fun get(index: Int): T {
        if (index !in indices) throw IndexOutOfBoundsException()
        return getUnchecked((readHead + index) % capacity)
    }

    @OptIn(PreludeInternal::class)
    fun peek(): Maybe<T> = if (isEmpty()) Maybe.Nothing() else Maybe.Just(getUnchecked(readHead))

    @OptIn(PreludeInternal::class)
    fun poll(): Maybe<T> {
        if (isEmpty()) return Maybe.Nothing()
        val value = getUnchecked(readHead)
        modClock++
        elements[readHead] = null
        readHead = (readHead + 1) % capacity
        size--
        return Maybe.Just(value)
    }

    fun clear() {
        if (isEmpty()) return
        modClock++
        elements.fill(null)
        writeHead = 0
        readHead = 0
        size = 0
    }

    override fun iterator(): Iterator<T> = IteratorImpl()

    private inner class IteratorImpl : Iterator<T> {
        private var next: Int = readHead
        private var remaining: Int = size
        private val expectedModClock: Int = modClock

        override fun hasNext(): Boolean = remaining > 0

        @OptIn(PreludeInternal::class)
        override fun next(): T {
            if (modClock != expectedModClock) throw ConcurrentModificationException()
            if (remaining <= 0) throw NoSuchElementException()
            val element = getUnchecked(next)
            next = (next + 1) % capacity
            remaining--
            return element
        }
    }
}

class HoleBuffer<T : Any> private constructor(
    private var buffer: Array<T?>,
    private var writeHead: Int
) : AbstractList<T>() {
    override var size: Int = writeHead
    private var modClock: Int = 0

    @Suppress("UNCHECKED_CAST")
    constructor(initialCapacity: Int = 10) : this(arrayOfNulls<Any>(initialCapacity) as Array<T?>, 0)

    @Suppress("UNCHECKED_CAST")
    constructor(elements: Collection<T>) : this(elements.toTypedArray<Any?>() as Array<T?>, elements.size)

    fun add(element: T) {
        if (writeHead >= buffer.size) {
            buffer = buffer.copyOf(buffer.size * 2)
        }
        buffer[writeHead] = element
        writeHead++
        size++
        modClock++
    }

    fun addAll(elements: Collection<T>) {
        if (elements.isEmpty()) return
        val neededSize = writeHead + elements.size
        if (neededSize > buffer.size) {
            buffer = buffer.copyOf(neededSize.coerceAtLeast(buffer.size * 2))
        }
        var i = writeHead
        elements.forEach {
            buffer[i++] = it
        }
        writeHead = i
        size += elements.size
        modClock++
    }

    override fun get(index: Int): T {
        if (index !in indices) throw IndexOutOfBoundsException()
        var i = 0
        buffer.forEach {
            if (it != null) {
                if (i == index) return it
                i++
            }
        }
        throw IllegalStateException()
    }

    inline fun removeIf(f: (T) -> Boolean) {
        val iter = iterator()
        while (iter.hasNext()) {
            if (f(iter.next())) {
                iter.remove()
            }
        }
    }

    override fun iterator(): MutableIterator<T> = IteratorImpl()

    private inner class IteratorImpl : MutableIterator<T> {
        private var pos: Int? = -1
        private var needNext: Boolean = true
        private var current: Int? = null
        private var expectedModClock: Int = modClock

        private fun normalize(): Int? {
            if (modClock != expectedModClock) throw ConcurrentModificationException()
            var i = pos ?: return null
            if (!needNext) return i
            while (true) {
                i++
                if (i >= buffer.size) {
                    pos = null
                    return null
                }
                if (buffer[i] != null) break
            }
            pos = i
            needNext = false
            return i
        }

        override fun hasNext(): Boolean = normalize() != null

        override fun next(): T {
            val here = normalize() ?: throw NoSuchElementException()
            current = here
            needNext = true
            return buffer[here]!!
        }

        override fun remove() {
            if (modClock != expectedModClock) throw ConcurrentModificationException()
            buffer[current ?: throw IllegalStateException()] = null
            size--
            modClock++
            expectedModClock++
            current = null
        }
    }
}
