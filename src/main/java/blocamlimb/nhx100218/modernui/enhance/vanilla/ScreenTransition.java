package blocamlimb.nhx100218.modernui.enhance.vanilla;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * 界面切换动画（以 SmoothGui 为蓝本）：切换界面时对当前界面整体做一段缓动的纵向位移，
 * 让打开/返回更顺滑。列表类、加载/聊天等界面跳过（与 SmoothGui 的屏蔽口径一致）。
 */
public final class ScreenTransition {

    public static volatile boolean enabled = true;
    public static volatile int durationMs = 220;
    public static volatile float offset = 9.0F;
    public static volatile float scale = 1.0F;
    public static volatile boolean slideDown = true;

    private static long sChangedAt;
    // 按“界面实例”而非“类”判定变化：重复打开同一容器（新实例）也应播放动画。
    private static GuiScreen sScreen;
    // 可嵌套：外层包 currentScreen.drawScreen，内层可再包 ModularScreen.drawScreen。
    private static int sDepth;

    private ScreenTransition() {}

    /** 压矩阵并施加当前动画位移；与 {@link #pop()} 成对。 */
    public static void push() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc == null ? null : mc.currentScreen;
        long now = System.currentTimeMillis();
        if (screen != sScreen) {
            sScreen = screen;
            sChangedAt = now;
        }
        Class<?> cls = screen == null ? null : screen.getClass();
        float displacement = displacement(cls, now);
        GL11.glPushMatrix();
        if (displacement != 0.0F) {
            GL11.glTranslatef(0.0F, -displacement, 0.0F);
        }
        sDepth++;
    }

    /** 外部触发重播：用于同实例切换内部场景（如任务书）时复位动画计时。 */
    public static void poke() {
        sChangedAt = System.currentTimeMillis();
    }

    /** 弹矩阵。 */
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
        float progress = (now - sChangedAt) / (float) durationMs;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        if (progress < 0.0F) {
            progress = 0.0F;
        }
        // SmoothGui：位移 = offset * scale * easeBack(1 - progress) * 方向
        float eased = easeBack(1.0F - progress);
        return offset * scale * eased * (slideDown ? 1.0F : -1.0F);
    }

    private static boolean shouldSkip(Class<?> cls) {
        String name = cls.getName();
        return name.contains("GuiChat") || name.contains("GuiSelectWorld") || name.contains("GuiMultiplayer")
                || name.contains("GuiResourcePack") || name.contains("GuiControls") || name.contains("GuiKeyBinding")
                || name.contains("GuiOptions") || name.contains("GuiIngame") || name.contains("GuiScreenOptions");
    }

    private static float easeBack(float x) {
        return (float) (2.70158 * x * x * x - 1.70158 * x * x);
    }
}
