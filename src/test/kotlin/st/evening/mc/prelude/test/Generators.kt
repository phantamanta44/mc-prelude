package st.evening.mc.prelude.test

import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveInt
import st.evening.mc.prelude.api.util.math.Vec2i
import kotlin.math.absoluteValue

fun Arb.Companion.nonZeroInt(range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): Arb<Int> =
    int(range).filter { it != 0 }

fun Arb<Int>.withDivisor(): Arb<Vec2i> = flatMap { dividend ->
    Arb.positiveInt(dividend.absoluteValue.coerceAtLeast(1)).map { Vec2i(dividend, it) }
}
