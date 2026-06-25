package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStack.class)
public interface ItemStackAccessor {
    @Nullable
    @Accessor(value = "capabilities", remap = false)
    CapabilityDispatcher prelude$capabilities();
}
