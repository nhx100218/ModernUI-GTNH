package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * 指数平滑滚动模型（对齐 Minecraft-Smooth-Scrolling 的核心公式）：
 *
 * <pre>pos = (pos - target) * smoothness<sup>frameTicks</sup> + target</pre>
 *
 * <p>{@code smoothness} 为 0..1 的每 tick 保留比例：0 = 立即到位（关闭），越接近 1 越慢。
 * 用真实帧时长折算成 tick 数，保证不同帧率下手感一致。每个滚动面各持有一个实例。</p>
 */
public final class SmoothScrollModel {

    /** 到位判定阈值（px）。 */
    private static final double SETTLE_EPSILON = 0.01D;

    private double pos;
    private double target;
    private boolean initialized;
    private long lastNanos;

    /** 设定目标滚动位置。 */
    public void setTarget(double value) {
        target = value;
    }

    public double target() {
        return target;
    }

    public double current() {
        return pos;
    }

    /** 直接吸附到给定值（初始化 / 拖拽 / 触控式输入时用）。 */
    public void snap(double value) {
        target = value;
        pos = value;
        initialized = true;
        lastNanos = System.nanoTime();
    }

    public boolean settled() {
        return initialized && Math.abs(pos - target) < SETTLE_EPSILON;
    }

    /**
     * 推进一帧并返回当前显示位置。
     *
     * @param smoothness 0..1 的保留比例（0 = 立即，1 = 不滚动）
     */
    public double update(double smoothness) {
        long now = System.nanoTime();
        if (!initialized) {
            pos = target;
            initialized = true;
            lastNanos = now;
            return pos;
        }
        if (smoothness <= 0.0D) {
            pos = target;
            lastNanos = now;
            return pos;
        }
        if (smoothness >= 1.0D) {
            lastNanos = now;
            return pos;
        }
        double ticks = frameTicks(now);
        pos = (pos - target) * Math.pow(smoothness, ticks) + target;
        if (Math.abs(pos - target) < SETTLE_EPSILON) {
            pos = target;
        }
        return pos;
    }

    /** 帧时长折算为 tick 数（20 ticks/秒），并对异常长的间隔做保护。 */
    private double frameTicks(long now) {
        double seconds = (now - lastNanos) / 1.0E9D;
        lastNanos = now;
        if (seconds <= 0.0D || seconds > 0.25D) {
            seconds = 1.0D / 60.0D;
        }
        return seconds * 20.0D;
    }
}
