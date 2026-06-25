package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PacketBuffer.class)
abstract class PacketBufferMixin {
    @WrapOperation(
            method = "readItemStack()Lnet/minecraft/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;readNBTShareTag(Lnet/minecraft/item/ItemStack;Lnet/minecraft/nbt/NBTTagCompound;)V",
                    remap = false
            )
    )
    @SuppressWarnings("DataFlowIssue")
    public void readItemStack_readNBTShareTag(final Item instance, final ItemStack stack,
                                              final @Nullable NBTTagCompound nbt, final Operation<Void> original) {
        if (nbt != null) {
            final NBTBase capsTag = nbt.getTag("p$caps");
            if (capsTag instanceof NBTTagCompound) {
                nbt.removeTag("p$caps");
                final CapabilityDispatcher dispatcher = ((ItemStackAccessor) (Object) stack).prelude$capabilities();
                if (dispatcher != null) {
                    dispatcher.deserializeNBT((NBTTagCompound) capsTag);
                }
            }
        }
        original.call(instance, stack, nbt);
    }
}
