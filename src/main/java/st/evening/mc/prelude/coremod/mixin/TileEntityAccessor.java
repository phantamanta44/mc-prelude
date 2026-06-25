package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TileEntity.class)
public interface TileEntityAccessor {
    @Invoker("setWorldCreate")
    void prelude$setWorldCreate(World world);
}
