package st.evening.mc.prelude.api.data.tjson

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import java.io.StringWriter
import java.io.Writer

sealed interface TJson {
    companion object {
        fun escapeJsonString(value: kotlin.String): kotlin.String =
            StringWriter().also { JsonWriter(it).value(value) }.toString()
    }

    val type: Type

    infix fun deepEquals(o: TJson): Boolean

    fun copy(): TJson

    fun toGson(): JsonElement

    fun writePrettyString(out: Writer, indent: kotlin.String = "")

    fun toPrettyString(): kotlin.String = StringWriter().also { writePrettyString(it) }.toString()

    enum class Type(val show: kotlin.String) { // mostly just a way to get a human-readable name for a type
        NULL("null"),
        OBJECT("object"),
        ARRAY("array"),
        STRING("string"),
        BOOL("boolean"),
        BYTE("byte"),
        SHORT("short"),
        INT("integer"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double")
    }

    sealed interface TJsonLiteral<T> : TJson {
        val value: T

        override fun writePrettyString(out: Writer, indent: kotlin.String) {
            out.write(toString())
        }

        override fun toPrettyString(): kotlin.String = toString()
    }

    object Null : TJsonLiteral<Any?> {
        override val type: Type
            get() = Type.NULL

        override val value: Any?
            get() = null

        override fun deepEquals(o: TJson): Boolean = o === this

        override fun copy(): TJson = this

        override fun toGson(): JsonNull = JsonNull.INSTANCE

        override fun toString(): kotlin.String = "null"
    }

    class Object private constructor(private val _entries: MutableMap<kotlin.String, TJson>) : TJson {
        companion object {
            fun of(entries: Map<kotlin.String, TJson>): Object = Object(entries.toMutableMap())
            fun of(vararg entries: Pair<kotlin.String, TJson>): Object = Object(entries.toMap(mutableMapOf()))
        }

        constructor() : this(mutableMapOf())

        override val type: Type
            get() = Type.OBJECT

        val entries: Map<kotlin.String, TJson>
            get() = _entries

        val size: kotlin.Int
            get() = _entries.size

        fun isEmpty(): Boolean = _entries.isEmpty()

        operator fun set(key: kotlin.String, value: TJson) {
            _entries[key] = value
        }

        operator fun set(key: kotlin.String, value: kotlin.String) {
            _entries[key] = String(value)
        }

