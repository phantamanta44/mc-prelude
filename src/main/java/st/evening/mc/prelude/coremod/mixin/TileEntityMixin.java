package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import st.evening.mc.prelude.Prelude;

@Mixin(TileEntity.class)
abstract class TileEntityMixin {
    @WrapOperation(
            method = "create(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/tileentity/TileEntity;",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/util/registry/RegistryNamespaced;getObject(Ljava/lang/Object;)Ljava/lang/Object;"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Class;newInstance()Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private static Object create_newInstance(final Class<?> instance, final Operation<Object> original,
                                             final World worldIn, final NBTTagCompound compound) {
        final TileEntity te = Prelude.INSTANCE.getDefns().getTileEntityLoadService()
                .tryConstruct(instance, worldIn, compound);
        return te != null ? te : original.call(instance);
    }
}
