package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * Original chat opening animation kept as-is:
 * the chat content quickly slides up from below with a non-linear ease-out.
 *
 * <p>Scrolling is deliberately left to vanilla. There is no scroll translation
 * here, so scrolling cannot replay the opening animation or move the chat
 * away from its box.</p>
 */
public final class ChatScrollSmooth {

    private static final float LINE = 9.0F;
    private static final float OPEN_OFFSET = 8.0F;

    private static boolean sWasOpen;
    private static long sOpenStart;

    private static boolean sMsgActive;
    private static long sMsgStart;

    private ChatScrollSmooth() {}

    /** Called once per chat draw; returns the vertical pixel offset. */
    public static float update(int target, boolean open) {
        float result = newMessageOffset();
        if (!open || !SmoothScrollConfig.enabled) {
            sWasOpen = false;
            return result;
        }
        result += updateOpening();
        return result;
    }

    public static boolean isAnimating() {
        return sMsgActive || (sWasOpen && sOpenStart > 0L);
    }

    /** A new message arrived; slide it in once. */
    public static void onNewMessage() {
        if (!SmoothScrollConfig.enabled) {
            return;
        }
        sMsgActive = true;
        sMsgStart = System.currentTimeMillis();
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
        // ease-out cubic: fast slide-in at the start, then settle.
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
