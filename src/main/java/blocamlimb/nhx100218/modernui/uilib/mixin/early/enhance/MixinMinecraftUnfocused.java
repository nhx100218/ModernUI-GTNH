package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig;

import net.minecraft.client.Minecraft;

/** 窗口失焦时降低最大帧率（仅在游戏世界中生效）。 */
@Mixin(Minecraft.class)
public class MixinMinecraftUnfocused {

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void modernui$unfocusedLimit(CallbackInfoReturnable<Integer> cir) {
        if (!UnfocusedConfig.framerateEnabled || !UnfocusedConfig.isUnfocused()) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.theWorld != null) {
            cir.setReturnValue(UnfocusedConfig.framerateLimit);
        }
    }
}
