package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(NBTTagCompound.class)
public interface NBTTagCompoundAccessor {
    @Accessor("tagMap")
    Map<String, NBTBase> prelude$tagMap();
}
