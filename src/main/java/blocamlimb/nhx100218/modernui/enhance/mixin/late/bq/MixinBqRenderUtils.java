package blocamlimb.nhx100218.modernui.enhance.mixin.late.bq;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

/**
 * 适配 ModernUI 字体渲染管线 + 本模组选中样式：
 *
 * <ul>
 *   <li><b>宽度口径</b>：BQ 的 {@code getStringWidth} 在 GTNHLib 存在时走
 *       {@code FontRendering.getStringWidth}（按 {@code getCharWidth} 累加，绕过了 ModernUI 对
 *       {@code FontRenderer} 的替换），而 BQ 文本实际经 {@code FontRenderer.drawString} 由 ModernUI
 *       渲染，两者不一致 → 光标随字数漂移。这里统一改用 {@code FontRenderer.getStringWidth}。</li>
 *   <li><b>选中框</b>：BQ 的 {@code drawHighlightBox} 用 {@code GL_COLOR_LOGIC_OP/GL_OR_REVERSE}
 *       （原版反色框），忽略传入颜色，故 {@code colHighlight} 无效。这里改为半透明柔和蓝填充，
 *       并上下各留 1px，避免 ModernUI 字体字形顶部未被框住。</li>
 * </ul>
 */
@Mixin(targets = "betterquesting.api.utils.RenderUtils")
public class MixinBqRenderUtils {

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true, remap = false)
    private static void modernui$adaptFontWidth(String text, FontRenderer font,
            CallbackInfoReturnable<Integer> cir) {
        if (font != null) {
            cir.setReturnValue(Integer.valueOf(font.getStringWidth(text == null ? "" : text)));
        }
    }

    @Inject(method = "drawHighlightBox(IIIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void modernui$blendHighlight(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        if (!UiConfig.selectionEnabled) {
            return;
        }
        // 整体按配置偏移对齐 ModernUI 替换后的字体字形（见 UiConfig.selectionOffsetX/Y，支持小数）。
        float left = Math.min(x1, x2) + UiConfig.selectionOffsetX;
        float right = Math.max(x1, x2) + UiConfig.selectionOffsetX;
        float top = Math.min(y1, y2) + UiConfig.selectionOffsetY;
        float bottom = Math.max(y1, y2) + UiConfig.selectionOffsetY;

        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_I(UiConfig.selectionColor, UiConfig.selectionAlpha);
        tessellator.addVertex((double) left, (double) bottom, 0.0D);
        tessellator.addVertex((double) right, (double) bottom, 0.0D);
        tessellator.addVertex((double) right, (double) top, 0.0D);
        tessellator.addVertex((double) left, (double) top, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        ci.cancel();
    }
}
