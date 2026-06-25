package st.evening.mc.prelude.test

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.contain
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.maps.containExactly
import io.kotest.matchers.maps.haveKey
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import st.evening.mc.prelude.api.util.collection.IntRangeSet
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import kotlin.random.Random
import io.kotest.matchers.maps.beEmpty as beEmptyMap
import io.kotest.matchers.maps.haveSize as haveMapSize

private const val INSERT_COUNT: Int = 100

private class Thing : WeaklyValid {
    override var weakValidity: WeakValidity = WeakValidity.VALID
}

class WeakValidityTests : FunSpec({
    context("Weak validity map tests") {
        test("Insert") {
            val map = WeakValidityMap<Thing, Int>()
            val things = mutableSetOf<Thing>()
            map should beEmptyMap()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                map[x] = i
                things += x
                map should haveKey(x)
                map should haveMapSize(i)
                map.values should containExactlyInAnyOrder(IntRangeSet(1..i))
            }
            map.keys should containExactlyInAnyOrder(things)
        }
        test("Insert, remove") {
            val rand = Random(4)
            val map = WeakValidityMap<Thing, Int>()
            val expected = mutableMapOf<Thing, Int>()
            val things = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                map[x] = i
                expected[x] = i
                things += x
                map should haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = things.removeAt(rand.nextInt(things.size))
                    map.remove(y)
                    expected.remove(y)
                    map shouldNot haveKey(y)
                    map should haveMapSize(expected.size)
                    map should containExactly(expected)
                }
            }
            things.shuffle(rand)
            things.forEach { x ->
                map.remove(x)
                expected.remove(x)
                map shouldNot haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
            }
            map should beEmptyMap()
        }
        test("Insert, invalidate") {
            val rand = Random(115942019)
            val map = WeakValidityMap<Thing, Int>()
            val expected = mutableMapOf<Thing, Int>()
            val things = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                map[x] = i
                expected[x] = i
                things += x
                map should haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = things.removeAt(rand.nextInt(things.size))
                    y.weakValidity = WeakValidity.INVALID
                    expected.remove(y)
                    map shouldNot haveKey(y)
                    map should haveMapSize(expected.size)
                    map should containExactly(expected)
                }
            }
            things.shuffle(rand)
            things.forEach { x ->
                x.weakValidity = WeakValidity.INVALID
                expected.remove(x)
                map shouldNot haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
            }
            map should beEmptyMap()
        }
        test("Insert, remove, invalidate") {
            val rand = Random(935893226)
            val map = WeakValidityMap<Thing, Int>()
            val expected = mutableMapOf<Thing, Int>()
            val things = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                map[x] = i
                expected[x] = i
                things += x
                map should haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = things.removeAt(rand.nextInt(things.size))
                    if (rand.nextBoolean()) {
                        y.weakValidity = WeakValidity.INVALID
                    } else {
                        map.remove(y)
                    }
                    expected.remove(y)
                    map shouldNot haveKey(y)
                    map should haveMapSize(expected.size)
                    map should containExactly(expected)
                }
            }
            things.shuffle(rand)
            things.forEach { x ->
                if (rand.nextBoolean()) {
                    x.weakValidity = WeakValidity.INVALID
                } else {
                    map.remove(x)
                }
                expected.remove(x)
                map shouldNot haveKey(x)
                map should haveMapSize(expected.size)
                map should containExactly(expected)
            }
            map should beEmptyMap()
        }
    }
    context("Weak validity set tests") {
        test("Insert") {
            val set = WeakValidityMap.newSet<Thing>()
            val expected = mutableSetOf<Thing>()
            set should beEmpty()
            repeat(INSERT_COUNT) {
                val x = Thing()
                set += x
                expected += x
                set should contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
            }
        }
        test("Insert, remove") {
            val rand = Random(226457503)
            val set = WeakValidityMap.newSet<Thing>()
            val expected = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                set += x
                expected += x
                set should contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = expected.removeAt(rand.nextInt(expected.size))
                    set -= y
                    set shouldNot contain(y)
                    set should haveSize(expected.size)
                    set should containExactlyInAnyOrder(expected)
                }
            }
            expected.shuffle(rand)
            while (expected.isNotEmpty()) {
                val x = expected.removeLast()
                set -= x
                set shouldNot contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
            }
            set should beEmpty()
        }
        test("Insert, invalidate") {
            val rand = Random(2115299971)
            val set = WeakValidityMap.newSet<Thing>()
            val expected = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                set += x
                expected += x
                set should contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = expected.removeAt(rand.nextInt(expected.size))
                    y.weakValidity = WeakValidity.INVALID
                    set shouldNot contain(y)
                    set should haveSize(expected.size)
                    set should containExactlyInAnyOrder(expected)
                }
            }
            expected.shuffle(rand)
            while (expected.isNotEmpty()) {
                val x = expected.removeLast()
                x.weakValidity = WeakValidity.INVALID
                set shouldNot contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
            }
            set should beEmpty()
        }
        test("Insert, remove, invalidate") {
            val rand = Random(409732684)
            val set = WeakValidityMap.newSet<Thing>()
            val expected = mutableListOf<Thing>()
            for (i in 1..INSERT_COUNT) {
                val x = Thing()
                set += x
                expected += x
                set should contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
                if (i > 2 && rand.nextInt(5) == 0) {
                    val y = expected.removeAt(rand.nextInt(expected.size))
                    if (rand.nextBoolean()) {
                        y.weakValidity = WeakValidity.INVALID
                    } else {
                        set -= y
                    }
                    set shouldNot contain(y)
                    set should haveSize(expected.size)
                    set should containExactlyInAnyOrder(expected)
                }
            }
            expected.shuffle(rand)
            while (expected.isNotEmpty()) {
                val x = expected.removeLast()
                if (rand.nextBoolean()) {
                    x.weakValidity = WeakValidity.INVALID
                } else {
                    set -= x
                }
                set shouldNot contain(x)
                set should haveSize(expected.size)
                set should containExactlyInAnyOrder(expected)
            }
            set should beEmpty()
        }
    }
})
