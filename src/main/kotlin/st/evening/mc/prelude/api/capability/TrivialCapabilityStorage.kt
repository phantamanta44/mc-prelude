package st.evening.mc.prelude.api.capability

import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

object TrivialCapabilityStorage : Capability.IStorage<Any> {
    // technically not sound, but should work under erasure
    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    inline operator fun <C> invoke(): Capability.IStorage<C> = this as Capability.IStorage<C>

    override fun writeNBT(capability: Capability<Any?>, instance: Any, side: EnumFacing): NBTBase? = null

    override fun readNBT(capability: Capability<Any>, instance: Any, side: EnumFacing, nbt: NBTBase) {}
}
