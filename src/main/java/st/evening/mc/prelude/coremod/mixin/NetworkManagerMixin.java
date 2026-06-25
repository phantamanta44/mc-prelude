package st.evening.mc.prelude.coremod.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import st.evening.mc.prelude.coremod.addons.NetworkManagerAddons;

@Mixin(NetworkManager.class)
abstract class NetworkManagerMixin implements NetworkManagerAddons {
    @Shadow
    private Channel channel;
    @Shadow
    private ITextComponent terminationReason;

    @Override
    public void prelude$closeChannelAsync(final ITextComponent message) {
        if (channel.isOpen()) {
            channel.close().addListener(f -> terminationReason = message);
        }
    }
}
