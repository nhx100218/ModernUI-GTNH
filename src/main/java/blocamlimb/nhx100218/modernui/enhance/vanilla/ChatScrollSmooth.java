package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * Smooth scrolling for the vanilla chat window.
 *
 * <p>The displayed scroll position lags behind the real scroll position by at
 * most one line. Large jumps (fast wheel input) snap instead of animating so
 * the whole chat cannot be translated far away from its box.</p>
 */
public final class ChatScrollSmooth {

    private static final float LINE = 9.0F;
    private static final float OPEN_OFFSET = 8.0F;
    /** Maximum visual lag in pixels; fast scrolling must not fling the chat. */
    private static final float MAX_OFFSET_PX = LINE * 1.5F;
    /** A target jump larger than this many lines is treated as a snap. */
    private static final double MAX_SMOOTH_LINES = 4.0D;

    private static final SmoothScrollModel MODEL = new SmoothScrollModel();
    private static boolean sEngaged;

    private static boolean sWasOpen;
    private static long sOpenStart;

    private static boolean sMsgActive;
    private static long sMsgStart;

    private ChatScrollSmooth() {}

    /** Called once per chat frame; returns the vertical pixel offset. */
    public static float update(int target, boolean open) {
        float result = newMessageOffset();
        if (!open || !SmoothScrollConfig.enabled) {
            MODEL.snap(target);
            sEngaged = false;
            sWasOpen = false;
            return result;
        }
        result += updateScroll(target);
        // Do not replay the open animation while the user is actively scrolling.
        if (!sEngaged) {
            result += updateOpening();
        }
        return result;
    }

    public static boolean isAnimating() {
        return (sEngaged && !MODEL.settled()) || sMsgActive || (sWasOpen && sOpenStart > 0L);
    }

    /** A new message arrived; slide it in once. */
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
        // Fast wheel input jumps several lines at once. Snapping here avoids a
        // huge translation of the whole chat and the "flying chat" artifact.
        if (Math.abs(target - MODEL.current()) > MAX_SMOOTH_LINES) {
            MODEL.snap(target);
            sEngaged = false;
            return 0.0F;
        }
        sEngaged = true;
        MODEL.setTarget(target);
        double displayed = MODEL.update(SmoothScrollConfig.smoothness(SmoothScrollConfig.chatSmoothness));
        double offset = (displayed - target) * LINE;
        if (offset > MAX_OFFSET_PX) {
            offset = MAX_OFFSET_PX;
        } else if (offset < -MAX_OFFSET_PX) {
            offset = -MAX_OFFSET_PX;
        }
        return (float) offset;
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
