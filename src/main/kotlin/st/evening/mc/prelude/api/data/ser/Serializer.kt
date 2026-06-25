package st.evening.mc.prelude.api.data.ser

import net.minecraft.nbt.NBTBase
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

interface NbtSerializer<T, N : NBTBase> {
    fun serializeToNbt(x: T): N

    fun deserializeFromNbt(dto: N): T
}

interface JsonSerializer<T, J : TJson> {
    fun serializeToJson(x: T): J

    context(_: JsonPath)
    fun deserializeFromJson(dto: J): T
}

interface NetworkSerializer<T> {
    fun serializeToNetwork(buf: PacketBuffer, x: T)

    fun deserializeFromNetwork(buf: PacketBuffer): T
}

interface GameSerializer<T, N : NBTBase> : NbtSerializer<T, N>, NetworkSerializer<T>

interface FullSerializer<T, N : NBTBase, J : TJson> : GameSerializer<T, N>, JsonSerializer<T, J>

context(serializer: NbtSerializer<T, N>)
fun <T, N : NBTBase> T.serializeToNbt(): N = serializer.serializeToNbt(this)

context(serializer: NbtSerializer<T, N>)
fun <T, N : NBTBase> N.deserializerFromNbt(): T = serializer.deserializeFromNbt(this)

context(serializer: JsonSerializer<T, J>)
fun <T, J : TJson> T.serializeToJson(): J = serializer.serializeToJson(this)

context(serializer: JsonSerializer<T, J>, _: JsonPath)
fun <T, J : TJson> J.deserializeFromJson(): T = serializer.deserializeFromJson(this)

context(serializer: NetworkSerializer<T>)
fun <T> T.serializeToNetwork(buf: PacketBuffer) {
    serializer.serializeToNetwork(buf, this)
}

context(serializer: NetworkSerializer<T>)
fun <T> PacketBuffer.deserializeFromNetwork(): T = serializer.deserializeFromNetwork(this)

class SerializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    constructor(path: JsonPath, message: String, cause: Throwable? = null) : this("$path: $message", cause)

    companion object {
        context(path: JsonPath)
        fun withPath(message: String, cause: Throwable? = null): SerializationException =
            SerializationException(path, message, cause)
    }
}
