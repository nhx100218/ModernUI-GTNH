package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig;

import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.settings.GameSettings;

/** 窗口失焦时按系数降低各音轨音量。 */
@Mixin(GameSettings.class)
public class MixinGameSettingsUnfocused {

    @Inject(method = "getSoundLevel", at = @At("RETURN"), cancellable = true)
    private void modernui$unfocusedVolume(SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (!UnfocusedConfig.volumeEnabled || !UnfocusedConfig.isUnfocused()) {
            return;
        }
        cir.setReturnValue(cir.getReturnValueF() * UnfocusedConfig.volumeFactor);
    }
}
