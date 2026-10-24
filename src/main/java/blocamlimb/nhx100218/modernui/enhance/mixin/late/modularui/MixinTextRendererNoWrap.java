package blocamlimb.nhx100218.modernui.enhance.mixin.late.modularui;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;

/**
 * 禁用 ModularUI2 文本自动折行：{@code TextRenderer.wrapLine} 直接返回不折行的原行。
 * 用于消除内置字体偏宽导致的 tooltip 意外换行。
 */
@Mixin(targets = "com.cleanroommc.modularui.drawable.text.TextRenderer")
public class MixinTextRendererNoWrap {

    @Inject(method = "wrapLine", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$wrapLine(String line, CallbackInfoReturnable<List<String>> cir) {
        if (TooltipConfig.enabled && TooltipConfig.modularUiNoWrap) {
            cir.setReturnValue(Collections.singletonList(line));
        }
    }
}