        operator fun set(key: kotlin.String, value: Boolean) {
            _entries[key] = Bool(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Byte) {
            _entries[key] = Byte(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Short) {
            _entries[key] = Short(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Int) {
            _entries[key] = Int(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Long) {
            _entries[key] = Long(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Float) {
            _entries[key] = Float(value)
        }

        operator fun set(key: kotlin.String, value: kotlin.Double) {
            _entries[key] = Double(value)
        }

        operator fun get(key: kotlin.String): TJson? = _entries[key]

        fun getObject(key: kotlin.String): Object? = this[key] as? Object
        fun getArray(key: kotlin.String): Array? = this[key] as? Array
        fun getString(key: kotlin.String): kotlin.String? = (this[key] as? String)?.value
        fun getBool(key: kotlin.String): Boolean? = (this[key] as? Bool)?.value
        fun getByte(key: kotlin.String): kotlin.Byte? = (this[key] as? Byte)?.value
        fun getShort(key: kotlin.String): kotlin.Short? = (this[key] as? Short)?.value
        fun getInt(key: kotlin.String): kotlin.Int? = (this[key] as? Int)?.value
        fun getLong(key: kotlin.String): kotlin.Long? = (this[key] as? Long)?.value
        fun getFloat(key: kotlin.String): kotlin.Float? = (this[key] as? Float)?.value
        fun getDouble(key: kotlin.String): kotlin.Double? = (this[key] as? Double)?.value

        fun getValue(key: kotlin.String): TJson = _entries.getValue(key)

        fun getObjectValue(key: kotlin.String): Object = getValue(key) as Object
        fun getArrayValue(key: kotlin.String): Array = getValue(key) as Array
        fun getStringValue(key: kotlin.String): kotlin.String = (getValue(key) as String).value
        fun getBoolValue(key: kotlin.String): Boolean = (getValue(key) as Bool).value
        fun getByteValue(key: kotlin.String): kotlin.Byte = (getValue(key) as Byte).value
        fun getShortValue(key: kotlin.String): kotlin.Short = (getValue(key) as Short).value
        fun getIntValue(key: kotlin.String): kotlin.Int = (getValue(key) as Int).value
        fun getLongValue(key: kotlin.String): kotlin.Long = (getValue(key) as Long).value
        fun getFloatValue(key: kotlin.String): kotlin.Float = (getValue(key) as Float).value
        fun getDoubleValue(key: kotlin.String): kotlin.Double = (getValue(key) as Double).value

        fun getOrCreateObject(key: kotlin.String): Object = getObject(key) ?: Object().also { this[key] = it }
        fun getOrCreateArray(key: kotlin.String): Array = getArray(key) ?: Array().also { this[key] = it }

        operator fun contains(key: kotlin.String): Boolean = key in _entries

        operator fun minusAssign(key: kotlin.String) {
            _entries -= key
        }

        override fun deepEquals(o: TJson): Boolean {
            if (o !is Object || _entries.size != o._entries.size) return false
            _entries.forEach { (key, value) ->
                if (!((o._entries[key] ?: return false) deepEquals value)) return false
            }
            return true
        }

        override fun copy(): Object = Object(_entries.mapValuesTo(mutableMapOf()) { it.value.copy() })

        override fun toGson(): JsonObject = JsonObject().also { obj ->
            _entries.forEach { (key, value) -> obj.add(key, value.toGson()) }
        }

        override fun toString(): kotlin.String =
            _entries.entries.joinToString(", ", "{", "}") { (key, value) -> "${escapeJsonString(key)}: $value" }

        override fun writePrettyString(out: Writer, indent: kotlin.String) {
            if (_entries.isEmpty()) {
                out.write("{}")
                return
            }
            out.write("{\n")
            val newIndent = "$indent  "
            val iter = _entries.iterator()
            while (true) {
                val (key, value) = iter.next()
                out.write(newIndent)
                out.write(escapeJsonString(key))
                out.write(": ")
                value.writePrettyString(out, newIndent)
                if (!iter.hasNext()) break
                out.write(",\n")
            }
            out.write("\n")
            out.write(indent)
            out.write("}")
        }
    }

    class Array private constructor(private val _children: MutableList<TJson>) : TJson {
        companion object {
            fun of(elems: Iterable<TJson>): Array = Array(elems.toMutableList())
            fun of(vararg elems: TJson): Array = Array(elems.toMutableList())
        }

        constructor() : this(mutableListOf())

        override val type: Type
            get() = Type.ARRAY

        val children: List<TJson>
            get() = _children

        val size: kotlin.Int
            get() = _children.size

        fun isEmpty(): Boolean = _children.isEmpty()

        operator fun plusAssign(value: TJson) {
            _children += value
        }

        operator fun set(index: kotlin.Int, value: TJson) {
            _children[index] = value
        }

        operator fun set(index: kotlin.Int, value: kotlin.String) {
            _children[index] = String(value)
        }

        operator fun set(index: kotlin.Int, value: Boolean) {
            _children[index] = Bool(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Byte) {
            _children[index] = Byte(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Short) {
            _children[index] = Short(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Int) {
            _children[index] = Int(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Long) {
            _children[index] = Long(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Float) {
            _children[index] = Float(value)
        }

        operator fun set(index: kotlin.Int, value: kotlin.Double) {
            _children[index] = Double(value)
        }

        operator fun get(index: kotlin.Int): TJson = _children[index]

        fun getObject(index: kotlin.Int): Object = _children[index] as Object
        fun getArray(index: kotlin.Int): Array = _children[index] as Array
        fun getString(index: kotlin.Int): kotlin.String = (_children[index] as String).value
        fun getBool(index: kotlin.Int): Boolean = (_children[index] as Bool).value
        fun getByte(index: kotlin.Int): kotlin.Byte = (_children[index] as Byte).value
        fun getShort(index: kotlin.Int): kotlin.Short = (_children[index] as Short).value
        fun getInt(index: kotlin.Int): kotlin.Int = (_children[index] as Int).value
        fun getLong(index: kotlin.Int): kotlin.Long = (_children[index] as Long).value
        fun getFloat(index: kotlin.Int): kotlin.Float = (_children[index] as Float).value
        fun getDouble(index: kotlin.Int): kotlin.Double = (_children[index] as Double).value

        override fun deepEquals(o: TJson): Boolean {
            if (o !is Array || _children.size != o._children.size) return false
            _children.forEachIndexed { index, element ->
                if (!(o._children[index] deepEquals element)) return false
            }
            return true
        }

        override fun copy(): Array = Array(_children.mapTo(mutableListOf()) { it.copy() })

        override fun toGson(): JsonArray = JsonArray().also { arr ->
            _children.forEach { arr.add(it.toGson()) }
        }

        override fun toString(): kotlin.String = _children.joinToString(", ", "[", "]")

        override fun writePrettyString(out: Writer, indent: kotlin.String) {
            if (_children.isEmpty()) {
                out.write("[]")
            } else {
                out.write("[\n")
                val newIndent = "$indent  "
                val iter = _children.iterator()
                while (true) {
                    val value = iter.next()
                    out.write(newIndent)
                    value.writePrettyString(out, newIndent)
                    if (!iter.hasNext()) break
                    out.write(",\n")
                }
                out.write("\n")
                out.write(indent)
                out.write("]")
            }
        }
    }

    data class String(override val value: kotlin.String) : TJsonLiteral<kotlin.String> {
        override val type: Type
            get() = Type.STRING

        override fun deepEquals(o: TJson): Boolean = o is String && o.value == value

        override fun copy(): String = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = escapeJsonString(value)
    }

    data class Bool(override val value: Boolean) : TJsonLiteral<Boolean> {
        override val type: Type
            get() = Type.BOOL

        override fun deepEquals(o: TJson): Boolean = o is Bool && o.value == value

        override fun copy(): Bool = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = value.toString()
    }

    data class Byte(override val value: kotlin.Byte) : TJsonLiteral<kotlin.Byte> {
        override val type: Type
            get() = Type.BYTE

        override fun deepEquals(o: TJson): Boolean = o is Byte && o.value == value

        override fun copy(): Byte = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = "${value}b"
    }

    data class Short(override val value: kotlin.Short) : TJsonLiteral<kotlin.Short> {
        override val type: Type
            get() = Type.SHORT

        override fun deepEquals(o: TJson): Boolean = o is Short && o.value == value

        override fun copy(): Short = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = "${value}s"
    }

    data class Int(override val value: kotlin.Int) : TJsonLiteral<kotlin.Int> {
        override val type: Type
            get() = Type.INT

        override fun deepEquals(o: TJson): Boolean = o is Int && o.value == value

        override fun copy(): Int = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = value.toString()
    }

    data class Long(override val value: kotlin.Long) : TJsonLiteral<kotlin.Long> {
        override val type: Type
            get() = Type.LONG

        override fun deepEquals(o: TJson): Boolean = o is Long && o.value == value

        override fun copy(): Long = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = "${value}l"
    }

    data class Float(override val value: kotlin.Float) : TJsonLiteral<kotlin.Float> {
        override val type: Type
            get() = Type.FLOAT

        override fun deepEquals(o: TJson): Boolean = o is Float && o.value == value

        override fun copy(): Float = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = "${value}f"
    }

    data class Double(override val value: kotlin.Double) : TJsonLiteral<kotlin.Double> {
        override val type: Type
            get() = Type.DOUBLE

        override fun deepEquals(o: TJson): Boolean = o is Double && o.value == value

        override fun copy(): Double = this

        override fun toGson(): JsonPrimitive = JsonPrimitive(value)

        override fun toString(): kotlin.String = "${value}d"
    }
}
