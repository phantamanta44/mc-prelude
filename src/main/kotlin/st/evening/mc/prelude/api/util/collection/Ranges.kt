package st.evening.mc.prelude.api.util.collection

class IntRangeSet(val range: IntRange) : AbstractSet<Int>() {
    override val size: Int
        get() = range.last - range.first + 1 // this may overflow for very large ranges!

    override fun isEmpty(): Boolean = range.isEmpty()

    override fun contains(element: Int): Boolean = element in range

    override fun iterator(): Iterator<Int> = range.iterator()
}

class LongRangeSet(val range: LongRange) : AbstractSet<Long>() {
    override val size: Int
        get() = (range.last - range.first).toInt() + 1 // this may overflow for even moderately large ranges!

    override fun isEmpty(): Boolean = range.isEmpty()

    override fun contains(element: Long): Boolean = element in range

    override fun iterator(): Iterator<Long> = range.iterator()
}
