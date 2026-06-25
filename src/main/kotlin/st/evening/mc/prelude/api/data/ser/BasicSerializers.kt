package st.evening.mc.prelude.api.data.ser

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.data.getEnumConstant
import st.evening.mc.prelude.api.util.data.readString

object ByteSerializer : FullSerializer<Byte, NBTTagByte, TJson.Byte> {
    override fun serializeToNbt(x: Byte): NBTTagByte = NBTTagByte(x)

    override fun deserializeFromNbt(dto: NBTTagByte): Byte = dto.byte

    override fun serializeToNetwork(buf: PacketBuffer, x: Byte) {
        buf.writeByte(x.toInt())
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Byte = buf.readByte()

    override fun serializeToJson(x: Byte): TJson.Byte = TJson.Byte(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Byte): Byte = dto.value
}

object ShortSerializer : FullSerializer<Short, NBTTagShort, TJson.Short> {
    override fun serializeToNbt(x: Short): NBTTagShort = NBTTagShort(x)

    override fun deserializeFromNbt(dto: NBTTagShort): Short = dto.short

    override fun serializeToNetwork(buf: PacketBuffer, x: Short) {
        buf.writeShort(x.toInt())
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Short = buf.readShort()

    override fun serializeToJson(x: Short): TJson.Short = TJson.Short(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Short): Short = dto.value
}

object IntSerializer : FullSerializer<Int, NBTTagInt, TJson.Int> {
    override fun serializeToNbt(x: Int): NBTTagInt = NBTTagInt(x)

    override fun deserializeFromNbt(dto: NBTTagInt): Int = dto.int

    override fun serializeToNetwork(buf: PacketBuffer, x: Int) {
        buf.writeInt(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Int = buf.readInt()

    override fun serializeToJson(x: Int): TJson.Int = TJson.Int(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Int): Int = dto.value
}

object LongSerializer : FullSerializer<Long, NBTTagLong, TJson.Long> {
    override fun serializeToNbt(x: Long): NBTTagLong = NBTTagLong(x)

    override fun deserializeFromNbt(dto: NBTTagLong): Long = dto.long

    override fun serializeToNetwork(buf: PacketBuffer, x: Long) {
        buf.writeLong(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Long = buf.readLong()

    override fun serializeToJson(x: Long): TJson.Long = TJson.Long(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Long): Long = dto.value
}

object FloatSerializer : FullSerializer<Float, NBTTagFloat, TJson.Float> {
    override fun serializeToNbt(x: Float): NBTTagFloat = NBTTagFloat(x)

    override fun deserializeFromNbt(dto: NBTTagFloat): Float = dto.float

    override fun serializeToNetwork(buf: PacketBuffer, x: Float) {
        buf.writeFloat(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Float = buf.readFloat()

    override fun serializeToJson(x: Float): TJson.Float = TJson.Float(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Float): Float = dto.value
}

object DoubleSerializer : FullSerializer<Double, NBTTagDouble, TJson.Double> {
    override fun serializeToNbt(x: Double): NBTTagDouble = NBTTagDouble(x)

    override fun deserializeFromNbt(dto: NBTTagDouble): Double = dto.double

    override fun serializeToNetwork(buf: PacketBuffer, x: Double) {
        buf.writeDouble(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Double = buf.readDouble()

    override fun serializeToJson(x: Double): TJson.Double = TJson.Double(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Double): Double = dto.value
}

object BoolSerializer : FullSerializer<Boolean, NBTTagByte, TJson.Bool> {
    override fun serializeToNbt(x: Boolean): NBTTagByte = NBTTagByte(if (x) 1 else 0)

    override fun deserializeFromNbt(dto: NBTTagByte): Boolean = dto.byte != 0.toByte()

    override fun serializeToNetwork(buf: PacketBuffer, x: Boolean) {
        buf.writeBoolean(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Boolean = buf.readBoolean()

    override fun serializeToJson(x: Boolean): TJson.Bool = TJson.Bool(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Bool): Boolean = dto.value
}

object StringSerializer : FullSerializer<String, NBTTagString, TJson.String> {
    override fun serializeToNbt(x: String): NBTTagString = NBTTagString(x)

    override fun deserializeFromNbt(dto: NBTTagString): String = dto.string

    override fun serializeToNetwork(buf: PacketBuffer, x: String) {
        buf.writeString(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): String = buf.readString()

    override fun serializeToJson(x: String): TJson.String = TJson.String(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.String): String = dto.value
}

class ConstantSerializer<T>(private val constant: T) : FullSerializer<T, NBTBase, TJson> {
    override fun serializeToNbt(x: T): NBTBase = NBTTagByte(0)

    override fun deserializeFromNbt(dto: NBTBase): T = constant

    override fun serializeToJson(x: T): TJson = TJson.Null

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson): T = constant

    override fun serializeToNetwork(buf: PacketBuffer, x: T) {}

    override fun deserializeFromNetwork(buf: PacketBuffer): T = constant
}

class EnumSerializer<E : Enum<E>>(private val enumType: Class<E>) : FullSerializer<E, NBTTagString, TJson.String> {
    companion object {
        inline operator fun <reified E : Enum<E>> invoke(): EnumSerializer<E> = EnumSerializer(E::class.java)
    }

    override fun serializeToNbt(x: E): NBTTagString = NBTTagString(x.name)

    override fun deserializeFromNbt(dto: NBTTagString): E = deserializeFromNbt(dto.string)
        ?: throw SerializationException("No such ${enumType.simpleName} value: ${dto.string}")

    fun deserializeFromNbt(stringValue: String): E? = try {
        enumType.getEnumConstant(stringValue)
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun serializeToJson(x: E): TJson.String = TJson.String(x.name)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.String): E = deserializeFromJson(dto.value)

    context(_: JsonPath)
    fun deserializeFromJson(stringValue: String): E {
        try {
            return enumType.getEnumConstant(stringValue)
        } catch (_: IllegalArgumentException) {
            throw SerializationException.withPath("No such ${enumType.simpleName} value: $stringValue")
        }
    }

    override fun serializeToNetwork(buf: PacketBuffer, x: E) {
        buf.writeByte(x.ordinal) // this could be too small if the enum has >256 constants for some reason
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): E {
        val index = buf.readByte().toInt()
        try {
            return enumType.getEnumConstant(index)
        } catch (_: IllegalArgumentException) {
            throw SerializationException("Index out of bounds for ${enumType.simpleName}: $index")
        }
    }
}

class SerializableSerializer<N : NBTBase, T : GameSerializable<N>>(
    private val factory: () -> T
) : GameSerializer<T, N> {
    override fun serializeToNbt(x: T): N = x.writeToNbt()

    override fun deserializeFromNbt(dto: N): T = factory().apply { readFromNbt(dto) }

    override fun serializeToNetwork(buf: PacketBuffer, x: T) {
        x.writeToNetwork(buf)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): T = factory().apply { readFromNetwork(buf) }
}
