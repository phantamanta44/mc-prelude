package st.evening.mc.prelude.api.data.inbt

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagByteArray
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagEnd
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagLongArray
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraftforge.common.util.Constants
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.ser.GameSerializer
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.forEachValue
import st.evening.mc.prelude.api.util.data.forEach
import st.evening.mc.prelude.api.util.data.get
import st.evening.mc.prelude.api.util.data.isByte
import st.evening.mc.prelude.api.util.data.isByteArray
import st.evening.mc.prelude.api.util.data.isCompound
import st.evening.mc.prelude.api.util.data.isDouble
import st.evening.mc.prelude.api.util.data.isEnd
import st.evening.mc.prelude.api.util.data.isFloat
import st.evening.mc.prelude.api.util.data.isInt
import st.evening.mc.prelude.api.util.data.isIntArray
import st.evening.mc.prelude.api.util.data.isList
import st.evening.mc.prelude.api.util.data.isLong
import st.evening.mc.prelude.api.util.data.isLongArray
import st.evening.mc.prelude.api.util.data.isShort
import st.evening.mc.prelude.api.util.data.isString
import st.evening.mc.prelude.api.util.data.mapToNbtList
import st.evening.mc.prelude.api.util.data.readString
import st.evening.mc.prelude.api.util.data.set

sealed interface ImmutableNbt {
    val typeId: kotlin.Int

    fun matches(tag: NBTBase): Boolean

    fun toNbt(): NBTBase

    data class Compound(val entries: ImmutableMap<kotlin.String, ImmutableNbt>) : ImmutableNbt {
        companion object {
            val EMPTY: Compound = Compound(ImmutableMap.of())
        }

        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_COMPOUND

        val size: kotlin.Int
            get() = entries.size

        fun isEmpty(): Boolean = entries.isEmpty()

        override fun matches(tag: NBTBase): Boolean {
            if (!tag.isCompound() || (tag as NBTTagCompound).size != entries.size) return false
            entries.forEach { (key, value) ->
                if (!value.matches(tag[key] ?: return false)) return false
            }
            return true
        }

        override fun toNbt(): NBTTagCompound {
            val tag = NBTTagCompound()
            entries.forEach { (key, value) ->
                tag[key] = value.toNbt()
            }
            return tag
        }

        override fun toString(): kotlin.String =
            "{${entries.entries.joinToString(", ") { (key, value) -> "$key: $value" }}}"

        object Serializer : FullSerializer<Compound, NBTTagCompound, TJson.Object> {
            override fun serializeToNbt(x: Compound): NBTTagCompound = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagCompound): Compound = dto.toImmutable()

            override fun serializeToJson(x: Compound): TJson.Object {
                val dto = TJson.Object()
                x.entries.forEach { (key, value) ->
                    dto[key] = ImmutableNbt.Serializer.serializeToJson(value)
                }
                return dto
            }

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Object): Compound {
                if (dto.isEmpty()) return EMPTY
                val builder = ImmutableMap.builder<kotlin.String, ImmutableNbt>()
                dto.forEachValue { key, valueDto ->
                    builder.put(key, ImmutableNbt.Serializer.deserializeFromJson(valueDto))
                }
                return Compound(builder.build())
            }

