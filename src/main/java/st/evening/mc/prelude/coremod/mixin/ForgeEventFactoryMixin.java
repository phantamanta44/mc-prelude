package st.evening.mc.prelude.coremod.mixin;

import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import st.evening.mc.prelude.coremod.addons.AttachCapabilitiesEventAddons;

@Mixin(value = ForgeEventFactory.class, remap = false)
public class ForgeEventFactoryMixin {
    @Inject(
            method = "gatherCapabilities(Lnet/minecraftforge/event/AttachCapabilitiesEvent;Lnet/minecraftforge/common/capabilities/ICapabilityProvider;)" +
                    "Lnet/minecraftforge/common/capabilities/CapabilityDispatcher;",
            at = @At("HEAD")
    )
    private static void gatherCapabilities(final AttachCapabilitiesEvent<?> event,
                                           final @Nullable ICapabilityProvider parent,
                                           final CallbackInfoReturnable<CapabilityDispatcher> ci) {
        ((AttachCapabilitiesEventAddons) event).prelude$setParentCapabilities(parent);
    }
}
