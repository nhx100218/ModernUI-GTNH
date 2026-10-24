package blocamlimb.nhx100218.modernui.enhance.vanilla;

/** 快捷栏选中框的平滑缓动（显示槽位向实际槽位做指数缓动）。 */
public final class HotbarSmoothScroll {

    /** 快捷栏槽位数。 */
    public static final int SLOTS = 9;

    private static final SmoothScrollModel MODEL = new SmoothScrollModel();
    private static boolean sInit;
    private static int sLastLogged = Integer.MIN_VALUE;
    private static int sRollover;

    /** 是否正处在 {@code GuiIngameForge.renderHotbar} 调用栈内（供 callee 侧识别选中框绘制）。 */
    public static volatile boolean inRenderHotbar;
    /** 本次 renderHotbar 内的 drawTexturedModalRect 调用序号。 */
    public static volatile int drawCounter;

    private HotbarSmoothScroll() {}

    public static float displayed() {
        return (float) MODEL.current();
    }

    /** 动画是否尚未收敛（用于驱动 HUD 缓存重建）。 */
    public static boolean isAnimating(int current) {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.hotbarSmoothness > 0 && sInit
                && !MODEL.settled();
    }

    /** 每帧推进并返回当前显示槽位（浮点）。 */
    public static float update(int current) {
        if (current != sLastLogged) {
            sLastLogged = current;
            blocamlimb.nhx100218.modernui.enhance.MyMod.LOG.info("[HotbarSmooth] update current={} displayed={}",
                    Integer.valueOf(current), Float.valueOf((float) MODEL.current()));
        }
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.hotbarSmoothness == 0) {
            MODEL.snap(current);
            sInit = true;
            return current;
        }
        if (!sInit) {
            MODEL.snap(current);
            sInit = true;
            return current;
        }
        // 目标 = 真实槽位 + 环绕圈数 * 9。环绕圈数只在滚轮跨首尾时改变（键盘直接改槽位不受影响）。
        MODEL.setTarget((double) current + SLOTS * sRollover);
        double value = MODEL.update(SmoothScrollConfig.smoothness(SmoothScrollConfig.hotbarSmoothness));
        if (MODEL.settled()) {
            // 收敛后回落到真实槽位，避免残留在 ±9 的等价位置导致对边多画一份。
            MODEL.snap(current);
            sRollover = 0;
            value = current;
        }
        return (float) value;
    }

    /** 滚轮改变槽位后调用：仅当跨过首尾时调整环绕圈数，使滑动继续同向。键盘改槽位不走这里。 */
    public static void onWheel(int oldSlot, int newSlot, int direction) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.hotbarSmoothness == 0
                || !SmoothScrollConfig.hotbarRollover) {
            return;
        }
        // changeCurrentItem: direction>0 使 currentItem 减小（向左），direction<0 向右。
        if (direction > 0) {
            if (newSlot > oldSlot) {
                // 左跨首尾（0→8）：继续向左
                sRollover--;
            }
        } else if (direction < 0) {
            if (newSlot < oldSlot) {
                // 右跨首尾（8→0）：继续向右
                sRollover++;
            }
        }
    }
}
