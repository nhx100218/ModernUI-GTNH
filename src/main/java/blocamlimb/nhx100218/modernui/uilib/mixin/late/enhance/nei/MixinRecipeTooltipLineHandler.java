package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.recipe.NEIRecipeWidget;
import codechicken.nei.recipe.RecipeTooltipLineHandler;

/**
 * NEI 配方预览（tooltip 里的「工作台页面」）标题灰条按 {@code getSize().width} 居中，
 * 而标题文字却按 {@code widget.w} 居中——两者宽度不同导致灰条整体偏右。
 * 这里让灰条也以 {@code widget.w} 居中，与文字对齐。
 */
@Mixin(RecipeTooltipLineHandler.class)
public class MixinRecipeTooltipLineHandler {

    /** 灰色标题条向右偏移的像素数（可按观感调整；负值左移）。 */
    private static final int BAR_X_SHIFT = 1;
    /** 灰色标题条相对 {@code widget.w} 的左右各留白。 */
    private static final int BAR_WIDTH_MARGIN = 10;

    @Shadow(remap = false)
    private NEIRecipeWidget widget;

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/gui/GuiDraw;drawRect(IIIII)V"), remap = false)
    private void modernui$titleBar(int x, int y, int width, int height, int color) {
        int barWidth = width;
        if (widget != null) {
            barWidth = widget.w - BAR_WIDTH_MARGIN;
        }
        if (barWidth < 0) {
            barWidth = 0;
        }
        GuiDraw.drawRect(x + BAR_X_SHIFT, y, barWidth, height, color);
    }
}
