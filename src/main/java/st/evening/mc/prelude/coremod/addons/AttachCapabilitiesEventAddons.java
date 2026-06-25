package st.evening.mc.prelude.coremod.addons;

import kotlin.NotImplementedError;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jspecify.annotations.Nullable;

public interface AttachCapabilitiesEventAddons {
    default void prelude$setParentCapabilities(final @Nullable ICapabilityProvider provider) {
        throw new NotImplementedError();
    }

    default @Nullable ICapabilityProvider prelude$getParentCapabilities() {
        throw new NotImplementedError();
    }
}
