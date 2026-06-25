package st.evening.mc.prelude.api.data.tjson

import st.evening.mc.prelude.api.util.data.NbtDsl

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TJsonDsl {
    companion object {
        inline fun obj(action: TJsonObjectAction): TJson.Object = TJson.Object().runAction(action)
    }
}

@JvmInline
@NbtDsl
value class TJsonObjectDsl(private val obj: TJson.Object) {
    infix fun String.string(value: String) {
        obj[this] = value
    }
    
    infix fun String.bool(value: Boolean) {
        obj[this] = value
    }
    
    infix fun String.byte(value: Byte) {
        obj[this] = value
    }
    
    infix fun String.short(value: Short) {
        obj[this] = value
    }
    
    infix fun String.int(value: Int) {
        obj[this] = value
    }
    
    infix fun String.long(value: Long) {
        obj[this] = value
    }
    
    infix fun String.float(value: Float) {
        obj[this] = value
    }
    
    infix fun String.double(value: Double) {
        obj[this] = value
    }
    
    infix fun String.elem(elem: TJson) {
        obj[this] = elem
    }

    inline infix fun String.obj(action: TJsonObjectAction) {
        this elem TJsonDsl.obj(action)
    }
}

typealias TJsonObjectAction = TJsonObjectDsl.() -> Unit

inline fun TJson.Object.runAction(action: TJsonObjectAction): TJson.Object = also { TJsonObjectDsl(it).action() }

fun Iterable<TJson>.toTJsonArray(): TJson.Array = TJson.Array.of(this)
fun Array<out TJson>.toTJsonArray(): TJson.Array = TJson.Array.of(*this)

inline fun <T> Iterable<T>.mapToTJsonArray(f: (T) -> TJson): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += f(it) }
}

inline fun <T> Array<out T>.mapToTJsonArray(f: (T) -> TJson): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += f(it) }
}

fun Iterable<String>.toTJsonStringArray(): TJson.Array = mapToTJsonArray { TJson.String(it) }
fun Array<out String>.toTJsonStringArray(): TJson.Array = mapToTJsonArray { TJson.String(it) }

fun Iterable<Boolean>.toTJsonBoolArray(): TJson.Array = mapToTJsonArray { TJson.Bool(it) }
fun BooleanArray.toTJsonArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Bool(it) }
}

fun Iterable<Byte>.toTJsonByteArray(): TJson.Array = mapToTJsonArray { TJson.Byte(it) }
fun ByteArray.toTJsonArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Byte(it) }
}

fun Iterable<Short>.toTJsonShortArray(): TJson.Array = mapToTJsonArray { TJson.Short(it) }
fun ShortArray.toTJsonShortArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Short(it) }
}

fun Iterable<Int>.toTJsonIntArray(): TJson.Array = mapToTJsonArray { TJson.Int(it) }
fun IntArray.toTJsonIntArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Int(it) }
}

fun Iterable<Long>.toTJsonLongArray(): TJson.Array = mapToTJsonArray { TJson.Long(it) }
fun LongArray.toTJsonLongArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Long(it) }
}

fun Iterable<Float>.toTJsonFloatArray(): TJson.Array = mapToTJsonArray { TJson.Float(it) }
fun FloatArray.toTJsonFloatArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Float(it) }
}

fun Iterable<Double>.toTJsonDoubleArray(): TJson.Array = mapToTJsonArray { TJson.Double(it) }
fun DoubleArray.toTJsonDoubleArray(): TJson.Array = TJson.Array().also { arr ->
    forEach { arr += TJson.Double(it) }
}
