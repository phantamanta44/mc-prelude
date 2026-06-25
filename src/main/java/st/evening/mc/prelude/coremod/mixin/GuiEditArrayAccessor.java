package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiEditArray;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiEditArray.class, remap = false)
public interface GuiEditArrayAccessor {
    @Accessor("parentScreen")
    @Nullable GuiScreen prelude$parentScreen();

    @Accessor("enabled")
    boolean prelude$enabled();

    @Accessor("title")
    String prelude$title();

    @Accessor("titleLine2")
    @Nullable String prelude$titleLine2();

    @Accessor("titleLine3")
    @Nullable String prelude$titleLine3();
}
