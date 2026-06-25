package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import st.evening.mc.prelude.coremod.addons.GuiContainerAddons;

@Mixin(GuiContainer.class)
abstract class GuiContainerMixin implements GuiContainerAddons {
    @Shadow
    protected int xSize;
    @Shadow
    protected int ySize;
    @Shadow
    protected int guiLeft;
    @Shadow
    protected int guiTop;

    @Override
    public void prelude$setGeometry(final int x, final int y, final int width, final int height) {
        guiLeft = x;
        guiTop = y;
        xSize = width;
        ySize = height;
    }
}
