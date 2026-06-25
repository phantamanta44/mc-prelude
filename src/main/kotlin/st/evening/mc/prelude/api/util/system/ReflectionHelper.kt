package st.evening.mc.prelude.api.util.system

import st.evening.mc.prelude.api.PreludeInternal
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method

fun <T : AccessibleObject> T.asAccessible(): T = apply { isAccessible = true }

private val mClass_getEnumConstantsShared: Method? = try {
    Class::class.java.getDeclaredMethod("getEnumConstantsShared").asAccessible() // should be present on openjdk stdlib
} catch (_: NoSuchMethodException) {
    null
}

@PreludeInternal
@Suppress("UNCHECKED_CAST")
val <E : Enum<E>> Class<E>.sharedEnumConstantsUnsafe: Array<E> // fall back to stable public api
    get() = mClass_getEnumConstantsShared?.let { it(this) as Array<E> } ?: enumConstants
