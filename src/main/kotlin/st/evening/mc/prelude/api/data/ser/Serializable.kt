package st.evening.mc.prelude.api.data.ser

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.game.ServerSide

interface NbtSerializable<N : NBTBase> {
    fun writeToNbt(): N

    fun readFromNbt(dto: N)
}

interface NbtCompoundSerializable : NbtSerializable<NBTTagCompound>, ServerSideSerializable {
    fun writeToNbt(dto: NBTTagCompound)

    override fun writeToNbt(): NBTTagCompound = NBTTagCompound().also { writeToNbt(it) }

    @ServerSide
    override fun writeToNbtServerSide(dto: NBTTagCompound) {
        writeToNbt(dto)
    }

    @ServerSide
    override fun readFromNbtServerSide(dto: NBTTagCompound) {
        readFromNbt(dto)
    }
}

interface NetworkWritable {
    fun writeToNetwork(buf: PacketBuffer)
}

interface NetworkReadable {
    fun readFromNetwork(buf: PacketBuffer)
}

interface NetworkSerializable : NetworkReadable, NetworkWritable

interface GameSerializable<N : NBTBase> : NbtSerializable<N>, NetworkSerializable

interface JsonSerializable<J : TJson> {
    fun writeToJson(): J

    context(_: JsonPath)
    fun readFromJson(dto: J)
}

interface FullSerializable<N : NBTBase, J : TJson> : GameSerializable<N>, JsonSerializable<J>
