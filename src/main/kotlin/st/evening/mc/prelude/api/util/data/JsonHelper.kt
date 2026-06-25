package st.evening.mc.prelude.api.util.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path

object JsonHelper {
    val parser: JsonParser = JsonParser()

    fun readJson(path: Path): JsonElement = Files.newBufferedReader(path).use { parser.parse(it) }
}

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class JsonDsl {
    companion object {
        inline fun obj(action: JsonObjectAction): JsonObject = JsonObject().runAction(action)

        fun arrayOf(): JsonArray = JsonArray()
        fun arrayOf(elem: JsonElement): JsonArray = JsonArray().apply { add(elem) }
        fun arrayOf(vararg elems: JsonElement): JsonArray = elems.toJsonArray()
        fun arrayOf(value: Boolean): JsonArray = JsonArray().apply { add(value) }
        fun arrayOf(vararg values: Boolean): JsonArray = values.toJsonBoolArray()
        fun arrayOf(value: Char): JsonArray = JsonArray().apply { add(value) }
        fun arrayOf(vararg values: Char): JsonArray = values.toJsonCharArray()
        fun arrayOf(value: Number): JsonArray = JsonArray().apply { add(value) }
        fun arrayOf(vararg values: Number): JsonArray = values.toJsonNumberArray()
        fun arrayOf(value: String): JsonArray = JsonArray().apply { add(value) }
        fun arrayOf(vararg values: String): JsonArray = values.toJsonStringArray()
    }
}

@JvmInline
@NbtDsl
value class JsonObjectDsl(private val obj: JsonObject) {
    infix fun String.bool(value: Boolean) {
        obj.addProperty(this, value)
    }

    infix fun String.char(value: Char) {
        obj.addProperty(this, value)
    }

    infix fun String.number(value: Number) {
        obj.addProperty(this, value)
    }

    infix fun String.string(value: String) {
        obj.addProperty(this, value)
    }

    infix fun String.elem(elem: JsonElement) {
        obj.add(this, elem)
    }

    inline infix fun String.obj(action: JsonObjectAction) {
        this elem JsonDsl.obj(action)
    }
}

typealias JsonObjectAction = JsonObjectDsl.() -> Unit

inline fun JsonObject.runAction(action: JsonObjectAction): JsonObject = also { JsonObjectDsl(it).action() }

operator fun JsonObject.contains(key: String): Boolean = contains(key)

operator fun JsonObject.set(key: String, value: Boolean): Unit = addProperty(key, value)
operator fun JsonObject.set(key: String, value: Char): Unit = addProperty(key, value)
operator fun JsonObject.set(key: String, value: Number): Unit = addProperty(key, value)
operator fun JsonObject.set(key: String, value: String): Unit = addProperty(key, value)
operator fun JsonObject.set(key: String, elem: JsonElement): Unit = add(key, elem)

fun Iterable<JsonElement>.toJsonArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun Array<out JsonElement>.toJsonArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

inline fun <T> Iterable<T>.mapToJsonArray(f: (T) -> JsonElement): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(f(it)) }
}

inline fun <T> Array<out T>.mapToJsonArray(f: (T) -> JsonElement): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(f(it)) }
}

fun Iterable<Boolean>.toJsonBoolArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }
fun BooleanArray.toJsonBoolArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun Iterable<Char>.toJsonCharArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }
fun CharArray.toJsonCharArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun Iterable<Number>.toJsonNumberArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }
fun Array<out Number>.toJsonNumberArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }

fun ByteArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun ShortArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun IntArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun LongArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun FloatArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun DoubleArray.toJsonNumberArray(): JsonArray = JsonArray().also { arr ->
    forEach { arr.add(it) }
}

fun Iterable<String>.toJsonStringArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }
fun Array<out String>.toJsonStringArray(): JsonArray = mapToJsonArray { JsonPrimitive(it) }

operator fun JsonArray.plusAssign(value: Boolean): Unit = add(value)
operator fun JsonArray.plusAssign(value: Char): Unit = add(value)
operator fun JsonArray.plusAssign(value: Number): Unit = add(value)
operator fun JsonArray.plusAssign(value: String): Unit = add(value)
operator fun JsonArray.plusAssign(elem: JsonElement): Unit = add(elem)
