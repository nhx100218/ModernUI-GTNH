package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * Feeds the F3 overlay text to the ModernUI SDF queue **without** cancelling the
 * native glyphs.
 *
 * <p>Angelica caches TextMeshes and replays them without calling this hook again.
 * Cancelling the native draw therefore produced grey/native text on replay
 * frames and white SDF text on capture frames (flicker). Keeping the native
 * white glyphs as the base layer is stable; the SDF layer is drawn over them.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugText$Batched", remap = false)
public abstract class MixinAngelicaDebugTextBatched {

    @Inject(method = "draw", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$queueOverlayText(CharSequence text, int start, int end, float x, float y,
            int color, CallbackInfo callbackInfo) {
        F3FontBridge.queueOverlayText(text, start, end, x, y, color);
    }
}
