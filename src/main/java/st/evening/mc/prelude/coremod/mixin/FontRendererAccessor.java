package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontRenderer.class)
public interface FontRendererAccessor {
    @Accessor("alpha")
    float prelude$alpha();

    @Accessor("locationFontTexture")
    ResourceLocation prelude$locationFontTexture();

    @Accessor("renderEngine")
    TextureManager prelude$renderEngine();
}
