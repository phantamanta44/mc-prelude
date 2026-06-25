package st.evening.mc.prelude.api.data.ser

import net.minecraft.nbt.NBTTagCompound
import st.evening.mc.prelude.api.util.game.ServerSide

interface ServerSideSerializable {
    @ServerSide
    fun writeToNbtServerSide(): NBTTagCompound = NBTTagCompound().also { writeToNbtServerSide(it) }

    @ServerSide
    fun writeToNbtServerSide(dto: NBTTagCompound)

    @ServerSide
    fun readFromNbtServerSide(dto: NBTTagCompound)
}
