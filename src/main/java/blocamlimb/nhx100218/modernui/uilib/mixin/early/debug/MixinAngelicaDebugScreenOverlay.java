package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * 在 Angelica 新版 F3 的整帧绘制边界内补画 ModernUI 文本。
 *
 * <p>Angelica 先把面板矩形和原生字形一起烘焙进网格，再在帧内 replay。桥接在采集阶段
 * 只拦字形、保留矩形，等整帧绘制结束（此时网格矩形已经在屏幕上）再按采集时的矩阵
 * 补画文字，保证文字在最上层且不会与 Angelica 的批量提交互相穿插。</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", remap = false)
public abstract class MixinAngelicaDebugScreenOverlay {

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void modernui$beginFrame(CallbackInfo callbackInfo) {
        F3FontBridge.beginFrame();
    }

    @Inject(method = "draw", at = @At("RETURN"), require = 0)
    private void modernui$flushFrame(CallbackInfo callbackInfo) {
        F3FontBridge.flushFrame(this);
    }
}
