package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import st.evening.mc.prelude.api.event.SlotClickEvent;

@Mixin(Container.class)
abstract class ContainerMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(
            method = "slotClick(IILnet/minecraft/inventory/ClickType;Lnet/minecraft/entity/player/EntityPlayer;" +
                    ")Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void slotClick(final int slotIndex, final int dragType, final ClickType clickType, final EntityPlayer player,
                          final CallbackInfoReturnable<ItemStack> ci) {
        if (MinecraftForge.EVENT_BUS.post(
                new SlotClickEvent(player, (Container) (Object) this, slotIndex, clickType, dragType))) {
            ci.setReturnValue(ItemStack.EMPTY);
        }
    }
}
