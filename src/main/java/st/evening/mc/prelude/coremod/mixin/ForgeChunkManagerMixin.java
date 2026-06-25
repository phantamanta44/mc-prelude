package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import st.evening.mc.prelude.Prelude;

@Mixin(value = ForgeChunkManager.class, remap = false)
abstract class ForgeChunkManagerMixin {
    @WrapOperation(
            method = "loadChunkEntities(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/World;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/tileentity/TileEntity;create(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)" +
                            "Lnet/minecraft/tileentity/TileEntity;"
            )
    )
    private static TileEntity loadChunkEntities_create(final World worldIn, final NBTTagCompound compound,
                                                       final Operation<TileEntity> original, final Chunk chunk) {
        final TileEntity te = Prelude.INSTANCE.getDefns().getTileEntityLoadService().tryLoad(worldIn, chunk, compound);
        return te != null ? te : original.call(worldIn, compound);
    }
}
