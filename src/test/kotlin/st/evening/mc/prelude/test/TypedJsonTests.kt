package st.evening.mc.prelude.test

import io.kotest.assertions.print.Printed
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.MatcherResultBuilder
import io.kotest.matchers.should
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.toTJsonArray

private fun deepEqual(expected: TJson): Matcher<TJson> = object : Matcher<TJson> {
    override fun test(value: TJson): MatcherResult = MatcherResultBuilder(
        value deepEquals expected,
        { Printed(expected.toPrettyString(), expected::class) },
        { Printed(value.toPrettyString(), value::class) },
        { "TJSON should be equal!" },
        { "TJSON should not be equal!" }
    ).build()
}

private fun Arb.Companion.tjson(depth: Int): Arb<TJson> = Arb.of(TJson.Type.entries).flatMap {
    when (it) {
        TJson.Type.NULL -> Arb.constant(TJson.Null)
        TJson.Type.OBJECT -> tjsonObject(depth)
        TJson.Type.ARRAY -> tjsonArray(depth)
        TJson.Type.STRING -> Arb.string(maxSize = 32).map(TJson::String)
        TJson.Type.BOOL -> Arb.boolean().map(TJson::Bool)
        TJson.Type.BYTE -> Arb.byte().map(TJson::Byte)
        TJson.Type.SHORT -> Arb.short().map(TJson::Short)
        TJson.Type.INT -> Arb.int().map(TJson::Int)
        TJson.Type.LONG -> Arb.long().map(TJson::Long)
        TJson.Type.FLOAT -> Arb.float(includeNaNs = false).map(TJson::Float)
        TJson.Type.DOUBLE -> Arb.double(includeNaNs = false).map(TJson::Double)
    }
}

private fun Arb.Companion.tjsonObject(depth: Int): Arb<TJson.Object> =
    list(pair(string(maxSize = 16), tjson(depth - 1)), 0..5).map { entries ->
        val obj = TJson.Object()
        entries.forEach { (key, value) -> obj[key] = value }
        return@map obj
    }

private fun Arb.Companion.tjsonArray(depth: Int): Arb<TJson.Array> =
    list(tjson(depth - 1), 0..5).map { it.toTJsonArray() }

class TypedJsonTests : FunSpec({
    context("Parsing tests") {
        test("Print then parse") {
            checkAll(Arb.tjson(3)) { tjson ->
                TypedJsonParser.parse(tjson.toString()) should deepEqual(tjson)
            }
        }
        test("Pretty-print then parse") {
            checkAll(Arb.tjson(3)) { tjson ->
                TypedJsonParser.parse(tjson.toPrettyString()) should deepEqual(tjson)
            }
        }
    }
})
