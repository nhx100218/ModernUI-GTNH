package blocamlimb.nhx100218.modernui.enhance.vanilla;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

/**
 * Chat-only open/close animation.
 *
 * <p>This intentionally does not use the generic {@link ScreenTransition}:
 * the chat screen has its own short slide and its own delayed close. Scrolling
 * is left to vanilla, so there is no per-scroll translation and the chat can
 * never "fly away".</p>
 */
public final class ChatScrollSmooth {

    private static final float OPEN_OFFSET = 12.0F;

    private static boolean sEnabled = true;
    private static boolean sPushed;
    private static GuiScreen sScreen;
    private static long sOpenStart;
    private static boolean sClosing;
    private static long sCloseStart;
    private static boolean sSuppressIntercept;

    private ChatScrollSmooth() {}

    /** Called at the head of {@code GuiChat.drawScreen}. */
    public static void pushChat() {
        sPushed = false;
        if (!animationOn()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc == null ? null : mc.currentScreen;
        if (!(screen instanceof GuiChat)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (screen != sScreen) {
            sScreen = screen;
            sOpenStart = now;
            sClosing = false;
        }
        float translateY = 0.0F;
        if (sClosing) {
            float progress = (now - sCloseStart) / (float) animationDuration();
            if (progress < 1.0F) {
                translateY = OPEN_OFFSET * easeInCubic(Math.max(0.0F, progress));
            }
        } else if (sOpenStart > 0L) {
            float progress = (now - sOpenStart) / (float) animationDuration();
            if (progress >= 1.0F) {
                sOpenStart = 0L;
            } else {
                translateY = OPEN_OFFSET * (1.0F - easeOutCubic(Math.max(0.0F, progress)));
            }
        }
        GL11.glPushMatrix();
        if (translateY != 0.0F) {
            GL11.glTranslatef(0.0F, translateY, 0.0F);
        }
        sPushed = true;
    }

    /** Called at the return of {@code GuiChat.drawScreen}. */
    public static void popChat() {
        if (!sPushed) {
            return;
        }
        sPushed = false;
        GL11.glPopMatrix();
    }

    /**
     * Delays {@code displayGuiScreen(null)} while the chat is open so the close
     * animation can play. Returns true when the original call must be cancelled.
     */
    public static boolean interceptChatClose(GuiScreen target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || target != null || sSuppressIntercept) {
            return false;
        }
        if (!animationOn() || animationDuration() <= 0
                || sClosing || !(mc.currentScreen instanceof GuiChat)) {
            return false;
        }
        sClosing = true;
        sCloseStart = System.currentTimeMillis();
        return true;
    }

    /** Called once per render tick to finish a delayed chat close. */
    public static void tick() {
        if (!sClosing) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - sCloseStart < animationDuration()) {
            return;
        }
        sClosing = false;
        sScreen = null;
        sOpenStart = 0L;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.currentScreen instanceof GuiChat) {
            sSuppressIntercept = true;
            try {
                mc.displayGuiScreen(null);
            } finally {
                sSuppressIntercept = false;
            }
        }
    }

    /** True while the chat open/close slide is still running. */
    public static boolean isAnimating() {
        if (sClosing) {
            return true;
        }
        return sOpenStart > 0L && System.currentTimeMillis() - sOpenStart < animationDuration();
    }
    /** Test/API switch; open and close animations are disabled when false. */
    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    private static boolean animationOn() {
        return sEnabled && SmoothScrollConfig.chatOpenSmoothness > 0;
    }

    private static int animationDuration() {
        return (int) (90.0F + Math.max(0, SmoothScrollConfig.chatOpenSmoothness) * 2.0F);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }
}
