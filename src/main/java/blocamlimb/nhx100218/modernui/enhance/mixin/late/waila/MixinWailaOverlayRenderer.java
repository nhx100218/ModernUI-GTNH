package blocamlimb.nhx100218.modernui.enhance.mixin.late.waila;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.WailaTooltipStyle;

/**
 * 把 Waila 的方形 HUD 框改成圆角。
 *
 * <p>目标为可选模组 Waila，仅在客户端且 Waila 已加载时由 {@code LateMixins} 注册；
 * 方法名不做混淆重映射（remap=false）。</p>
 */
@Mixin(targets = "mcp.mobius.waila.overlay.OverlayRenderer")
public class MixinWailaOverlayRenderer {

    @Inject(method = "drawTooltipBox", at = @At("HEAD"), cancellable = true, remap = false)
    private static void modernui$drawTooltipBox(int x, int y, int width, int height, int border,
            int gradTop, int gradBottom, CallbackInfo ci) {
        if (WailaTooltipStyle.draw(x, y, width, height, border, gradTop, gradBottom)) {
            ci.cancel();
        }
    }
}
