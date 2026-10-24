package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * 聊天栏平滑：
 * <ul>
 *   <li><b>滚动</b>：显示行位置向实际滚动位置做指数缓动，返回整体内容的 y 残差（px）。</li>
 *   <li><b>打开</b>：聊天栏快速从下方滑入（非线性 ease-out）。</li>
 *   <li><b>新消息</b>：新消息到达时整栏一起上滚一行（而非硬切），返回临时下移量。</li>
 * </ul>
 */
public final class ChatScrollSmooth {

    private static final float LINE = 9.0F;
    private static final float OPEN_OFFSET = 8.0F;

    private static final SmoothScrollModel MODEL = new SmoothScrollModel();
    private static boolean sEngaged;
    private static int sLastLogged = Integer.MIN_VALUE;

    private static boolean sWasOpen;
    private static long sOpenStart;

    private static boolean sMsgActive;
    private static long sMsgStart;

    private ChatScrollSmooth() {}

    /** 每帧调用：返回聊天内容应整体的 y 偏移（px）。 */
    public static float update(int target, boolean open) {
        if (open && target != sLastLogged) {
            sLastLogged = target;
            blocamlimb.nhx100218.modernui.enhance.MyMod.LOG.info("[ChatSmooth] update target={} open={}",
                    Integer.valueOf(target), Boolean.valueOf(open));
        }
        float result = newMessageOffset();
        if (!open || !SmoothScrollConfig.enabled) {
            MODEL.snap(target);
            sEngaged = false;
            sWasOpen = false;
            return result;
        }
        result += updateScroll(target);
        result += updateOpening();
        return result;
    }

    public static boolean isAnimating() {
        return (sEngaged && !MODEL.settled()) || sMsgActive || (sWasOpen && sOpenStart > 0L);
    }

    /** 新消息到达：触发整栏上滚脉冲。 */
    public static void onNewMessage() {
        if (!SmoothScrollConfig.enabled) {
            return;
        }
        sMsgActive = true;
        sMsgStart = System.currentTimeMillis();
    }

    private static float updateScroll(int target) {
        if (SmoothScrollConfig.chatSmoothness == 0) {
            MODEL.snap(target);
            sEngaged = false;
            return 0.0F;
        }
        sEngaged = true;
        MODEL.setTarget(target);
        double displayed = MODEL.update(SmoothScrollConfig.smoothness(SmoothScrollConfig.chatSmoothness));
        return (float) ((displayed - target) * LINE);
    }

    private static float updateOpening() {
        if (SmoothScrollConfig.chatOpenSmoothness == 0) {
            sWasOpen = false;
            return 0.0F;
        }
        long now = System.currentTimeMillis();
        if (!sWasOpen) {
            sWasOpen = true;
            sOpenStart = now;
        }
        if (sOpenStart <= 0L) {
            return 0.0F;
        }
        float progress = (now - sOpenStart) / openDuration();
        if (progress >= 1.0F) {
            sOpenStart = 0L;
            return 0.0F;
        }
        if (progress < 0.0F) {
            progress = 0.0F;
        }
        // ease-out cubic：一开始就快速滑入。
        return OPEN_OFFSET * (1.0F - easeOutCubic(progress));
    }

    private static float newMessageOffset() {
        if (!sMsgActive || !SmoothScrollConfig.enabled || SmoothScrollConfig.chatSmoothness == 0) {
            sMsgActive = false;
            return 0.0F;
        }
        float progress = (System.currentTimeMillis() - sMsgStart) / msgDuration();
        if (progress >= 1.0F) {
            sMsgActive = false;
            return 0.0F;
        }
        if (progress < 0.0F) {
            progress = 0.0F;
        }
        return LINE * (1.0F - easeOutCubic(progress));
    }

    private static float openDuration() {
        return 90.0F + SmoothScrollConfig.chatOpenSmoothness * 2.0F;
    }

    private static float msgDuration() {
        return 120.0F + SmoothScrollConfig.chatSmoothness * 2.0F;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }
}
