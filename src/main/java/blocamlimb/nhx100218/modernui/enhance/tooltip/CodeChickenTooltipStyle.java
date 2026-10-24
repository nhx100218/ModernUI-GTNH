package blocamlimb.nhx100218.modernui.enhance.tooltip;

/**
 * CodeChickenLib {@code GuiDraw.drawTooltipBox} 的圆角化实现。
 *
 * <p>NEI 的物品/配方面板、NEI 配置页，以及其它使用 CodeChickenLib 绘制 tooltip 的模组
 * （任务书、匠魂炉等若走该路径）最终都会调用 {@code GuiDraw.drawTooltipBox} 绘制背景；
 * 本类把方角背景替换为圆角，文字仍由原方法后续绘制。</p>
 */
public final class CodeChickenTooltipStyle {

    private CodeChickenTooltipStyle() {}

    public static boolean draw(int x, int y, int width, int height, int borderStart, int borderEnd,
            int bgStart, int bgEnd) {
        if (!TooltipConfig.enabled || width <= 0 || height <= 0) {
            return false;
        }
        // 忽略 CodeChicken/NEI 传进来的紫蓝配色，统一为物品栏 tooltip 的观感：
        // 黑色半透明背景 + 浅色（近白）边框。
        int fill = TooltipConfig.fillColor[0];
        int[] stroke = TooltipConfig.strokeColor;
        TooltipRoundedRect.draw(x, y, x + width, y + height, TooltipConfig.cornerRadius,
                Math.max(0.5F, TooltipConfig.borderWidth),
                fill, fill, fill, fill,
                stroke[0], stroke[1], stroke[2], stroke[3], 300.0);
        return true;
    }
}
