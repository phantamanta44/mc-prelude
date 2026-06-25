package st.evening.mc.prelude.api.util.collection

import java.util.Collections
import java.util.NavigableSet
import java.util.SortedSet

class ImmutableLinkedSortedSet<T> private constructor(
    private val order: List<T>,
    private val indexTable: Map<T, IndexedValue<T>>,
    private val comparator: Comparator<in T>,
    private var downSet: NavigableSet<T>?
) : AbstractSet<T>(), NavigableSet<T> {
    companion object {
        fun <T> ofBy(elements: Collection<T>, comparator: Comparator<T>): ImmutableLinkedSortedSet<T> {
            if (elements.isEmpty()) {
                return ImmutableLinkedSortedSet(emptyList(), emptyMap(), comparator, Collections.emptyNavigableSet())
            } else {
                val order = elements.sortedWith(comparator).toList()
                return ImmutableLinkedSortedSet(order, order.withIndex().associateBy { it.value }, comparator, null)
            }
        }

        fun <T : Comparable<T>> of(elements: Collection<T>): ImmutableLinkedSortedSet<T> =
            ofBy(elements, naturalOrder())
    }

    override val size: Int
        get() = order.size

    override fun contains(element: T): Boolean = indexTable.containsKey(element)

    override fun isEmpty(): Boolean = order.isEmpty()

    override fun comparator(): Comparator<in T> = comparator

    override fun iterator(): MutableIterator<T> = order.iterator().asFakeMutable()

    override fun lower(e: T): T? {
        indexTable[e]?.let { return order.getOrNull(it.index - 1) }
        return searchDown(e)?.let { order[it] }
    }

    override fun floor(e: T): T? {
        indexTable[e]?.let { return it.value }
        return searchDown(e)?.let { order[it] }
    }

    override fun ceiling(e: T): T? {
        indexTable[e]?.let { return it.value }
        return searchUp(e)?.let { order[it] }
    }

    override fun higher(e: T): T? {
        indexTable[e]?.let { return order.getOrNull(it.index + 1) }
        return searchUp(e)?.let { order[it] }
    }

    private fun searchDown(e: T): Int? =
        (-order.binarySearch { comparator.compare(it, e) } - 2).takeIf { it >= 0 }

    private fun searchUp(e: T): Int? =
        (-order.binarySearch { comparator.compare(it, e) } - 1).takeIf { it < order.size }

    override fun descendingSet(): NavigableSet<T> {
        downSet?.let { return it }
        val rev = order.asReversed()
        return ImmutableLinkedSortedSet(rev, rev.withIndex().associateBy { it.value }, comparator.reversed(), this)
            .also { downSet = it }
    }

    override fun descendingIterator(): Iterator<T> = descendingSet().iterator()

    override fun subSet(
        fromElement: T?,
        fromInclusive: Boolean,
        toElement: T?,
        toInclusive: Boolean
    ): NavigableSet<T> {
        val fromIndex = getSubSetFromIndex(fromElement, fromInclusive)
        if (fromIndex >= order.size) return Collections.emptyNavigableSet()
        val toIndex = getSubSetToIndex(toElement, toInclusive)
        if (toIndex <= fromIndex) return Collections.emptyNavigableSet()
        val subOrder = order.subList(fromIndex, toIndex)
        return ImmutableLinkedSortedSet(subOrder, subOrder.withIndex().associateBy { it.value }, comparator, null)
    }

    private fun getSubSetFromIndex(element: T?, inclusive: Boolean): Int {
        if (element == null) return 0
        indexTable[element]?.let { entry ->
            return if (inclusive) entry.index else (entry.index + 1)
        }
        return searchUp(element) ?: Integer.MAX_VALUE
    }

    private fun getSubSetToIndex(element: T?, inclusive: Boolean): Int {
        if (element == null) return order.size
        indexTable[element]?.let { entry ->
            return if (inclusive) entry.index + 1 else entry.index
        }
        return searchDown(element) ?: 0
    }

    override fun subSet(fromElement: T, toElement: T): SortedSet<T> = subSet(fromElement, true, toElement, false)

    override fun headSet(toElement: T, inclusive: Boolean): NavigableSet<T> = subSet(null, true, toElement, inclusive)

    override fun headSet(toElement: T): SortedSet<T> = headSet(toElement, false)

    override fun tailSet(fromElement: T, inclusive: Boolean): NavigableSet<T> =
        subSet(fromElement, inclusive, null, false)

    override fun tailSet(fromElement: T): SortedSet<T> = tailSet(fromElement, true)

    override fun first(): T? = order.firstOrNull()

    override fun last(): T? = order.lastOrNull()

    override fun add(element: T?): Nothing = throw UnsupportedOperationException()
    override fun remove(element: T?): Nothing = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<T?>): Nothing = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<T?>): Nothing = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<T?>): Nothing = throw UnsupportedOperationException()
    override fun clear(): Nothing = throw UnsupportedOperationException()
    override fun pollFirst(): Nothing = throw UnsupportedOperationException()
    override fun pollLast(): Nothing = throw UnsupportedOperationException()
}
