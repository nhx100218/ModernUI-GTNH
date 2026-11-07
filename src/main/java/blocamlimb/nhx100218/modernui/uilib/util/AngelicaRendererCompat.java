package blocamlimb.nhx100218.modernui.uilib.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Detects Angelica's active render backend without linking to Angelica classes.
 */
public final class AngelicaRendererCompat {

    private static final String BACKEND_MANAGER = "com.gtnewhorizons.angelica.glsm.backend.BackendManager";

    private AngelicaRendererCompat() {}

    /** @return true when Angelica's SDL GPU backend is active. */
    public static boolean isSdlGpuActive() {
        String name = backendName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("sdl");
    }

    /** @return true when the SDL GPU backend is using Direct3D 12. */
    public static boolean isD3D12Active() {
        String name = backendName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("direct3d12");
    }

    private static String backendName() {
        try {
            Class<?> manager = Class.forName(BACKEND_MANAGER, false,
                    AngelicaRendererCompat.class.getClassLoader());
            Field backendField = manager.getField("RENDER_BACKEND");
            Object backend = backendField.get(null);
            if (backend == null) {
                return null;
            }
            Method getName = backend.getClass().getMethod("getName");
            Object name = getName.invoke(backend);
            return name == null ? null : name.toString();
        } catch (ReflectiveOperationException | LinkageError | SecurityException absent) {
            return null;
        }
    }
}
