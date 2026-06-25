package st.evening.mc.prelude.test

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.checkAll
import st.evening.mc.prelude.api.util.math.FastMathsHelper
import st.evening.mc.prelude.api.util.math.ceilDiv
import st.evening.mc.prelude.api.util.math.ceilDivPos
import st.evening.mc.prelude.api.util.math.floorDiv
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow

private fun ifloor(n: Double): Int = floor(n).toInt()
private fun iceil(n: Double): Int = ceil(n).toInt()

class MathsTests : FunSpec({
    context("Fast maths tests") {
        test("Integer floorDiv") {
            checkAll(Arb.int(min = -Int.MAX_VALUE /* avoid overflow */), Arb.nonZeroInt()) { p, q ->
                p floorDiv q shouldBe ifloor(p / q.toDouble())
            }
        }
        test("Integer ceilDiv") {
            checkAll(Arb.int(min = -Int.MAX_VALUE /* avoid overflow */), Arb.nonZeroInt()) { p, q ->
                p ceilDiv q shouldBe iceil(p / q.toDouble())
            }
        }
        test("Integer ceilDivPos") {
            checkAll(Arb.nonNegativeInt(Int.MAX_VALUE / 2 /* avoid overflow */).withDivisor()) { (p, q) ->
                p ceilDivPos q shouldBe iceil(p / q.toDouble())
            }
        }
        test("exp10Int") {
            checkAll(Arb.nonNegativeInt(11)) { x ->
                FastMathsHelper.exp10Int(x) shouldBe 10.0.pow(x).toInt()
            }
        }
        test("exp10Long") {
            checkAll(Arb.nonNegativeInt(20)) { x ->
                FastMathsHelper.exp10Long(x) shouldBe 10.0.pow(x).toLong()
            }
        }
        test("Integer floorLog2") {
            checkAll(Arb.positiveInt()) { x ->
                FastMathsHelper.floorLog2(x) shouldBe floor(log2(x.toDouble())).toInt()
            }
        }
        test("Long floorLog2") {
            checkAll(Arb.positiveLong(9000000000000000000L /* Math.log is too inaccurate at Long.MAX_VALUE */)) { x ->
                FastMathsHelper.floorLog2(x) shouldBe floor(log2(x.toDouble())).toInt()
            }
        }
        test("Integer ceilLog2") {
            checkAll(Arb.positiveInt()) { x ->
                FastMathsHelper.ceilLog2(x) shouldBe ceil(log2(x.toDouble())).toInt()
            }
        }
        test("Long ceilLog2") {
            checkAll(Arb.positiveLong()) { x ->
                FastMathsHelper.ceilLog2(x) shouldBe ceil(log2(x.toDouble())).toInt()
            }
        }
        test("Integer floorLog10") {
            checkAll(Arb.positiveInt()) { x ->
                FastMathsHelper.floorLog10(x) shouldBe floor(log10(x.toDouble())).toInt()
            }
        }
        test("Long floorLog10") {
            checkAll(Arb.positiveLong()) { x ->
                FastMathsHelper.floorLog10(x) shouldBe floor(log10(x.toDouble())).toInt()
            }
        }
        test("Integer ceilLog10") {
            checkAll(Arb.positiveInt()) { x ->
                FastMathsHelper.ceilLog10(x) shouldBe ceil(log10(x.toDouble())).toInt()
            }
        }
        test("Long ceilLog10") {
            checkAll(Arb.positiveLong()) { x ->
                FastMathsHelper.ceilLog10(x) shouldBe ceil(log10(x.toDouble())).toInt()
            }
        }
    }
})
