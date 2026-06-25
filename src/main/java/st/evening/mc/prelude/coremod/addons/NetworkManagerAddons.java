package st.evening.mc.prelude.coremod.addons;

import kotlin.NotImplementedError;
import net.minecraft.util.text.ITextComponent;

public interface NetworkManagerAddons {
    default void prelude$closeChannelAsync(final ITextComponent message) {
        throw new NotImplementedError();
    }
}
