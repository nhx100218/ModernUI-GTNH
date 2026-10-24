package blocamlimb.nhx100218.modernui.enhance.vanilla;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * Screen open transition for vanilla screens.
 *
 * <p>The chat screen is intentionally excluded: the chat has its own
 * {@link ChatScrollSmooth} animation, and the generic screen slide must not be
 * mixed into it.</p>
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

    private static float displacement(Class<?> cls, long now) {
        if (!enabled || cls == null || shouldSkip(cls)) {
            return 0.0F;
        }
        float progress = (now - sChangedAt) / (float) Math.max(1, durationMs);
        if (progress >= 1.0F) {
            return 0.0F;
        }
        progress = Math.max(0.0F, progress);
        float eased = easeBack(1.0F - progress);
        return offset * scale * eased * (slideDown ? 1.0F : -1.0F);
    }

    /** The chat owns its own animation and must not receive the screen slide. */
    private static boolean shouldSkip(Class<?> cls) {
        return cls.getName().contains("GuiChat");
    }

    private static float easeBack(float x) {
        return (float) (2.70158 * x * x * x - 1.70158 * x * x);
    }
}
