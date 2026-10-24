package blocamlimb.nhx100218.modernui.enhance.tooltip;

/**
 * Waila HUD 框的圆角化渲染。
 *
 * <p>Waila 的 {@code OverlayRenderer.drawTooltipBox(x,y,w,h,border,gradTop,gradBottom)} 原本用
 * 多个矩形拼出方角框；这里改画一个圆角矩形，颜色沿用 Waila 的边框色与纵向渐变填充。</p>
 */
public final class WailaTooltipStyle {

    /**
     * Waila 外边框固定为不透明深灰。RC2 起 Waila 传入的 {@code bgcolor} 在运行期变成白色
     * （配置读取/默认值变动），若沿用会变白；这里直接固定为原本的深灰，保证观感稳定。
     */
    private static final int WAILA_BORDER = 0xFF303030;

    private WailaTooltipStyle() {}

    public static boolean draw(int x, int y, int width, int height, int border, int gradTop,
            int gradBottom) {
        if (!TooltipConfig.enabled || !TooltipConfig.waila || width <= 0 || height <= 0) {
            return false;
        }
        TooltipRoundedRect.draw(x, y, x + width, y + height, TooltipConfig.wailaCornerRadius,
                Math.max(0.5F, TooltipConfig.wailaBorderWidth),
                gradTop, gradTop, gradBottom, gradBottom,
                WAILA_BORDER, WAILA_BORDER, WAILA_BORDER, WAILA_BORDER, 300.0);
        return true;
    }
}
