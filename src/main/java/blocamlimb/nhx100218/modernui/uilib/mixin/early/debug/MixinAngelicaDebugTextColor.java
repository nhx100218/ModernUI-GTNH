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
        // The native glyphs are only a layout/cache carrier now; ModernUI draws
        // the visible SDF text on top at the end of the overlay. Making the
        // native layer transparent avoids double/bold text.
        return color & 0x00FFFFFF;
    }
}