            override fun serializeToNetwork(buf: PacketBuffer, x: Compound) {
                // vanilla's encoder uses an END-terminated alist, which can't encode (invalid) inner END tags correctly
                buf.writeVarInt(x.entries.size)
                x.entries.forEach { (key, value) ->
                    buf.writeString(key)
                    ImmutableNbt.Serializer.serializeToNetwork(buf, value)
                }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Compound {
                val count = buf.readVarInt()
                if (count == 0) return EMPTY
                val builder = ImmutableMap.Builder<kotlin.String, ImmutableNbt>()
                repeat(count) {
                    builder.put(buf.readString(), ImmutableNbt.Serializer.deserializeFromNetwork(buf))
                }
                return Compound(builder.build())
            }

            object Nullable : NetworkSerializer<Compound?> {
                override fun serializeToNetwork(buf: PacketBuffer, x: Compound?) {
                    if (x == null) {
                        buf.writeVarInt(0)
                    } else {
                        buf.writeVarInt(x.entries.size + 1)
                        x.entries.forEach { (key, value) ->
                            buf.writeString(key)
                            ImmutableNbt.Serializer.serializeToNetwork(buf, value)
                        }
                    }
                }

                override fun deserializeFromNetwork(buf: PacketBuffer): Compound? {
                    when (val count = buf.readVarInt()) {
                        0 -> return null
                        1 -> return EMPTY
                        else -> {
                            val builder = ImmutableMap.Builder<kotlin.String, ImmutableNbt>()
                            repeat(count - 1) {
                                builder.put(buf.readString(), ImmutableNbt.Serializer.deserializeFromNetwork(buf))
                            }
                            return Compound(builder.build())
                        }
                    }
                }
            }
        }
    }

    data class List(val elements: ImmutableList<ImmutableNbt>) : ImmutableNbt {
        companion object {
            val EMPTY: List = List(ImmutableList.of())
        }

        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_LIST

        val size: kotlin.Int
            get() = elements.size

        fun isEmpty(): Boolean = elements.isEmpty()

        override fun matches(tag: NBTBase): Boolean {
            if (!tag.isList() || (tag as NBTTagList).tagCount() != elements.size) return false
            elements.forEachIndexed { index, element ->
                if (!element.matches(tag[index])) return false
            }
            return true
        }

        override fun toNbt(): NBTTagList = elements.mapToNbtList { it.toNbt() }

        override fun toString(): kotlin.String = "[${elements.joinToString(", ")}]"

        object Serializer : FullSerializer<List, NBTTagList, TJson.Array> {
            override fun serializeToNbt(x: List): NBTTagList = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagList): List = dto.toImmutable()

            override fun serializeToJson(x: List): TJson.Array {
                val dto = TJson.Array()
                x.elements.forEach { element ->
                    dto += ImmutableNbt.Serializer.serializeToJson(element)
                }
                return dto
            }

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Array): List {
                if (dto.isEmpty()) return EMPTY
                val builder = ImmutableList.builder<ImmutableNbt>()
                dto.forEachValue {
                    builder.add(ImmutableNbt.Serializer.deserializeFromJson(it))
                }
                return List(builder.build())
            }

            override fun serializeToNetwork(buf: PacketBuffer, x: List) {
                // vanilla's encoder uses an END-terminated list, which can't encode (invalid) inner END tags correctly
                buf.writeVarInt(x.elements.size)
                x.elements.forEach {
                    ImmutableNbt.Serializer.serializeToNetwork(buf, it)
                }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): List {
                val count = buf.readVarInt()
                if (count == 0) return EMPTY
                val builder = ImmutableList.Builder<ImmutableNbt>()
                repeat(count) {
                    builder.add(ImmutableNbt.Serializer.deserializeFromNetwork(buf))
                }
                return List(builder.build())
            }
        }
    }

    class ByteArray internal constructor(private val array: kotlin.ByteArray) : ImmutableNbt {
        companion object {
            fun copyOf(elements: kotlin.ByteArray): ByteArray = ByteArray(elements.copyOf())
        }

        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_BYTE_ARRAY

        val size: kotlin.Int
            get() = array.size

        fun getCopyOfArray(): kotlin.ByteArray = array.copyOf()

        fun copyArrayInto(
            dest: kotlin.ByteArray,
            destOffset: kotlin.Int = 0,
            startIndex: kotlin.Int = 0,
            endIndex: kotlin.Int = array.size
        ): kotlin.ByteArray = array.copyInto(dest, destOffset, startIndex, endIndex)

        fun contentEquals(o: kotlin.ByteArray): Boolean = array.contentEquals(o)

        override fun matches(tag: NBTBase): Boolean =
            tag.isByteArray() && array.contentEquals((tag as NBTTagByteArray).byteArray)

        override fun toNbt(): NBTTagByteArray = NBTTagByteArray(array.copyOf())

        override fun equals(other: Any?): Boolean = other is ByteArray && array.contentEquals(other.array)

        override fun hashCode(): kotlin.Int = 863122550 xor array.contentHashCode()

        override fun toString(): kotlin.String = "B[${array.joinToString(", ")}]"

        object Serializer : GameSerializer<ByteArray, NBTTagByteArray> {
            override fun serializeToNbt(x: ByteArray): NBTTagByteArray = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagByteArray): ByteArray = dto.toImmutable()

            override fun serializeToNetwork(buf: PacketBuffer, x: ByteArray) {
                buf.writeByteArray(x.array)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): ByteArray = ByteArray(buf.readByteArray())
        }
    }

    class IntArray internal constructor(private val array: kotlin.IntArray) : ImmutableNbt {
        companion object {
            fun copyOf(elements: kotlin.IntArray): IntArray = IntArray(elements.copyOf())
        }

        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_INT_ARRAY

        fun getCopyOfArray(): kotlin.IntArray = array.copyOf()

        fun copyArrayInto(
            dest: kotlin.IntArray,
            destOffset: kotlin.Int = 0,
            startIndex: kotlin.Int = 0,
            endIndex: kotlin.Int = array.size
        ): kotlin.IntArray = array.copyInto(dest, destOffset, startIndex, endIndex)

        fun contentEquals(o: kotlin.IntArray): Boolean = array.contentEquals(o)

        override fun matches(tag: NBTBase): Boolean =
            tag.isIntArray() && array.contentEquals((tag as NBTTagIntArray).intArray)

        override fun toNbt(): NBTTagIntArray = NBTTagIntArray(array.copyOf())

        override fun equals(other: Any?): Boolean = other is IntArray && array.contentEquals(other.array)

        override fun hashCode(): kotlin.Int = -428612333 xor array.contentHashCode()

        override fun toString(): kotlin.String = "I[${array.joinToString(", ")}]"

        object Serializer : GameSerializer<IntArray, NBTTagIntArray> {
            override fun serializeToNbt(x: IntArray): NBTTagIntArray = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagIntArray): IntArray = dto.toImmutable()

            override fun serializeToNetwork(buf: PacketBuffer, x: IntArray) {
                buf.writeVarInt(x.array.size)
                x.array.forEach { buf.writeInt(it) }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): IntArray {
                val array = IntArray(buf.readVarInt())
                array.indices.forEach { array[it] = buf.readInt() }
                return IntArray(array)
            }
        }
    }

    class LongArray internal constructor(private val array: kotlin.LongArray) : ImmutableNbt {
        companion object {
            fun copyOf(elements: kotlin.LongArray): LongArray = LongArray(elements.copyOf())
        }

        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_LONG_ARRAY

        val size: kotlin.Int
            get() = array.size

        fun getCopyOfArray(): kotlin.LongArray = array.copyOf()

        fun copyArrayInto(
            dest: kotlin.LongArray,
            destOffset: kotlin.Int = 0,
            startIndex: kotlin.Int = 0,
            endIndex: kotlin.Int = array.size
        ): kotlin.LongArray = array.copyInto(dest, destOffset, startIndex, endIndex)

        fun contentEquals(o: kotlin.LongArray): Boolean = array.contentEquals(o)

        override fun matches(tag: NBTBase): Boolean =
            tag.isLongArray() && array.contentEquals((tag as NBTTagLongArray).data)

        override fun toNbt(): NBTTagLongArray = NBTTagLongArray(array.copyOf())

        override fun equals(other: Any?): Boolean = other is LongArray && array.contentEquals(other.array)

        override fun hashCode(): kotlin.Int = 1343915845 xor array.contentHashCode()

        override fun toString(): kotlin.String = "L[${array.joinToString(", ")}]"

        object Serializer : GameSerializer<LongArray, NBTTagLongArray> {
            override fun serializeToNbt(x: LongArray): NBTTagLongArray = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagLongArray): LongArray = dto.toImmutable()

            override fun serializeToNetwork(buf: PacketBuffer, x: LongArray) {
                buf.writeVarInt(x.array.size)
                x.array.forEach { buf.writeLong(it) }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): LongArray {
                val array = LongArray(buf.readVarInt())
                array.indices.forEach { array[it] = buf.readLong() }
                return LongArray(array)
            }
        }
    }

    data class String(val value: kotlin.String) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_STRING

        override fun matches(tag: NBTBase): Boolean = tag.isString() && (tag as NBTTagString).string == value

        override fun toNbt(): NBTTagString = NBTTagString(value)

        override fun toString(): kotlin.String = "\"${TJson.escapeJsonString(value)}\""

        object Serializer : FullSerializer<String, NBTTagString, TJson.String> {
            override fun serializeToNbt(x: String): NBTTagString = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagString): String = String(dto.string)

            override fun serializeToJson(x: String): TJson.String = TJson.String(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.String): String = String(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: String) {
                buf.writeString(x.value)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): String = String(buf.readString())
        }
    }

    data class Byte(val value: kotlin.Byte) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_BYTE

        override fun matches(tag: NBTBase): Boolean = tag.isByte() && (tag as NBTTagByte).byte == value

        override fun toNbt(): NBTTagByte = NBTTagByte(value)

        override fun toString(): kotlin.String = "${value}B"

        object Serializer : FullSerializer<Byte, NBTTagByte, TJson.Byte> {
            override fun serializeToNbt(x: Byte): NBTTagByte = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagByte): Byte = Byte(dto.byte)

            override fun serializeToJson(x: Byte): TJson.Byte = TJson.Byte(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Byte): Byte = Byte(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Byte) {
                buf.writeByte(x.value.toInt())
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Byte = Byte(buf.readByte())
        }
    }

    data class Short(val value: kotlin.Short) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_SHORT

        override fun matches(tag: NBTBase): Boolean = tag.isShort() && (tag as NBTTagShort).short == value

        override fun toNbt(): NBTTagShort = NBTTagShort(value)

        override fun toString(): kotlin.String = "${value}S"

        object Serializer : FullSerializer<Short, NBTTagShort, TJson.Short> {
            override fun serializeToNbt(x: Short): NBTTagShort = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagShort): Short = Short(dto.short)

            override fun serializeToJson(x: Short): TJson.Short = TJson.Short(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Short): Short = Short(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Short) {
                buf.writeShort(x.value.toInt())
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Short = Short(buf.readShort())
        }
    }

    data class Int(val value: kotlin.Int) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_INT

        override fun matches(tag: NBTBase): Boolean = tag.isInt() && (tag as NBTTagInt).int == value

        override fun toNbt(): NBTTagInt = NBTTagInt(value)

        override fun toString(): kotlin.String = "${value}I"

        object Serializer : FullSerializer<Int, NBTTagInt, TJson.Int> {
            override fun serializeToNbt(x: Int): NBTTagInt = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagInt): Int = Int(dto.int)

            override fun serializeToJson(x: Int): TJson.Int = TJson.Int(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Int): Int = Int(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Int) {
                buf.writeInt(x.value)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Int = Int(buf.readInt())
        }
    }

    data class Long(val value: kotlin.Long) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_LONG

        override fun matches(tag: NBTBase): Boolean = tag.isLong() && (tag as NBTTagLong).long == value

        override fun toNbt(): NBTTagLong = NBTTagLong(value)

        override fun toString(): kotlin.String = "${value}L"

        object Serializer : FullSerializer<Long, NBTTagLong, TJson.Long> {
            override fun serializeToNbt(x: Long): NBTTagLong = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagLong): Long = Long(dto.long)

            override fun serializeToJson(x: Long): TJson.Long = TJson.Long(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Long): Long = Long(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Long) {
                buf.writeLong(x.value)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Long = Long(buf.readLong())
        }
    }

    data class Float(val value: kotlin.Float) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_FLOAT

        override fun matches(tag: NBTBase): Boolean = tag.isFloat() && (tag as NBTTagFloat).float == value

        override fun toNbt(): NBTTagFloat = NBTTagFloat(value)

        override fun toString(): kotlin.String = "${value}F"

        object Serializer : FullSerializer<Float, NBTTagFloat, TJson.Float> {
            override fun serializeToNbt(x: Float): NBTTagFloat = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagFloat): Float = Float(dto.float)

            override fun serializeToJson(x: Float): TJson.Float = TJson.Float(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Float): Float = Float(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Float) {
                buf.writeFloat(x.value)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Float = Float(buf.readFloat())
        }
    }

    data class Double(val value: kotlin.Double) : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_DOUBLE

        override fun matches(tag: NBTBase): Boolean = tag.isDouble() && (tag as NBTTagDouble).double == value

        override fun toNbt(): NBTTagDouble = NBTTagDouble(value)

        override fun toString(): kotlin.String = "${value}D"

        object Serializer : FullSerializer<Double, NBTTagDouble, TJson.Double> {
            override fun serializeToNbt(x: Double): NBTTagDouble = x.toNbt()

            override fun deserializeFromNbt(dto: NBTTagDouble): Double = Double(dto.double)

            override fun serializeToJson(x: Double): TJson.Double = TJson.Double(x.value)

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Double): Double = Double(dto.value)

            override fun serializeToNetwork(buf: PacketBuffer, x: Double) {
                buf.writeDouble(x.value)
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): Double = Double(buf.readDouble())
        }
    }

    // this should never be used because it will break serialization, but it's necessary for the 1:1 correspondence with
    // representable NBT structures, even invalid ones
    object End : ImmutableNbt {
        override val typeId: kotlin.Int
            get() = Constants.NBT.TAG_END

        override fun matches(tag: NBTBase): Boolean = tag.isEnd()

        override fun toNbt(): NBTTagEnd = NBTTagEnd()

        override fun toString(): kotlin.String = "END"
    }

    object Serializer : FullSerializer<ImmutableNbt, NBTBase, TJson> {
        override fun serializeToNbt(x: ImmutableNbt): NBTBase = x.toNbt()

        override fun deserializeFromNbt(dto: NBTBase): ImmutableNbt = dto.toImmutable()

        override fun serializeToJson(x: ImmutableNbt): TJson = when (x) {
            is Compound -> Compound.Serializer.serializeToJson(x)
            is List -> List.Serializer.serializeToJson(x)
            is String -> String.Serializer.serializeToJson(x)
            is Byte -> Byte.Serializer.serializeToJson(x)
            is Short -> Short.Serializer.serializeToJson(x)
            is Int -> Int.Serializer.serializeToJson(x)
            is Long -> Long.Serializer.serializeToJson(x)
            is Float -> Float.Serializer.serializeToJson(x)
            is Double -> Double.Serializer.serializeToJson(x)
            End -> TJson.Null // questionable
            is ByteArray -> throw UnsupportedOperationException("Cannot serialize byte array to JSON!")
            is IntArray -> throw UnsupportedOperationException("Cannot serialize int array to JSON!")
            is LongArray -> throw UnsupportedOperationException("Cannot serialize long array to JSON!")
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson): ImmutableNbt = when (dto) {
            is TJson.Object -> Compound.Serializer.deserializeFromJson(dto)
            is TJson.Array -> List.Serializer.deserializeFromJson(dto)
            is TJson.String -> String.Serializer.deserializeFromJson(dto)
            is TJson.Bool -> Byte(if (dto.value) 1 else 0) // lossy!
            is TJson.Byte -> Byte.Serializer.deserializeFromJson(dto)
            is TJson.Short -> Short.Serializer.deserializeFromJson(dto)
            is TJson.Int -> Int.Serializer.deserializeFromJson(dto)
            is TJson.Long -> Long.Serializer.deserializeFromJson(dto)
            is TJson.Float -> Float.Serializer.deserializeFromJson(dto)
            is TJson.Double -> Double.Serializer.deserializeFromJson(dto)
            TJson.Null -> End
        }

        override fun serializeToNetwork(buf: PacketBuffer, x: ImmutableNbt) {
            buf.writeByte(x.typeId)
            when (x) {
                is Compound -> Compound.Serializer.serializeToNetwork(buf, x)
                is List -> List.Serializer.serializeToNetwork(buf, x)
                is ByteArray -> ByteArray.Serializer.serializeToNetwork(buf, x)
                is IntArray -> IntArray.Serializer.serializeToNetwork(buf, x)
                is LongArray -> LongArray.Serializer.serializeToNetwork(buf, x)
                is String -> String.Serializer.serializeToNetwork(buf, x)
                is Byte -> Byte.Serializer.serializeToNetwork(buf, x)
                is Short -> Short.Serializer.serializeToNetwork(buf, x)
                is Int -> Int.Serializer.serializeToNetwork(buf, x)
                is Long -> Long.Serializer.serializeToNetwork(buf, x)
                is Float -> Float.Serializer.serializeToNetwork(buf, x)
                is Double -> Double.Serializer.serializeToNetwork(buf, x)
                End -> {}
            }
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): ImmutableNbt =
            when (val typeId = buf.readByte().toInt()) {
                Constants.NBT.TAG_COMPOUND -> Compound.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_LIST -> List.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_BYTE_ARRAY -> ByteArray.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_INT_ARRAY -> IntArray.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_LONG_ARRAY -> LongArray.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_STRING -> String.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_BYTE -> Byte.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_SHORT -> Short.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_INT -> Int.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_LONG -> Long.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_FLOAT -> Float.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_DOUBLE -> Double.Serializer.deserializeFromNetwork(buf)
                Constants.NBT.TAG_END -> End
                else -> throw IllegalStateException("Bad NBT tag type ID: $typeId")
            }
    }
}

