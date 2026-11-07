package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * Angelica 新版 F3 的文本适配入口。
 *
 * <p>{@code DebugText.Batched} 是 F3 分组布局唯一的取字入口：测量走
 * {@code measure/advance}，采集走 {@code draw}。这里把测量替换为 ModernUI 的
 * 字体度量，并在采集阶段拦下文本交给 {@link F3FontBridge}，让面板布局与自定义
 * 字体宽度一致。</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugText$Batched", remap = false)
public abstract class MixinAngelicaDebugTextBatched {

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$captureModernUiText(CharSequence text, int start, int end, float x, float y,
            int color, CallbackInfo callbackInfo) {
        if (F3FontBridge.recordIfActive(this, text, start, end, x, y, color)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "measure", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$measure(CharSequence text, int start, int end,
            CallbackInfoReturnable<Float> callbackInfo) {
        Float width = F3FontBridge.tryMeasure(text, start, end);
        if (width != null) {
            callbackInfo.setReturnValue(width);
        }
    }

    @Inject(method = "advance", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$advance(char character, boolean unicode,
            CallbackInfoReturnable<Float> callbackInfo) {
        Float width = F3FontBridge.tryAdvance(character);
        if (width != null) {
            callbackInfo.setReturnValue(width);
        }
    }
}
