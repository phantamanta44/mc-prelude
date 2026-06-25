package st.evening.mc.prelude.coremod.addons;

import kotlin.NotImplementedError;

public interface GuiContainerAddons {
    default void prelude$setGeometry(final int x, final int y, final int width, final int height) {
        throw new NotImplementedError();
    }
}
