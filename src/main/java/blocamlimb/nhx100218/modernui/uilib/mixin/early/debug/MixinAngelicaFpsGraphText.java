package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import net.minecraft.client.gui.ScaledResolution;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;

/**
 * Captures the FPS/TPS graph text (F3+Alt / F3+2/3) for ModernUI.
 *
 * <p>The graphs are rendered from Angelica's overlay event, outside
 * {@code DebugScreenOverlay.draw}, so they need their own capture window.
 * Angelica routes the graph labels through {@code angelica$drawStringBatched};
 * the shared queue replays them with SDF glyphs after the graph geometry.</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.F3Graph", remap = false)
public abstract class MixinAngelicaFpsGraphText {

    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$beginGraphText(ScaledResolution resolution, int ticks, CallbackInfo callbackInfo) {
        if (FontConfig.replaceOrigin) {
            F3FontBridge.beginGraphFrame();
        }
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 0)
    private void modernui$endGraphText(ScaledResolution resolution, int ticks, CallbackInfo callbackInfo) {
        if (FontConfig.replaceOrigin) {
            F3FontBridge.endGraphFrame();
        }
    }
}