infix fun ImmutableNbt?.matchesNullable(tag: NBTTagCompound?): Boolean =
    if (this != null) tag != null && matches(tag) else tag == null

fun NBTTagCompound.toImmutable(): ImmutableNbt.Compound {
    if (isEmpty) return ImmutableNbt.Compound.EMPTY
    val builder = ImmutableMap.builder<String, ImmutableNbt>()
    forEach { (key, value) ->
        builder.put(key, value.toImmutable())
    }
    return ImmutableNbt.Compound(builder.build())
}

fun NBTTagList.toImmutable(): ImmutableNbt.List {
    if (isEmpty) return ImmutableNbt.List.EMPTY
    val builder = ImmutableList.builder<ImmutableNbt>()
    forEach {
        builder.add(it.toImmutable())
    }
    return ImmutableNbt.List(builder.build())
}

fun NBTTagByteArray.toImmutable(): ImmutableNbt.ByteArray = ImmutableNbt.ByteArray.copyOf(byteArray)

fun NBTTagIntArray.toImmutable(): ImmutableNbt.IntArray = ImmutableNbt.IntArray.copyOf(intArray)

fun NBTTagLongArray.toImmutable(): ImmutableNbt.LongArray = ImmutableNbt.LongArray.copyOf(data)

fun NBTBase.toImmutable(): ImmutableNbt = when (this) {
    is NBTTagCompound -> toImmutable()
    is NBTTagList -> toImmutable()
    is NBTTagByteArray -> toImmutable()
    is NBTTagIntArray -> toImmutable()
    is NBTTagLongArray -> toImmutable()
    is NBTTagString -> ImmutableNbt.String(string)
    is NBTTagByte -> ImmutableNbt.Byte(byte)
    is NBTTagShort -> ImmutableNbt.Short(short)
    is NBTTagInt -> ImmutableNbt.Int(int)
    is NBTTagLong -> ImmutableNbt.Long(long)
    is NBTTagFloat -> ImmutableNbt.Float(float)
    is NBTTagDouble -> ImmutableNbt.Double(double)
    is NBTTagEnd -> ImmutableNbt.End
    else -> throw IllegalArgumentException(this.javaClass.canonicalName)
}
