package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEvent.class)
public interface SoundEventAccessor {
    @Accessor("soundName")
    ResourceLocation prelude$soundName();
}
