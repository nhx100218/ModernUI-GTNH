package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;

/**
 * Captures the debug-options screen's batched text for ModernUI SDF rendering.
 * The text is replayed by {@code F3OptionsTextFlush} after the screen draws.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen", remap = false)
public abstract class MixinAngelicaDebugOptionsText {

    @Inject(method = "func_73863_a", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$beginTextFrame(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        if (FontConfig.replaceOrigin) {
            F3FontBridge.beginScreenText();
        }
    }
}
