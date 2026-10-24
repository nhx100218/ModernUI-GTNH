package blocamlimb.nhx100218.modernui.enhance.mixin.late.modularui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;

/**
 * 禁用 ModularUI2 文本折行：把 {@code RichTextCompiler.compileLines} 的 maxWidth 入参强制为极大值。
 * 这才是 ModularUI 真正的折行入口（TextRenderer.wrapLine 不是）。
 */
@Mixin(targets = "com.cleanroommc.modularui.drawable.text.RichTextCompiler")
public class MixinRichTextCompilerNoWrap {

    // 实例方法：this=0, FontRenderer=1, List=2, int maxWidth=3, float=4。
    @ModifyVariable(method = "compileLines", at = @At("HEAD"), argsOnly = true, index = 3, remap = false)
    private int modernui$maxWidth(int maxWidth) {
        if (TooltipConfig.enabled && TooltipConfig.modularUiNoWrap) {
            return 100000;
        }
        return maxWidth;
    }
}
