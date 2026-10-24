package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * BogoSorter「快捷栏竖向滚动」高亮条（{@code HotbarSwap.verticalIndex}，0..3）的平滑缓动。
 *
 * <p>BogoSorter 的该功能改变 {@code verticalIndex} 并自绘一条高亮条，行与行之间是硬跳变。
 * 这里给 {@code verticalIndex} 加与快捷栏一致的缓动，使高亮条平滑滑动。</p>
 */
public final class HotbarColumnSmooth {

    /** BogoSorter 每行高亮的像素间距。 */
    public static final int ROW_PX = 18;

    private static final SmoothScrollModel MODEL = new SmoothScrollModel();
    private static boolean init;

    private HotbarColumnSmooth() {}

    /** 当前显示用的 {@code verticalIndex}（浮点，可落在两行之间）。 */
    public static float displayed() {
        return (float) MODEL.current();
    }

    /** 是否正在缓动（未收敛）。 */
    public static boolean active() {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.hotbarSmoothness > 0 && init && !MODEL.settled();
    }

    /** 每帧推进，{@code current} 为目标 {@code verticalIndex}。 */
    public static void update(int current) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.hotbarSmoothness == 0) {
            MODEL.snap(current);
            init = true;
            return;
        }
        if (!init) {
            MODEL.snap(current);
            init = true;
            return;
        }
        MODEL.setTarget(current);
        MODEL.update(SmoothScrollConfig.smoothness(SmoothScrollConfig.hotbarSmoothness));
        if (MODEL.settled()) {
            MODEL.snap(current);
        }
    }
}
