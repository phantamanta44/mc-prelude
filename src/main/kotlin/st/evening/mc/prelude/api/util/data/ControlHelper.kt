package st.evening.mc.prelude.api.util.data

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> orNull(condition: Boolean, f: () -> T): T? {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
        returnsNotNull() implies condition
    }
    return if (condition) f() else null
}

@OptIn(ExperimentalContracts::class)
inline fun <T> void(f: () -> T) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    f()
}

@OptIn(ExperimentalContracts::class)
inline fun <T> T.letIf(condition: Boolean, f: (T) -> T): T {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) f(this) else this
}
