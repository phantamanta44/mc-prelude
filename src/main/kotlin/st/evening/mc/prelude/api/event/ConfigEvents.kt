package st.evening.mc.prelude.api.event

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.eventhandler.Event

abstract class ConfigSyncEvent : Event() {
    abstract val configs: Map<String, Map<String, NBTTagCompound>>

    class Build(
        val player: EntityPlayer,
        override val configs: MutableMap<String, MutableMap<String, NBTTagCompound>>
    ) : ConfigSyncEvent() {
        fun addConfigData(modId: String, configId: String, configData: NBTTagCompound) {
            configs.getOrPut(modId) { mutableMapOf() }[configId] = configData
        }
    }

    @HasResult
    class Receive(override val configs: Map<String, Map<String, NBTTagCompound>>) : ConfigSyncEvent() {
        fun getConfigData(modId: String, configId: String): NBTTagCompound? = configs[modId]?.get(configId)
    }
}
