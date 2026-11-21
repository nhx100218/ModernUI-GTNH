package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Recolors Angelica's F3 text without replacing its renderer.
 *
 * <p>Replacing the batched glyph path with SDF text proved unstable (cached
 * meshes replay the native glyphs on some frames, which produced grey flicker).
 * The stable fix is to keep Angelica's renderer and force debug text to be
 * opaque white, while preserving semantic colours.</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugText$Batched", remap = false)
public abstract class MixinAngelicaDebugTextColor {

    @ModifyVariable(method = "draw", at = @At("HEAD"), argsOnly = true, index = 6,
            remap = false, require = 0)
    private int modernui$whiteDebugText(int color) {
        int argb = color == 0 ? 0xFFFFFFFF : (color | 0xFF000000);
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        // Neutral labels (the usual F3 grey) become pure white; keep red/green/
        // yellow warnings and values readable.
        if (red >= 0x80 && Math.abs(red - green) <= 12 && Math.abs(green - blue) <= 12) {
            return 0xFFFFFFFF;
        }
        return argb;
    }
}
