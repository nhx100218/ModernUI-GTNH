package blocamlimb.nhx100218.modernui.enhance.vanilla;

/**
 * Smooth scrolling for the vanilla chat window.
 *
 * <p>The displayed scroll position lags behind the real scroll position by at
 * most 1.5 lines. Very large jumps snap instead of animating so the whole chat
 * cannot be translated far away from its box.</p>
 *
 * <p>Open/close motion is handled by {@link ScreenTransition} on the vanilla
 * chat screen; this class only handles scrolling and new-message slide-in.</p>
 */
public final class ChatScrollSmooth {

    private static final float LINE = 9.0F;
    /** Maximum visual lag in pixels; fast scrolling must not fling the chat. */
    private static final float MAX_OFFSET_PX = LINE * 1.5F;
    /** A target jump larger than this many lines is treated as a snap. */
    private static final double MAX_SMOOTH_LINES = 8.0D;

    private static final SmoothScrollModel MODEL = new SmoothScrollModel();
    private static boolean sEngaged;

    private static boolean sMsgActive;
    private static long sMsgStart;

    private ChatScrollSmooth() {}

    /** Called once per chat frame; returns the vertical pixel offset. */
    public static float update(int target, boolean open) {
        float result = newMessageOffset();
        if (!open || !SmoothScrollConfig.enabled) {
            MODEL.snap(target);
            sEngaged = false;
            return result;
        }
        result += updateScroll(target);
        return result;
    }

    public static boolean isAnimating() {
        return (sEngaged && !MODEL.settled()) || sMsgActive;
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

    private static float msgDuration() {
        return 120.0F + SmoothScrollConfig.chatSmoothness * 2.0F;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }
}
