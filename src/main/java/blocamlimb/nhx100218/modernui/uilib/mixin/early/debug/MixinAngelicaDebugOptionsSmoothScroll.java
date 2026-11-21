package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollModel;

/**
 * Smooth wheel scrolling for Angelica's debug-options screen, using the same
 * easing model/configuration as the other ModernUI lists.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen", remap = false)
public abstract class MixinAngelicaDebugOptionsSmoothScroll {

    private static final int WHEEL_ROWS = 3;

    @Shadow
    private int scrollRow;

    @Shadow
    private void scrollTo(int row) {
        throw new AssertionError();
    }

    @Shadow
    private int maxScroll() {
        throw new AssertionError();
    }

    @Unique
    private final SmoothScrollModel modernui$model = new SmoothScrollModel();
    @Unique
    private boolean modernui$animating;
    @Unique
    private int modernui$lastRow;
    @Unique
    private int modernui$wheel;

    @Redirect(method = "func_146274_d",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"),
            remap = false, require = 0)
    private int modernui$captureWheel() {
        modernui$wheel = Mouse.getEventDWheel();
        return 0;
    }

    @Inject(method = "func_146274_d", at = @At("TAIL"), remap = false, require = 0)
    private void modernui$applyWheel(CallbackInfo callbackInfo) {
        int wheel = modernui$wheel;
        modernui$wheel = 0;
        if (wheel == 0) {
            return;
        }
        int step = -Integer.signum(wheel) * WHEEL_ROWS;
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.listSmoothness == 0) {
            scrollTo(scrollRow + step);
            return;
        }
        if (!modernui$animating) {
            modernui$model.snap(scrollRow);
            modernui$animating = true;
        }
        double target = modernui$model.target() + step;
        target = Math.max(0.0D, Math.min(target, maxScroll()));
        modernui$model.setTarget(target);
    }

    @Inject(method = "func_73863_a", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$animate(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (!modernui$animating) {
            if (scrollRow != modernui$lastRow) {
                modernui$model.snap(scrollRow);
            }
            modernui$lastRow = scrollRow;
            return;
        }
        double next = modernui$model.update(
                SmoothScrollConfig.smoothness(SmoothScrollConfig.listSmoothness));
        int row = (int) Math.round(next);
        if (row != scrollRow) {
            scrollRow = row;
            scrollTo(row);
        }
        if (modernui$model.settled()) {
            modernui$animating = false;
        }
    }

    /** Keyboard/drag scrolling should cancel the wheel animation. */
    @Inject(method = "scrollTo", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$syncExternalScroll(int row, CallbackInfo callbackInfo) {
        if (row != scrollRow && modernui$animating) {
            modernui$model.snap(scrollRow);
            modernui$animating = false;
        }
    }
}
