package blocamlimb.nhx100218.modernui.enhance.tooltip;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;

import blocamlimb.nhx100218.modernui.enhance.ui.render.TooltipGradientRenderer;

/**
 * 原版悬浮提示（tooltip）替换渲染器。
 *
 * <p>被 {@code MixinGuiScreenTooltip} 在 {@code GuiScreen.drawHoveringText} HEAD 调用；返回 true
 * 表示已接管，调用方 cancel 原版实现。布局沿用 1.7.10 原版度量（保证与各宿主/模组兼容），
 * 背景替换为 ModernUI 风格：圆角 + 四角渐变 + 自适应颜色 + 标题分隔线/居中。</p>
 */
public final class TooltipRenderer {

    // 文本与边框的留白（原版 3/4，取较宽松但不拥挤的 5px）。
    private static final int H_BORDER = 5;
    private static final int V_BORDER = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int TITLE_GAP = 2;

    private static final Logger LOG = LogManager.getLogger("ModernUI/tooltip");
    private static boolean sLoggedActive;

    private TooltipRenderer() {}

    /** 总开关，供 mixin 在不组装文本行时提前跳过。 */
    public static boolean isEnabled() {
        return TooltipConfig.enabled;
    }

    /**
     * 仅绘制默认配色的圆角背景（黑底 + 浅色边框），供 ModularUI2 等“背景与文字分离”的宿主使用。
     */
    public static void drawSimpleBackground(double x, double y, int width, int height) {
        if (!TooltipConfig.enabled || width <= 0 || height <= 0) {
            return;
        }
        int fill = TooltipConfig.fillColor[0];
        int[] stroke = TooltipConfig.strokeColor;
        TooltipRoundedRect.draw(x, y, x + width, y + height, TooltipConfig.cornerRadius,
                Math.max(0.5F, TooltipConfig.borderWidth), fill, fill, fill, fill,
                stroke[0], stroke[1], stroke[2], stroke[3], 300.0);
    }

    public static boolean draw(List<String> textLines, int mouseX, int mouseY, FontRenderer font,
            int screenWidth, int screenHeight) {
        if (font == null || !TooltipConfig.enabled || textLines == null || textLines.isEmpty()) {
            return false;
        }

        int textWidth = 0;
        for (String line : textLines) {
            if (line != null) {
                textWidth = Math.max(textWidth, font.getStringWidth(line));
            }
        }
        int lineCount = textLines.size();
        int textHeight = 8 + (lineCount > 1 ? TITLE_GAP + (lineCount - 1) * LINE_HEIGHT : 0);

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + textWidth > screenWidth) {
            x -= 28 + textWidth;
        }
        if (y + textHeight + 6 > screenHeight) {
            y = screenHeight - textHeight - 6;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        if (!sLoggedActive) {
            sLoggedActive = true;
            LOG.info("[ModernUI-tooltip] active: lines={}, at=({},{}), size={}x{}", lineCount, x, y,
                    textWidth, textHeight);
        }

        boolean hasTitleGap = lineCount > 1;

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        try {
            int[] fill = TooltipColors.fill(textLines);
            int[] stroke = activeStroke(TooltipColors.stroke(textLines));

            float bw = Math.max(0.5F, TooltipConfig.borderWidth);
            double baseRadius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.0;

            double left = x - H_BORDER;
            double top = y - V_BORDER;
            double right = x + textWidth + H_BORDER;
            double bottom = y + textHeight + V_BORDER;

            TooltipRoundedRect.draw(left, top, right, bottom, baseRadius, bw,
                    fill[0], fill[1], fill[2], fill[3],
                    stroke[0], stroke[1], stroke[2], stroke[3], 300.0);
            if (hasTitleGap && TooltipConfig.titleBreak) {
                TooltipGradientRenderer.begin();
                try {
                    TooltipGradientRenderer.fillRect(x, y + 9, x + textWidth, y + 10, 0x60FFFFFF, 300.0);
                } finally {
                    TooltipGradientRenderer.end();
                }
            }

            int ty = y;
            for (int i = 0; i < lineCount; i++) {
                String line = textLines.get(i);
                if (line != null) {
                    int tx = x;
                    if (i == 0 && hasTitleGap && TooltipConfig.centerTitle) {
                        tx = x + (textWidth - font.getStringWidth(line)) / 2;
                    }
                    font.drawStringWithShadow(line, tx, ty, -1);
                }
                if (i == 0) {
                    ty += TITLE_GAP;
                }
                ty += LINE_HEIGHT;
            }
        } finally {
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
        return true;
    }

    /** ModernUI 式边框颜色循环：四角颜色随时间沿周向插值。 */
    private static int[] activeStroke(int[] work) {
        int cycle = TooltipConfig.borderColorCycle;
        if (cycle <= 0) {
            return work;
        }
        long now = System.currentTimeMillis();
        float p = (now % cycle) / (float) cycle;
        int pos = 3 - (int) ((now / cycle) & 3);
        int[] active = new int[4];
        for (int i = 0; i < 4; i++) {
            active[i] = lerpRgb(p, work[(i + pos) & 3], work[(i + pos + 3) & 3]);
        }
        return active;
    }

    private static int lerpRgb(float f, int a, int b) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            int v = Math.round(ca + (cb - ca) * f);
            out |= (v & 0xFF) << shift;
        }
        return out;
    }
}
