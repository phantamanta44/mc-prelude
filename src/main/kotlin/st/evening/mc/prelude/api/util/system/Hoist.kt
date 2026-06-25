package st.evening.mc.prelude.api.util.system

import st.evening.mc.prelude.api.util.data.ThreadLocalProperty

// helps with situations where constructor parameters must be available to methods in the class before they're bound to
// fields, e.g. when they're called by superconstructors like in the case of Block#createBlockState
object Hoist {
    private val stack: ArrayDeque<Any?> by ThreadLocalProperty { ArrayDeque() }

    fun push(value: Any?) {
        stack.addLast(value)
    }

    fun popRaw(): Any? = stack.removeLast()

    inline fun <reified T> pop(): T = popRaw() as T
}
