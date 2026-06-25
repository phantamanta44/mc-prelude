package st.evening.mc.prelude.api.util.math

import net.minecraft.util.math.MathHelper

infix fun Int.floorDiv(divisor: Int): Int {
    val quot = this / divisor
    return if (this xor divisor < 0 && quot * divisor != this) quot - 1 else quot
}

infix fun Int.ceilDiv(divisor: Int): Int {
    val quot = this / divisor
    return if (this xor divisor >= 0 && quot * divisor != this) quot + 1 else quot
}

infix fun Int.ceilDivPos(divisor: Int): Int = (this + divisor - 1) / divisor // could overflow unexpectedly!

object FastMathsHelper {
    private val EXP10_INT: IntArray =
        intArrayOf(10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000)
    private const val EXP10_INT_LIMIT: Int = 9
    private val EXP10_LONG: LongArray = longArrayOf(
        10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000, 10_000_000_000,
        100_000_000_000, 1_000_000_000_000, 10_000_000_000_000, 100_000_000_000_000, 1_000_000_000_000_000,
        10_000_000_000_000_000, 100_000_000_000_000_000, 1_000_000_000_000_000_000
    )
    private const val EXP10_LONG_LIMIT: Int = 18
    private val LOG2_TO_10: IntArray = intArrayOf(
        0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10,
        10, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18, 18
    )

    fun exp10Int(power: Int): Int = when {
        power < 0 -> 0
        power == 0 -> 1
        power > EXP10_INT_LIMIT -> Int.MAX_VALUE
        else -> EXP10_INT[power - 1]
    }

    fun exp10Long(power: Int): Long = when {
        power < 0 -> 0L
        power == 0 -> 1L
        power > EXP10_LONG_LIMIT -> Long.MAX_VALUE
        else -> EXP10_LONG[power - 1]
    }

    fun floorLog2(n: Int): Int = MathHelper.log2(n)

    fun floorLog2(n: Long): Int = 63 - n.countLeadingZeroBits()

    fun ceilLog2(n: Int): Int = 32 - (n - 1).countLeadingZeroBits()

    fun ceilLog2(n: Long): Int = 64 - (n - 1).countLeadingZeroBits()

    fun floorLog10(n: Int): Int {
        val k = LOG2_TO_10[floorLog2(n)]
        if (k >= EXP10_INT_LIMIT) return k
        return if (n >= EXP10_INT[k]) k + 1 else k
    }

    fun floorLog10(n: Long): Int {
        val k = LOG2_TO_10[floorLog2(n)]
        if (k >= EXP10_LONG_LIMIT) return k
        return if (n >= EXP10_LONG[k]) k + 1 else k
    }

    fun ceilLog10(n: Int): Int {
        val k = LOG2_TO_10[ceilLog2(n)]
        if (k <= 0) return if (n <= 1) 0 else 1
        return if (n < EXP10_INT[k - 1]) k else k + 1
    }

    fun ceilLog10(n: Long): Int {
        val k = LOG2_TO_10[ceilLog2(n)]
        if (k <= 0) return if (n <= 1L) 0 else 1
        return if (n < EXP10_LONG[k - 1]) k else k + 1
    }
}
