package blocamlimb.nhx100218.modernui.enhance.vanilla;

import java.lang.reflect.Method;

import net.minecraft.util.MouseHelper;

/**
 * Applies zoom to lwjgl3ify's optional float mouse deltas without linking
 * against the lwjgl3ify API at class-load time.
 */
public final class Lwjgl3ifyMouseCompat {

    private static final String FLOAT_MOUSE_HELPER = "me.eigenraven.lwjgl3ify.api.FloatMouseHelper";

    private static boolean resolved;
    private static Class<?> helperType;
    private static Method getDx;
    private static Method setDx;
    private static Method getDy;
    private static Method setDy;

    private Lwjgl3ifyMouseCompat() {}

    public static void scale(MouseHelper helper, float zoom) {
        if (helper == null || zoom <= 1.0001F) {
            return;
        }
        if (!resolved) {
            resolve();
        }
        if (helperType == null || !helperType.isInstance(helper)) {
            return;
        }
        try {
            setDx.invoke(helper, Float.valueOf(((Float) getDx.invoke(helper)).floatValue() / zoom));
            setDy.invoke(helper, Float.valueOf(((Float) getDy.invoke(helper)).floatValue() / zoom));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            resolved = true;
            helperType = null;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> type = Class.forName(FLOAT_MOUSE_HELPER);
            helperType = type;
            getDx = type.getMethod("lwjgl3ify$getFloatDX");
            setDx = type.getMethod("lwjgl3ify$setFloatDX", Float.TYPE);
            getDy = type.getMethod("lwjgl3ify$getFloatDY");
            setDy = type.getMethod("lwjgl3ify$setFloatDY", Float.TYPE);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException absent) {
            helperType = null;
        }
    }
}
