package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.client.F3OptionsTooltip;

/**
 * Takes over the debug-options tooltip so it can be redrawn with ModernUI SDF
 * text after the screen finishes (the screen's second batched layer kept the
 * tooltip text on Angelica's bitmap font).
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen", remap = false)
public abstract class MixinAngelicaDebugOptionsTooltip {

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$captureTooltip(List<String> lines, int mouseX, int mouseY,
            CallbackInfo callbackInfo) {
        F3OptionsTooltip.capture(lines, mouseX, mouseY);
        callbackInfo.cancel();
    }
}
