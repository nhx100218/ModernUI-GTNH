package blocamlimb.nhx100218.modernui.enhance.vanilla;

/** 窗口失焦时的降帧 / 降音量配置。 */
public final class UnfocusedConfig {

    public static volatile boolean framerateEnabled = true;
    public static volatile int framerateLimit = 30;
    public static volatile boolean volumeEnabled = false;
    public static volatile float volumeFactor = 0.5F;

    /** 窗口是否失去焦点（无窗口/异常时视为聚焦，避免误触发）。 */
    public static boolean isUnfocused() {
        try {
            return !org.lwjgl.opengl.Display.isActive();
        } catch (Throwable t) {
            return false;
        }
    }

    private UnfocusedConfig() {}
}
