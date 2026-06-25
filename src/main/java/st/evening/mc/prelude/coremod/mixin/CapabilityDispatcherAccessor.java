package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.util.INBTSerializable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CapabilityDispatcher.class, remap = false)
public interface CapabilityDispatcherAccessor {
    @Accessor("names")
    String[] prelude$names();

    @Accessor("writers")
    INBTSerializable<NBTBase>[] prelude$writers();
}
