package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.PacketUtil;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import st.evening.mc.prelude.Prelude;
import st.evening.mc.prelude.api.util.game.ItemHelperKt;

@Mixin(value = PacketUtil.class, remap = false)
abstract class PacketUtilMixin {
    @WrapOperation(
            method = "writeItemStackFromClientToServer(Lnet/minecraft/network/PacketBuffer;Lnet/minecraft/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getTagCompound()Lnet/minecraft/nbt/NBTTagCompound;"
            )
    )
    @Nullable
    private static NBTTagCompound writeItemStackFromClientToServer_getTagCompound(final ItemStack instance,
                                                                                  final Operation<@Nullable NBTTagCompound> original) {
        NBTTagCompound tag = original.call(instance);
        if (Prelude.INSTANCE.getConfig().getNetworking().getSendItemStackCapsToServer()) {
            final NBTTagCompound capsTag = ItemHelperKt.getCapsTag(instance);
            if (capsTag != null) {
                if (tag == null) {
                    tag = new NBTTagCompound();
                }
                tag.setTag("p$caps", capsTag);
            }
        }
        return tag;
    }
}
