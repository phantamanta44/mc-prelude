package st.evening.mc.prelude.coremod.mixin;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import st.evening.mc.prelude.coremod.addons.AttachCapabilitiesEventAddons;

@Mixin(value = AttachCapabilitiesEvent.class, remap = false)
abstract class AttachCapabilitiesEventMixin implements AttachCapabilitiesEventAddons {
    private @Nullable ICapabilityProvider parentCapabilities = null;

    @Override
    public void prelude$setParentCapabilities(final ICapabilityProvider provider) {
        parentCapabilities = provider;
    }

    @Override
    public @Nullable ICapabilityProvider prelude$getParentCapabilities() {
        return parentCapabilities;
    }
}
