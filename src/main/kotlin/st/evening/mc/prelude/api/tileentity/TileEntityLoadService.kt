package st.evening.mc.prelude.api.tileentity

import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.coremod.mixin.TileEntityAccessor

class TileEntityLoadService {
    private val registry: MutableMap<String, TileEntityType<*>> = mutableMapOf()
    private val teClassTable: MutableMap<Class<*>, TileEntityType<*>> = mutableMapOf()

    fun register(type: TileEntityType<*>) {
        // malformed IDs containing colons could cause spurious conflicts, so please don't do that
        val exById = registry.putIfAbsent(type.id.toString(), type)
        if (exById != null) {
            throw IllegalStateException(
                "Duplicate tile entity registration for ID ${type.id}! " +
                    "Existing: ${exById.teClass.canonicalName}; new: ${type.teClass.canonicalName}"
            )
        }
        val exByClass = teClassTable.putIfAbsent(type.teClass, type)
        if (exByClass != null) {
            throw IllegalStateException(
                "Duplicate tile entity registration for class ${type.teClass.canonicalName}! " +
                    "Existing: ${exByClass.id}; new: ${type.id}"
            )
        }
    }

    fun tryConstruct(teClass: Class<*>, world: World, dataTag: NBTTagCompound): TileEntity? {
        val type = teClassTable[teClass] ?: return null
        val pos = BlockPos(dataTag.getInteger("x"), dataTag.getInteger("y"), dataTag.getInteger("z"))
        // this getBlockState call will cause a chunk-loading loop if create is being called by a chunk-loading task!
        // workarounds for vanilla/forge loaders are implemented in AnvilChunkLoaderMixin and ForgeChunkManagerMixin,
        // but custom chunk-loading logic from other mods may cause prelude mods relying on this behaviour to break!
        return type.factory(world, if (world.isBlockLoaded(pos)) world.getBlockState(pos) else Blocks.AIR.defaultState)
    }

    fun tryLoad(world: World, chunk: Chunk, dataTag: NBTTagCompound): TileEntity? {
        try {
            val te = (registry[dataTag.getString("id")] ?: return null).factory(
                world, chunk.getBlockState(dataTag.getInteger("x"), dataTag.getInteger("y"), dataTag.getInteger("z"))
            )
            (te as TileEntityAccessor).`prelude$setWorldCreate`(world)
            te.readFromNBT(dataTag)
            return te
        } catch (e: Exception) {
            Prelude.logger.warn(
                "Failed to load tile entity {}; falling back to vanilla loader!", dataTag.getString("id"), e
            )
            return null
        }
    }
}
