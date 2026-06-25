package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import st.evening.mc.prelude.api.gui.ExtraMouseInputScreen;

@Mixin(GuiScreen.class)
abstract class GuiScreenMixin {
    @Inject(
            method = "handleMouseInput()V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lorg/lwjgl/input/Mouse;getEventButton()I",
                    remap = false
            )
    )
    public void handleMouseInput(final CallbackInfo ci,
                                 @Local(name = "i") final int mouseX, @Local(name = "j") final int mouseY) {
        if (this instanceof ExtraMouseInputScreen) {
            ((ExtraMouseInputScreen) this).handleExtraMouseInput(mouseX, mouseY);
        }
    }
}
