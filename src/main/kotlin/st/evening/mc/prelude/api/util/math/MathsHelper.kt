package st.evening.mc.prelude.api.util.math

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.PI

@OptIn(ExperimentalContracts::class)
inline fun Byte.zeroUnless(condition: () -> Boolean): Byte {
    contract {
        callsInPlace(condition, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition()) this else 0
}

@OptIn(ExperimentalContracts::class)
inline fun Short.zeroUnless(condition: () -> Boolean): Short {
    contract {
        callsInPlace(condition, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition()) this else 0
}

@OptIn(ExperimentalContracts::class)
inline fun Int.zeroUnless(condition: () -> Boolean): Int {
    contract {
        callsInPlace(condition, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition()) this else 0
}

@OptIn(ExperimentalContracts::class)
inline fun Long.zeroUnless(condition: () -> Boolean): Long {
    contract {
        callsInPlace(condition, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition()) this else 0
}

object MathsHelper {
    const val PI_F: Float = PI.toFloat()
    const val D2R: Double = PI / 180.0
    const val D2R_F: Float = PI_F / 180F

    fun lerp(from: Float, to: Float, progress: Float): Float = from + (to - from) * progress
    fun lerp(from: Double, to: Double, progress: Double): Double = from + (to - from) * progress

    fun ceilSquare(n: Int): Int {
        var k = 0
        var k2 = 0
        var dk2 = 1
        while (true) {
            if (n <= k2) return k
            k++
            k2 += dk2
            dk2 += 2
        }
    }
}
