package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * Opens/closes the SDF overlay window around the F3 overlay draw, and forces a
 * fresh text capture every frame.
 *
 * <p>Angelica normally replays cached TextMeshes. A replay frame never calls the
 * text hook, so the SDF layer would only appear on capture frames and the font
 * would visibly switch between native and SDF. Forcing the dirty flags makes
 * every visible frame capture and replay the SDF layer consistently. The F3
 * overlay only exists while the player holds F3 open, so the cost is bounded.</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", remap = false)
public abstract class MixinAngelicaDebugScreenOverlay {

    private static final Field CHROME_DIRTY = fieldOf("chromeDirty");
    private static final Field GROUP_DIRTY = fieldOf("groupDirty");

    @Inject(method = "draw", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$beginOverlayText(CallbackInfo callbackInfo) {
        forceRecapture();
        F3FontBridge.beginFrame();
    }

    @Inject(method = "draw", at = @At("RETURN"), remap = false, require = 0)
    private void modernui$flushOverlayText(CallbackInfo callbackInfo) {
        F3FontBridge.flushFrame(this);
    }

    private void forceRecapture() {
        try {
            if (CHROME_DIRTY != null) {
                CHROME_DIRTY.setBoolean(this, true);
            }
            if (GROUP_DIRTY != null) {
                Object value = GROUP_DIRTY.get(this);
                if (value instanceof boolean[]) {
                    Arrays.fill((boolean[]) value, true);
                }
            }
        } catch (Throwable ignored) {
            // Fall back to Angelica's own cache policy; text stays visible.
        }
    }

    private static Field fieldOf(String name) {
        try {
            Field field = Class.forName("com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay")
                    .getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable absent) {
            return null;
        }
    }
}
