@file:OptIn(ExperimentalContracts::class, PreludeInternal::class)

package st.evening.mc.prelude.api.data.tjson

import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.SerializationException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PreludeInternal
context(_: JsonPath)
fun <J : TJson> TJson.expectIs(type: TJson.Type): J { // unsafe: J need not match up with type!
    if (this.type != type) throw SerializationException.withPath("Expected ${type.show}!")
    @Suppress("UNCHECKED_CAST")
    return this as J
}

context(_: JsonPath)
fun TJson.expectIsObject(): TJson.Object = expectIs(TJson.Type.OBJECT)

context(_: JsonPath)
fun TJson.expectIsArray(): TJson.Array = expectIs(TJson.Type.ARRAY)

context(_: JsonPath)
fun TJson.expectIsString(): String = expectIs<TJson.String>(TJson.Type.STRING).value

context(_: JsonPath)
fun TJson.expectIsBool(): Boolean = expectIs<TJson.Bool>(TJson.Type.BOOL).value

context(_: JsonPath)
fun TJson.expectIsByte(): Byte = expectIs<TJson.Byte>(TJson.Type.BYTE).value

context(_: JsonPath)
fun TJson.expectIsShort(): Short = expectIs<TJson.Short>(TJson.Type.SHORT).value

context(_: JsonPath)
fun TJson.expectIsInt(): Int = expectIs<TJson.Int>(TJson.Type.INT).value

context(_: JsonPath)
fun TJson.expectIsLong(): Long = expectIs<TJson.Long>(TJson.Type.LONG).value

context(_: JsonPath)
fun TJson.expectIsFloat(): Float = expectIs<TJson.Float>(TJson.Type.FLOAT).value

context(_: JsonPath)
fun TJson.expectIsDouble(): Double = expectIs<TJson.Double>(TJson.Type.DOUBLE).value

// OBJECT, OPTIONAL ====================================================================================================

@PreludeInternal
context(path: JsonPath)
inline fun <J : TJson, T> TJson.Object.expectUse(
    key: String,
    type: TJson.Type, // unsafe: J need not match up with type!
    action: context(JsonPath) (J) -> T
): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    val value = this[key] ?: return null
    if (value == TJson.Null) return null
    return context(path / key) {
        if (value.type != type) throw SerializationException.withPath("Should be ${type.show}!")
        @Suppress("UNCHECKED_CAST")
        action(value as J)
    }
}

context(path: JsonPath)
inline fun <T> TJson.Object.useAny(key: String, action: context(JsonPath) (TJson) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return context(path / key) {
        action(this[key] ?: return null)
    }
}

// expectAny would be exactly the same as indexing directly, so it's redundant

context(_: JsonPath)
inline fun <T> TJson.Object.useObject(key: String, action: context(JsonPath) (TJson.Object) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse(key, TJson.Type.OBJECT, action)
}

