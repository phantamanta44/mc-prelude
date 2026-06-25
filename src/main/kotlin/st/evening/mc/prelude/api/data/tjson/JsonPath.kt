package st.evening.mc.prelude.api.data.tjson

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface JsonPath {
    operator fun div(key: String): JsonPath = Child(this, key)

    operator fun div(index: Int): JsonPath = Child(this, index.toString())

    object Root : JsonPath {
        override fun toString(): String = "<root>"
    }

    data class Child(val parent: JsonPath, val key: String) : JsonPath {
        override fun toString(): String = if (parent == Root) key else "$parent.$key"
    }

    companion object {
        @OptIn(ExperimentalContracts::class)
        inline fun <T> atRoot(action: context(JsonPath) () -> T): T {
            contract {
                callsInPlace(action, InvocationKind.EXACTLY_ONCE)
            }
            return context(Root) {
                action()
            }
        }
    }
}
