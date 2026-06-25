package st.evening.mc.prelude.coremod.mixin;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@Mixin(NBTBase.class)
public interface NBTBaseAccessor {
    @Invoker("read")
    void prelude$read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException;

    @Invoker("write")
    void prelude$write(DataOutput output) throws IOException;
}
