package st.evening.mc.prelude.api.util.math

import kotlin.math.roundToInt
import kotlin.math.roundToLong

interface Arithmetic<T> {
    val zero: T
    val one: T

    fun fromInt(value: Int): T
    fun T.scale(factor: Float): T

    operator fun T.plus(o: T): T
    operator fun T.minus(o: T): T
    operator fun T.times(o: T): T
    operator fun T.div(o: T): T
    operator fun T.unaryMinus(): T
    operator fun T.compareTo(o: T): Int
}

fun <T> Arithmetic<T>.arithMin(x: T, y: T): T = if (x <= y) x else y

fun <T> Arithmetic<T>.arithMax(x: T, y: T): T = if (x >= y) x else y

context(arith: Arithmetic<T>)
inline fun <T : Any, E> Iterable<E>.arithMinOf(f: (E) -> T): T? {
    val iter = iterator()
    if (!iter.hasNext()) return null
    arith.run {
        var optimum = f(iter.next())
        while (iter.hasNext()) {
            val value = f(iter.next())
            if (value < optimum) {
                optimum = value
            }
        }
        return optimum
    }
}

context(arith: Arithmetic<T>)
inline fun <T : Any, E> Iterable<E>.arithMaxOf(f: (E) -> T): T? {
    val iter = iterator()
    if (!iter.hasNext()) return null
    arith.run {
        var optimum = f(iter.next())
        while (iter.hasNext()) {
            val value = f(iter.next())
            if (value > optimum) {
                optimum = value
            }
        }
        return optimum
    }
}

context(arith: Arithmetic<T>)
inline fun <T, E> Iterable<E>.arithSumOf(f: (E) -> T): T {
    arith.run {
        var acc = zero
        forEach { acc += f(it) }
        return acc
    }
}

context(arith: Arithmetic<T>)
inline fun <T, E> Iterable<E>.arithProductOf(f: (E) -> T): T {
    arith.run {
        var acc = one
        forEach { acc *= f(it) }
        return acc
    }
}

object IntArithmetic : Arithmetic<Int> {
    override val zero: Int
        get() = 0
    override val one: Int
        get() = 1

    override fun fromInt(value: Int): Int = value
    override fun Int.scale(factor: Float): Int = (this * factor).roundToInt()

    override fun Int.plus(o: Int): Int = this + o
    override fun Int.minus(o: Int): Int = this - o
    override fun Int.times(o: Int): Int = this * o
    override fun Int.div(o: Int): Int = this / o
    override fun Int.unaryMinus(): Int = -this
    override fun Int.compareTo(o: Int): Int = compareTo(o)
}

object LongArithmetic : Arithmetic<Long> {
    override val zero: Long
        get() = 0
    override val one: Long
        get() = 1

    override fun fromInt(value: Int): Long = value.toLong()
    override fun Long.scale(factor: Float): Long = (this * factor).roundToLong()

    override fun Long.plus(o: Long): Long = this + o
    override fun Long.minus(o: Long): Long = this - o
    override fun Long.times(o: Long): Long = this * o
    override fun Long.div(o: Long): Long = this / o
    override fun Long.unaryMinus(): Long = -this
    override fun Long.compareTo(o: Long): Int = compareTo(o)
}

object FloatArithmetic : Arithmetic<Float> {
    override val zero: Float
        get() = 0F
    override val one: Float
        get() = 1F

    override fun fromInt(value: Int): Float = value.toFloat()
    override fun Float.scale(factor: Float): Float = this * factor

    override fun Float.plus(o: Float): Float = this + o
    override fun Float.minus(o: Float): Float = this - o
    override fun Float.times(o: Float): Float = this * o
    override fun Float.div(o: Float): Float = this / o
    override fun Float.unaryMinus(): Float = -this
    override fun Float.compareTo(o: Float): Int = compareTo(o)
}

object DoubleArithmetic : Arithmetic<Double> {
    override val zero: Double
        get() = 0.0
    override val one: Double
        get() = 1.0

    override fun fromInt(value: Int): Double = value.toDouble()
    override fun Double.scale(factor: Float): Double = this * factor

    override fun Double.plus(o: Double): Double = this + o
    override fun Double.minus(o: Double): Double = this - o
    override fun Double.times(o: Double): Double = this * o
    override fun Double.div(o: Double): Double = this / o
    override fun Double.unaryMinus(): Double = -this
    override fun Double.compareTo(o: Double): Int = compareTo(o)
}
