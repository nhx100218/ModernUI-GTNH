package blocamlimb.nhx100218.modernui.enhance.vanilla;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

/**
 * Screen open transition (and chat close transition) for vanilla screens.
 *
 * <p>Every visible screen is wrapped by a small offset/scale animation. For the
 * chat screen a short close delay keeps the screen alive long enough to slide
 * out instead of disappearing instantly.</p>
 */
public final class ScreenTransition {

    public static volatile boolean enabled = true;
    public static volatile int durationMs = 220;
    public static volatile float offset = 9.0F;
    public static volatile float scale = 1.0F;
    public static volatile boolean slideDown = true;

    private static long sChangedAt;
    private static GuiScreen sScreen;
    private static int sDepth;

    private static boolean sChatClosing;
    private static long sChatCloseStart;
    private static boolean sSuppressIntercept;

    private ScreenTransition() {}

    public static void push() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc == null ? null : mc.currentScreen;
        long now = System.currentTimeMillis();
        if (screen != sScreen) {
            sScreen = screen;
            sChangedAt = now;
        }
        float displacement = displacement(screen == null ? null : screen.getClass(), now);
        GL11.glPushMatrix();
        if (displacement != 0.0F) {
            GL11.glTranslatef(0.0F, -displacement, 0.0F);
        }
        sDepth++;
    }

    public static void poke() {
        sChangedAt = System.currentTimeMillis();
    }

    public static void pop() {
        if (sDepth <= 0) {
            return;
        }
        sDepth--;
        GL11.glPopMatrix();
    }

    /**
     * Intercepts {@code Minecraft.displayGuiScreen(null)} while the chat screen
     * is open, so the chat can play its close animation first.
     *
     * @return true when the caller must cancel the original display call
     */
    public static boolean interceptGuiDisplay(GuiScreen target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || sSuppressIntercept) {
            return false;
        }
        if (target != null) {
            // Opening another screen while the chat is closing wins immediately.
            sChatClosing = false;
            return false;
        }
        if (!enabled || sChatClosing || !(mc.currentScreen instanceof GuiChat)) {
            return false;
        }
        sChatClosing = true;
        sChatCloseStart = System.currentTimeMillis();
        sScreen = mc.currentScreen;
        sChangedAt = sChatCloseStart;
        return true;
    }

    /** Called once per render tick; finishes a delayed chat close. */
    public static void tick() {
        if (!sChatClosing) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - sChatCloseStart < closeDuration()) {
            return;
        }
        sChatClosing = false;
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

    private static float displacement(Class<?> cls, long now) {
        if (!enabled || cls == null) {
            return 0.0F;
        }
        if (sChatClosing) {
            float progress = (now - sChatCloseStart) / (float) closeDuration();
            if (progress >= 1.0F) {
                return 0.0F;
            }
            progress = Math.max(0.0F, progress);
            // push() translates by -displacement: negative displacement slides down.
            return -offset * scale * easeInCubic(progress) * (slideDown ? 1.0F : -1.0F);
        }
        float progress = (now - sChangedAt) / (float) Math.max(1, durationMs);
        if (progress >= 1.0F) {
            return 0.0F;
        }
        progress = Math.max(0.0F, progress);
        float eased = easeBack(1.0F - progress);
        return offset * scale * eased * (slideDown ? 1.0F : -1.0F);
    }

    private static int closeDuration() {
        return Math.max(80, durationMs);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeBack(float x) {
        return (float) (2.70158 * x * x * x - 1.70158 * x * x);
    }
}
