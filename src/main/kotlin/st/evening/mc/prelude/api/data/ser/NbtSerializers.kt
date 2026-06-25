package st.evening.mc.prelude.api.data.ser

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTSizeTracker
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagEnd
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraftforge.common.util.Constants
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.forEachValue
import st.evening.mc.prelude.api.util.data.forEach
import st.evening.mc.prelude.api.util.data.plusAssign
import st.evening.mc.prelude.api.util.data.set
import st.evening.mc.prelude.coremod.mixin.NBTBaseAccessor

abstract class IdentityNbtSerializer<N : NBTBase, J : TJson> : FullSerializer<N, N, J> {
    override fun serializeToNbt(x: N): N = x

    override fun deserializeFromNbt(dto: N): N = dto
}

abstract class BaseNbtSerializer<N : NBTBase, J : TJson>(tagType: Int) : IdentityNbtSerializer<N, J>() {
    val tagType: Byte = tagType.toByte()

    override fun serializeToNetwork(buf: PacketBuffer, x: N) {
        NbtTagSerializer.serializeToNetwork(buf, x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): N =
        NbtTagSerializer.deserializeFromNetwork(buf).let {
            if (it.id != tagType) {
                throw SerializationException("${javaClass.simpleName} cannot deserialize tag: $it")
            }
            @Suppress("UNCHECKED_CAST")
            return it as N
        }
}

object NbtTagSerializer : IdentityNbtSerializer<NBTBase, TJson>() {
    override fun serializeToJson(x: NBTBase): TJson = when (x) {
        is NBTTagCompound -> NbtCompoundSerializer.serializeToJson(x)
        is NBTTagList -> NbtListSerializer.serializeToJson(x)
        is NBTTagString -> NbtStringSerializer.serializeToJson(x)
        is NBTTagByte -> NbtByteSerializer.serializeToJson(x)
        is NBTTagShort -> NbtShortSerializer.serializeToJson(x)
        is NBTTagInt -> NbtIntSerializer.serializeToJson(x)
        is NBTTagLong -> NbtLongSerializer.serializeToJson(x)
        is NBTTagFloat -> NbtFloatSerializer.serializeToJson(x)
        is NBTTagDouble -> NbtDoubleSerializer.serializeToJson(x)
        else -> throw SerializationException("Cannot serialize tag: $x")
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson): NBTBase = when (dto) {
        is TJson.Object -> NbtCompoundSerializer.deserializeFromJson(dto)
        is TJson.Array -> NbtListSerializer.deserializeFromJson(dto) // TODO nbt array types?
        is TJson.String -> NbtStringSerializer.deserializeFromJson(dto)
        is TJson.Bool -> NBTTagByte(if (dto.value) 1 else 0)
        is TJson.Byte -> NbtByteSerializer.deserializeFromJson(dto)
        is TJson.Short -> NbtShortSerializer.deserializeFromJson(dto)
        is TJson.Int -> NbtIntSerializer.deserializeFromJson(dto)
        is TJson.Long -> NbtLongSerializer.deserializeFromJson(dto)
        is TJson.Float -> NbtFloatSerializer.deserializeFromJson(dto)
        is TJson.Double -> NbtDoubleSerializer.deserializeFromJson(dto)
        else -> throw SerializationException.withPath("Cannot deserialize tag: $dto")
    }

    override fun serializeToNetwork(buf: PacketBuffer, x: NBTBase) {
        buf.writeByte(x.id.toInt())
        // CompressedStreamTools writes an empty string here for some reason; we don't do that
        (x as NBTBaseAccessor).`prelude$write`(ByteBufOutputStream(buf))
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): NBTBase {
        val tagType = buf.readByte()
        if (tagType == 0.toByte()) {
            return NBTTagEnd()
        }
        return NBTBase.create(tagType).also {
            // CompressedStreamTools writes an empty string here for some reason; we don't do that
            (it as NBTBaseAccessor).`prelude$read`(ByteBufInputStream(buf), 0, NBTSizeTracker.INFINITE)
        }
    }
}

object NbtCompoundSerializer : BaseNbtSerializer<NBTTagCompound, TJson.Object>(Constants.NBT.TAG_COMPOUND) {
    override fun serializeToJson(x: NBTTagCompound): TJson.Object = TJson.Object().also { dto ->
        x.forEach { (key, tag) ->
            dto[key] = NbtTagSerializer.serializeToJson(tag)
        }
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): NBTTagCompound = NBTTagCompound().also { tag ->
        dto.forEachValue { key, valueDto ->
            tag[key] = NbtTagSerializer.deserializeFromJson(valueDto)
        }
    }
}

object NbtListSerializer : BaseNbtSerializer<NBTTagList, TJson.Array>(Constants.NBT.TAG_LIST) {
    override fun serializeToJson(x: NBTTagList): TJson.Array = TJson.Array().also { dto ->
        x.forEach { dto += NbtTagSerializer.serializeToJson(it) }
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Array): NBTTagList = NBTTagList().also { tag ->
        dto.forEachValue { tag += NbtTagSerializer.deserializeFromJson(it) }
    }
}

object NbtStringSerializer : BaseNbtSerializer<NBTTagString, TJson.String>(Constants.NBT.TAG_STRING) {
    override fun serializeToJson(x: NBTTagString): TJson.String = TJson.String(x.string)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.String): NBTTagString = NBTTagString(dto.value)
}

object NbtByteSerializer : BaseNbtSerializer<NBTTagByte, TJson.Byte>(Constants.NBT.TAG_BYTE) {
    override fun serializeToJson(x: NBTTagByte): TJson.Byte = TJson.Byte(x.byte)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Byte): NBTTagByte = NBTTagByte(dto.value)
}

object NbtShortSerializer : BaseNbtSerializer<NBTTagShort, TJson.Short>(Constants.NBT.TAG_SHORT) {
    override fun serializeToJson(x: NBTTagShort): TJson.Short = TJson.Short(x.short)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Short): NBTTagShort = NBTTagShort(dto.value)
}

object NbtIntSerializer : BaseNbtSerializer<NBTTagInt, TJson.Int>(Constants.NBT.TAG_INT) {
    override fun serializeToJson(x: NBTTagInt): TJson.Int = TJson.Int(x.int)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Int): NBTTagInt = NBTTagInt(dto.value)
}

object NbtLongSerializer : BaseNbtSerializer<NBTTagLong, TJson.Long>(Constants.NBT.TAG_LONG) {
    override fun serializeToJson(x: NBTTagLong): TJson.Long = TJson.Long(x.long)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Long): NBTTagLong = NBTTagLong(dto.value)
}

object NbtFloatSerializer : BaseNbtSerializer<NBTTagFloat, TJson.Float>(Constants.NBT.TAG_FLOAT) {
    override fun serializeToJson(x: NBTTagFloat): TJson.Float = TJson.Float(x.float)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Float): NBTTagFloat = NBTTagFloat(dto.value)
}

object NbtDoubleSerializer : BaseNbtSerializer<NBTTagDouble, TJson.Double>(Constants.NBT.TAG_DOUBLE) {
    override fun serializeToJson(x: NBTTagDouble): TJson.Double = TJson.Double(x.double)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Double): NBTTagDouble = NBTTagDouble(dto.value)
}