context(_: JsonPath)
fun TJson.Object.expectObject(key: String): TJson.Object? = useObject(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useArray(key: String, action: context(JsonPath) (TJson.Array) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse(key, TJson.Type.ARRAY, action)
}

context(_: JsonPath)
fun TJson.Object.expectArray(key: String): TJson.Array? = useArray(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useString(key: String, action: context(JsonPath) (String) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.String, T>(key, TJson.Type.STRING) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectString(key: String): String? = useString(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useBool(key: String, action: context(JsonPath) (Boolean) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Bool, T>(key, TJson.Type.BOOL) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectBool(key: String): Boolean? = useBool(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useByte(key: String, action: context(JsonPath) (Byte) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Byte, T>(key, TJson.Type.BYTE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectByte(key: String): Byte? = useByte(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useShort(key: String, action: context(JsonPath) (Short) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Short, T>(key, TJson.Type.SHORT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectShort(key: String): Short? = useShort(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useInt(key: String, action: context(JsonPath) (Int) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Int, T>(key, TJson.Type.INT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectInt(key: String): Int? = useInt(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useLong(key: String, action: context(JsonPath) (Long) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Long, T>(key, TJson.Type.LONG) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectLong(key: String): Long? = useLong(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useFloat(key: String, action: context(JsonPath) (Float) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Float, T>(key, TJson.Type.FLOAT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectFloat(key: String): Float? = useFloat(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useDouble(key: String, action: context(JsonPath) (Double) -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Double, T>(key, TJson.Type.DOUBLE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectDouble(key: String): Double? = useDouble(key) { it }

// OBJECT, REQUIRED ====================================================================================================

@PreludeInternal
context(path: JsonPath)
inline fun <J : TJson, T> TJson.Object.expectUseValue(
    key: String,
    type: TJson.Type, // unsafe: J need not match up with type!
    action: context(JsonPath) (J) -> T
): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val value = this[key]
    return context(path / key) {
        if (value == null || value.type != type) throw SerializationException.withPath("Expected ${type.show}!")
        @Suppress("UNCHECKED_CAST")
        action(value as J)
    }
}

context(path: JsonPath)
inline fun <T> TJson.Object.useAnyValue(key: String, action: context(JsonPath) (TJson) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return context(path / key) {
        action(this[key] ?: throw SerializationException.withPath("Expected a value!"))
    }
}

context(_: JsonPath)
fun TJson.Object.expectAnyValue(key: String): TJson = useAnyValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useObjectValue(key: String, action: context(JsonPath) (TJson.Object) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue(key, TJson.Type.OBJECT, action)
}

context(_: JsonPath)
fun TJson.Object.expectObjectValue(key: String): TJson.Object = useObjectValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useArrayValue(key: String, action: context(JsonPath) (TJson.Array) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue(key, TJson.Type.ARRAY, action)
}

context(_: JsonPath)
fun TJson.Object.expectArrayValue(key: String): TJson.Array = useArrayValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useStringValue(key: String, action: context(JsonPath) (String) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.String, T>(key, TJson.Type.STRING) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectStringValue(key: String): String = useStringValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useBoolValue(key: String, action: context(JsonPath) (Boolean) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Bool, T>(key, TJson.Type.BOOL) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectBoolValue(key: String): Boolean = useBoolValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useByteValue(key: String, action: context(JsonPath) (Byte) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Byte, T>(key, TJson.Type.BYTE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectByteValue(key: String): Byte = useByteValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useShortValue(key: String, action: context(JsonPath) (Short) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Short, T>(key, TJson.Type.SHORT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectShortValue(key: String): Short = useShortValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useIntValue(key: String, action: context(JsonPath) (Int) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Int, T>(key, TJson.Type.INT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectIntValue(key: String): Int = useIntValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useLongValue(key: String, action: context(JsonPath) (Long) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Long, T>(key, TJson.Type.LONG) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectLongValue(key: String): Long = useLongValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useFloatValue(key: String, action: context(JsonPath) (Float) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Float, T>(key, TJson.Type.FLOAT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectFloatValue(key: String): Float = useFloatValue(key) { it }

context(_: JsonPath)
inline fun <T> TJson.Object.useDoubleValue(key: String, action: context(JsonPath) (Double) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return expectUseValue<TJson.Double, T>(key, TJson.Type.DOUBLE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Object.expectDoubleValue(key: String): Double = useDoubleValue(key) { it }

// OBJECT, ITERATION ===================================================================================================

context(path: JsonPath)
inline fun TJson.Object.forEachValue(action: context(JsonPath) (String, TJson) -> Unit) {
    entries.forEach { (key, value) ->
        context(path / key) {
            action(key, value)
        }
    }
}

context(path: JsonPath)
inline fun TJson.Object.forEachObject(action: context(JsonPath) (String, TJson.Object) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsObject()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachArray(action: context(JsonPath) (String, TJson.Array) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsArray()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachString(action: context(JsonPath) (String, String) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsString()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachBool(action: context(JsonPath) (String, Boolean) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsBool()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachByte(action: context(JsonPath) (String, Byte) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsByte()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachShort(action: context(JsonPath) (String, Short) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsShort()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachInt(action: context(JsonPath) (String, Int) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsInt()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachLong(action: context(JsonPath) (String, Long) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsLong()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachFloat(action: context(JsonPath) (String, Float) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsFloat()) }
}

context(path: JsonPath)
inline fun TJson.Object.forEachDouble(action: context(JsonPath) (String, Double) -> Unit) {
    forEachValue { key, value -> action(key, value.expectIsDouble()) }
}

// ARRAY, REQUIRED =====================================================================================================

@PreludeInternal
context(path: JsonPath)
inline fun <J : TJson, T> TJson.Array.expectUse(
    index: Int,
    type: TJson.Type, // unsafe: J need not match up with type!
    action: context(JsonPath) (J) -> T
): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val value = this[index]
    return context(path / index) {
        if (value.type != type) throw SerializationException.withPath("Expected ${type.show}!")
        @Suppress("UNCHECKED_CAST")
        action(value as J)
    }
}

context(path: JsonPath)
inline fun <T> TJson.Array.useAny(index: Int, action: context(JsonPath) (TJson) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return context(path / index) {
        action(this[index])
    }
}

// expectAny would be exactly the same as indexing directly, so it's redundant

context(_: JsonPath)
inline fun <T> TJson.Array.useObject(index: Int, action: context(JsonPath) (TJson.Object) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse(index, TJson.Type.OBJECT, action)
}

context(_: JsonPath)
fun TJson.Array.expectObject(index: Int): TJson.Object = useObject(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useArray(index: Int, action: context(JsonPath) (TJson.Array) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse(index, TJson.Type.ARRAY, action)
}

context(_: JsonPath)
fun TJson.Array.expectArray(index: Int): TJson.Array = useArray(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useString(index: Int, action: context(JsonPath) (String) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.String, T>(index, TJson.Type.STRING) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectString(index: Int): String = useString(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useBool(index: Int, action: context(JsonPath) (Boolean) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Bool, T>(index, TJson.Type.BOOL) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectBool(index: Int): Boolean = useBool(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useByte(index: Int, action: context(JsonPath) (Byte) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Byte, T>(index, TJson.Type.BYTE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectByte(index: Int): Byte = useByte(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useShort(index: Int, action: context(JsonPath) (Short) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Short, T>(index, TJson.Type.SHORT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectShort(index: Int): Short = useShort(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useInt(index: Int, action: context(JsonPath) (Int) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Int, T>(index, TJson.Type.INT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectInt(index: Int): Int = useInt(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useLong(index: Int, action: context(JsonPath) (Long) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Long, T>(index, TJson.Type.LONG) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectLong(index: Int): Long = useLong(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useFloat(index: Int, action: context(JsonPath) (Float) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Float, T>(index, TJson.Type.FLOAT) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectFloat(index: Int): Float = useFloat(index) { it }

context(_: JsonPath)
inline fun <T> TJson.Array.useDouble(index: Int, action: context(JsonPath) (Double) -> T): T {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return expectUse<TJson.Double, T>(index, TJson.Type.DOUBLE) { action(it.value) }
}

context(_: JsonPath)
fun TJson.Array.expectDouble(index: Int): Double = useDouble(index) { it }

// ARRAY, ITERATION ====================================================================================================

context(path: JsonPath)
inline fun TJson.Array.forEachValueIndexed(action: context(JsonPath) (Int, TJson) -> Unit) {
    children.forEachIndexed { i, value ->
        context(path / i.toString()) {
            action(i, value)
        }
    }
}

context(path: JsonPath)
inline fun TJson.Array.forEachValue(action: context(JsonPath) (TJson) -> Unit) {
    forEachValueIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachObjectIndexed(action: context(JsonPath) (Int, TJson.Object) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsObject()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachObject(action: context(JsonPath) (TJson.Object) -> Unit) {
    forEachObjectIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachArrayIndexed(action: context(JsonPath) (Int, TJson.Array) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsArray()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachArray(action: context(JsonPath) (TJson.Array) -> Unit) {
    forEachArrayIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachStringIndexed(action: context(JsonPath) (Int, String) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsString()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachString(action: context(JsonPath) (String) -> Unit) {
    forEachStringIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachBoolIndexed(action: context(JsonPath) (Int, Boolean) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsBool()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachBool(action: context(JsonPath) (Boolean) -> Unit) {
    forEachBoolIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachByteIndexed(action: context(JsonPath) (Int, Byte) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsByte()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachByte(action: context(JsonPath) (Byte) -> Unit) {
    forEachByteIndexed { _, value -> action(value) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachShortIndexed(action: context(JsonPath) (Int, Short) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsShort()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachShort(action: context(JsonPath) (Short) -> Unit) {
    forEachShortIndexed { _, value -> action(value) }
}


context(path: JsonPath)
inline fun TJson.Array.forEachIntIndexed(action: context(JsonPath) (Int, Int) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsInt()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachInt(action: context(JsonPath) (Int) -> Unit) {
    forEachIntIndexed { _, value -> action(value) }
}


context(path: JsonPath)
inline fun TJson.Array.forEachLongIndexed(action: context(JsonPath) (Int, Long) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsLong()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachLong(action: context(JsonPath) (Long) -> Unit) {
    forEachLongIndexed { _, value -> action(value) }
}


context(path: JsonPath)
inline fun TJson.Array.forEachFloatIndexed(action: context(JsonPath) (Int, Float) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsFloat()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachFloat(action: context(JsonPath) (Float) -> Unit) {
    forEachFloatIndexed { _, value -> action(value) }
}


context(path: JsonPath)
inline fun TJson.Array.forEachDoubleIndexed(action: context(JsonPath) (Int, Double) -> Unit) {
    forEachValueIndexed { i, value -> action(i, value.expectIsDouble()) }
}

context(path: JsonPath)
inline fun TJson.Array.forEachDouble(action: context(JsonPath) (Double) -> Unit) {
    forEachDoubleIndexed { _, value -> action(value) }
}
