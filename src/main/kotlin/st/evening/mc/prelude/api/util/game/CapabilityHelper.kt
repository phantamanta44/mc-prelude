package st.evening.mc.prelude.api.util.game

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityDispatcher
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.event.AttachCapabilitiesEvent
import st.evening.mc.prelude.api.data.inbt.ImmutableNbt
import st.evening.mc.prelude.api.util.data.contains
import st.evening.mc.prelude.api.util.data.get
import st.evening.mc.prelude.coremod.addons.AttachCapabilitiesEventAddons
import st.evening.mc.prelude.coremod.mixin.CapabilityDispatcherAccessor

interface CapabilityVisitor {
    fun <T : Any> visit(capability: Capability<T>, instance: T)
}

interface CapabilityCollectionVisitor {
    fun <T : Any> visit(capability: Capability<T>, instances: Collection<T>)
}

val AttachCapabilitiesEvent<*>.parentCapabilities: ICapabilityProvider?
    get() = (this as AttachCapabilitiesEventAddons).`prelude$getParentCapabilities`()

@Suppress("CAST_NEVER_SUCCEEDS")
fun CapabilityDispatcher.hasCapsTag(): Boolean = (this as CapabilityDispatcherAccessor).`prelude$names`().isNotEmpty()

@Suppress("CAST_NEVER_SUCCEEDS")
fun CapabilityDispatcher.areCompatible(capsTag: NBTTagCompound): Boolean {
    val names = (this as CapabilityDispatcherAccessor).`prelude$names`()
    if (capsTag.size != names.size || names.any { it !in capsTag }) return false
    val writers = `prelude$writers`()
    names.forEachIndexed { index, key ->
        if (capsTag[key] != writers[index].serializeNBT()) return false
    }
    return true
}

infix fun CapabilityDispatcher?.areCompatibleNullable(capsTag: NBTTagCompound): Boolean =
    this?.areCompatible(capsTag) ?: capsTag.isEmpty

@Suppress("CAST_NEVER_SUCCEEDS")
fun CapabilityDispatcher.areCompatible(capsTag: ImmutableNbt.Compound): Boolean {
    val names = (this as CapabilityDispatcherAccessor).`prelude$names`()
    if (capsTag.entries.size != names.size || names.any { it !in capsTag.entries }) return false
    val writers = `prelude$writers`()
    names.forEachIndexed { index, key ->
        if (!capsTag.entries[key]!!.matches(writers[index].serializeNBT())) return false
    }
    return true
}

infix fun CapabilityDispatcher?.areCompatibleNullable(capsTag: ImmutableNbt.Compound): Boolean =
    this?.areCompatible(capsTag) ?: capsTag.isEmpty()
