package blocamlimb.nhx100218.modernui.enhance.vanilla;

import org.lwjgl.input.Keyboard;

/** 缩放运行态：按键 → 目标倍率 → 逐帧插值。 */
public final class ZoomState {

    private static float sCurrent = 1.0F;

    private ZoomState() {}

    /** 当前缩放倍率（不含副作用）。 */
    public static float current() {
        return sCurrent;
    }

    /** 缩放键此刻是否按住。 */
    public static boolean isActive() {
        return ZoomConfig.enabled && Keyboard.isKeyDown(ZoomConfig.keyCode);
    }

    /** 每帧调用，返回当前缩放倍率（>=1）。 */
    public static float update() {
        boolean down = ZoomConfig.enabled && Keyboard.isKeyDown(ZoomConfig.keyCode);
        float target = down ? Math.max(1.0F, ZoomConfig.factor) : 1.0F;
        if (ZoomConfig.smooth) {
            sCurrent += (target - sCurrent) * clamp(ZoomConfig.speed, 0.02F, 1.0F);
        } else {
            sCurrent = target;
        }
        if (Math.abs(sCurrent - target) < 0.001F) {
            sCurrent = target;
        }
        return sCurrent;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
