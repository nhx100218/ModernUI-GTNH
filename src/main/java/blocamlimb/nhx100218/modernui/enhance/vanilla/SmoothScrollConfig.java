package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * 平滑滚动配置（对齐 Minecraft-Smooth-Scrolling 的分面口径）。
 *
 * <p>每个面有两个参数：{@code smoothness}（0..100，0=关闭缓动，越大越慢；100=不再滚动）
 * 与 {@code amount}（每次滚轮的像素量，0=自动）。字段由配置页回灌。</p>
 */
public final class SmoothScrollConfig {

    private SmoothScrollConfig() {}

    /** 总开关。 */
    public static volatile boolean enabled = true;

    /** 原版可滚动列表（{@code GuiSlot}）：缓动强度 0..100。 */
    public static volatile int listSmoothness = 50;
    /** 原版可滚动列表每次滚轮像素量，0=自动（半行）。 */
    public static volatile int listAmount = 0;

    /** 聊天栏：缓动强度 0..100。 */
    public static volatile int chatSmoothness = 50;
    /** 聊天栏每次滚轮像素量，0=自动。 */
    public static volatile int chatAmount = 0;
    /** 聊天栏打开时的滑入平滑 0..100（0=关闭，越大越慢）。 */
    public static volatile int chatOpenSmoothness = 50;

    /** 创造模式物品栏：缓动强度 0..100。 */
    public static volatile int creativeSmoothness = 50;
    /** 创造模式物品栏每次滚轮像素量。 */
    public static volatile int creativeAmount = 30;

    /** 文本框：缓动强度 0..100。 */
    public static volatile int textSmoothness = 50;
    /** 文本框每次滚轮像素量。 */
    public static volatile int textAmount = 100;

    /** 快捷栏：缓动强度 0..100。 */
    public static volatile int hotbarSmoothness = 20;
    /** 快捷栏跨过首尾时是否环绕。 */
    public static volatile boolean hotbarRollover = true;
    /** 选择框固定在中间、快捷栏整体滚动。 */
    public static volatile boolean hotbarStaticSelector = false;

    /** 0..100 的百分比转 0..1 保留比例。 */
    public static double smoothness(int percent) {
        int clamped = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
        return clamped / 100.0D;
    }
}
