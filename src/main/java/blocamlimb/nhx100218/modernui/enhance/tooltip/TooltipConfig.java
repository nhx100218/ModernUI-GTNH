package blocamlimb.nhx100218.modernui.enhance.tooltip;

/**
 * 原版物品/悬浮提示（tooltip）替换样式配置。
 *
 * <p>对齐 ModernUI 的"new tooltip"：圆角边框 + 四角渐变 + 自适应颜色 + 标题分隔线/居中 +
 * 边框颜色循环。字段为 public static volatile，由配置页回灌（当前提供默认值）。</p>
 */
public final class TooltipConfig {

    private TooltipConfig() {}

    /** 总开关：false 时退还原版 tooltip。 */
    public static volatile boolean enabled = true;

    /** 是否使用圆角。 */
    public static volatile boolean rounded = true;

    /** 是否将标题行（第一行）居中。 */
    public static volatile boolean centerTitle = true;

    /** 是否在标题行下绘制分隔线（多行时）。 */
    public static volatile boolean titleBreak = true;

    /** 是否根据物品稀有度/名称颜色自适应边框颜色。 */
    public static volatile boolean adaptiveColors = true;

    /** ModularUI2（格雷等）tooltip 的最大折行宽度；<=0 表示自动（屏幕宽-20）。 */
    public static volatile int modularUiMaxWidth = 0;

    /** 是否禁用 ModularUI2 文本自动折行（影响所有 ModularUI 文本，默认开启以消除意外换行）。 */
    public static volatile boolean modularUiNoWrap = true;

    /** 是否给 Waila（手持/瞄准方块信息）的 HUD 框加圆角。 */
    public static volatile boolean waila = true;

    /** Waila HUD 框圆角半径。 */
    public static volatile float wailaCornerRadius = 4.0F;

    /** Waila HUD 框边框宽度。 */
    public static volatile float wailaBorderWidth = 1.0F;

    /** 边框颜色循环动画周期（ms）；<=0 关闭。 */
    public static volatile int borderColorCycle = 1000;

    /** 边框宽度（logical px）。ModernUI 默认 4/3。 */
    public static volatile float borderWidth = 4.0F / 3.0F;

    /** 圆角半径（logical px）。ModernUI 默认 4。 */
    public static volatile float cornerRadius = 4.0F;

    /** 背景四角颜色 ARGB，顺序 [左上, 右上, 右下, 左下]。ModernUI 默认 #E6000000。 */
    public static final int[] fillColor = { 0xE6000000, 0xE6000000, 0xE6000000, 0xE6000000 };

    /**
     * 边框四角颜色 ARGB，顺序 [左上, 右上, 右下, 左下]。无自适应色时使用（NEI/GT/匠魂炉等）。
     */
    public static final int[] strokeColor = { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
}
