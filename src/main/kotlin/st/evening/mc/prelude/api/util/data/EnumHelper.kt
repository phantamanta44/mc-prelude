package st.evening.mc.prelude.api.util.data

import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.util.collection.asUnmodifiableList
import st.evening.mc.prelude.api.util.collection.getCyclic
import st.evening.mc.prelude.api.util.system.sharedEnumConstantsUnsafe

@OptIn(PreludeInternal::class)
val <E : Enum<E>> Class<E>.enumConstantCount: Int
    get() = sharedEnumConstantsUnsafe.size

val <E : Enum<E>> Class<E>.enumConstantOrdinals: IntRange
    get() = 0..<enumConstantCount

@OptIn(PreludeInternal::class)
fun <E : Enum<E>> Class<E>.getEnumConstant(ordinal: Int): E = sharedEnumConstantsUnsafe[ordinal]

fun <E : Enum<E>> Class<E>.getEnumConstant(name: String): E = java.lang.Enum.valueOf(this, name)

@OptIn(PreludeInternal::class)
fun <E : Enum<E>> E.cycleEnum(offset: Int): E = declaringJavaClass.sharedEnumConstantsUnsafe.getCyclic(ordinal + offset)

@OptIn(PreludeInternal::class)
fun <E : Enum<E>> Class<E>.getEnumConstantsAsList(): List<E> = sharedEnumConstantsUnsafe.asList().asUnmodifiableList()

@OptIn(PreludeInternal::class)
inline fun <E : Enum<E>, T> Class<E>.mapEnumConstants(f: (E) -> T): List<T> = sharedEnumConstantsUnsafe.map(f)

@OptIn(PreludeInternal::class)
inline fun <E : Enum<E>> Class<E>.forEachEnumConstant(f: (E) -> Unit) {
    sharedEnumConstantsUnsafe.forEach(f)
}

@OptIn(PreludeInternal::class)
inline fun <E : Enum<E>> Class<E>.forEachEnumConstantIndexed(f: (Int, E) -> Unit) {
    sharedEnumConstantsUnsafe.forEachIndexed(f)
}
