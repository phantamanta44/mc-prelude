package st.evening.mc.prelude.api.util.collection

class RepeatIterator<T>(private val element: T, private val size: Int, private var index: Int = 0) : ListIterator<T> {
    override fun hasNext(): Boolean = index < size

    override fun next(): T {
        if (index >= size) throw NoSuchElementException()
        index++
        return element
    }

    override fun hasPrevious(): Boolean = index > 0

    override fun previous(): T {
        if (index <= 0) throw NoSuchElementException()
        index--
        return element
    }

    override fun nextIndex(): Int = index

    override fun previousIndex(): Int = index - 1
}

class RepeatList<T>(private val element: T, override val size: Int) : List<T> {
    override fun contains(element: T): Boolean = element == this.element

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { it == element }

    override fun get(index: Int): T = element

    @Suppress("ReplaceSizeCheckWithIsNotEmpty")
    override fun indexOf(element: T): Int = if (size > 0 && element == this.element) 0 else -1

    @Suppress("ReplaceSizeCheckWithIsNotEmpty")
    override fun lastIndexOf(element: T): Int = if (size > 0 && element == this.element) size - 1 else -1

    override fun isEmpty(): Boolean = size <= 0

    override fun iterator(): Iterator<T> = listIterator()

    override fun listIterator(): ListIterator<T> = RepeatIterator(element, size)

    override fun listIterator(index: Int): ListIterator<T> = RepeatIterator(element, size, index)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = RepeatList(element, toIndex - fromIndex)
}
