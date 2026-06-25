package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import st.evening.mc.prelude.api.event.BlockStateChangedEvent;

@Mixin(World.class)
abstract class WorldMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(
            method = "markAndNotifyBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/chunk/Chunk;" +
                    "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;I)V",
            at = @At(value = "HEAD"),
            remap = false
    )
    public void markAndNotifyBlock(final BlockPos pos, final Chunk chunk, final IBlockState iblockstate,
                                   final IBlockState newState, final int flags, final CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new BlockStateChangedEvent((World) (Object) this, pos, iblockstate, newState));
    }
}
