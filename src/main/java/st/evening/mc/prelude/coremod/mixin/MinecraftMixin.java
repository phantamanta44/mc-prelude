package st.evening.mc.prelude.coremod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import st.evening.mc.prelude.api.event.ClientClickBlockEvent;
import st.evening.mc.prelude.api.event.InitializationCompleteEvent;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(
            method = "init()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/client/FMLClientHandler;onInitializationComplete()V",
                    remap = false
            )
    )
    public void init(final CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new InitializationCompleteEvent());
    }

    @WrapOperation(
            method = "clickMouse()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;clickBlock" +
                            "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z"
            )
    )
    public boolean clickMouse_onPlayerDamageBlock(final PlayerControllerMP instance, final BlockPos loc,
                                                  final EnumFacing face, final Operation<Boolean> original) {
        final ClientClickBlockEvent event = new ClientClickBlockEvent(loc, face);
        MinecraftForge.EVENT_BUS.post(event);
        switch (event.getClickResult()) {
            case SUCCESS:
                Minecraft.getMinecraft().leftClickCounter = 4;
                return false;
            case PASS: // let the default handling go through
                return original.call(instance, loc, face);
            case FAIL:
                return false;
        }
        throw new IllegalStateException(event.getClickResult().toString());
    }

    @WrapOperation(
            method = "sendClickBlockToController(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;onPlayerDamageBlock" +
                            "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z"
            )
    )
    public boolean sendClickBlockToController_onPlayerDamageBlock(final PlayerControllerMP instance,
                                                                  final BlockPos posBlock,
                                                                  final EnumFacing directionFacing,
                                                                  final Operation<Boolean> original) {
        if (instance.blockHitDelay > 0) return original.call(instance, posBlock, directionFacing);
        final ClientClickBlockEvent event = new ClientClickBlockEvent(posBlock, directionFacing);
        MinecraftForge.EVENT_BUS.post(event);
        switch (event.getClickResult()) {
            case SUCCESS:
                final Minecraft mc = Minecraft.getMinecraft();
                mc.leftClickCounter = 4;
                mc.player.swingArm(EnumHand.MAIN_HAND);
                return false;
            case PASS: // let the default handling go through
                return original.call(instance, posBlock, directionFacing);
            case FAIL:
                return false;
        }
        throw new IllegalStateException(event.getClickResult().toString());
    }
}
