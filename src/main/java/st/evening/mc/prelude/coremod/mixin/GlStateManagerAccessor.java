package st.evening.mc.prelude.coremod.mixin;

import kotlin.NotImplementedError;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor {
    @Accessor("stencilState")
    static GlStateManager.StencilState prelude$stencilState() {
        throw new NotImplementedError();
    }
}
