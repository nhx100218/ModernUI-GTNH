package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * 捕获中止时清理已采集但未完成 replay 的 ModernUI 文本，避免半成品残留在下一帧。
 *
 * <p>正常 replay 的生命周期由 {@code DebugScreenOverlay.draw} 边界统一处理；
 * 这里只保留 abort 清理，注入点必须存在且稳定。</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.client.font.BatchingFontRenderer", remap = false)
public abstract class MixinAngelicaBatchingFontRenderer {

    @Inject(method = "abortCapture()V", at = @At("HEAD"), require = 0)
    private void modernui$abortCapture(CallbackInfo callbackInfo) {
        F3FontBridge.clearCurrentCapture(this);
    }
}
