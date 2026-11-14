package blocamlimb.nhx100218.modernui.enhance.menu;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reflection bridge to Et Futurum Requiem's loading state.
 *
 * <p>ModernUI must run with and without EFR. This class keeps every EFR type out
 * of method signatures and constant pools, so the shared loading renderer links
 * on a vanilla client.</p>
 */
public final class EfrCompat {

    private static final Logger LOG = LogManager.getLogger("ModernUI/Menu/EfrCompat");

    private static final String TRACKER = "ganymedes01.etfuturum.client.loading.LoadingScreenStateTracker";
    private static final String SPAWN_PROGRESS = "ganymedes01.etfuturum.client.SpawnChunkProgress";

    private static volatile boolean resolved;
    private static volatile Class<?> trackerClass;
    private static volatile Method isActive;
    private static volatile Method snapshot;
    private static volatile Method getTitle;
    private static volatile Method getSubtitle;
    private static volatile Method getProgress;
    private static volatile Method spawnProgress;
    private static volatile Method chunkProgress;

    private EfrCompat() {
    }

    /** Whether EFR's loading classes are on the runtime classpath. */
    public static boolean isPresent() {
        resolve();
        return trackerClass != null;
    }

    /** Whether EFR is currently in an active loading session. */
    public static boolean isActive() {
        resolve();
        if (trackerClass == null || isActive == null) {
            return false;
        }
        try {
            Object value = isActive.invoke(null);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable failure) {
            LOG.debug("读取 EFR loading active 失败", failure);
            return false;
        }
    }

    /** Snapshot of EFR's loading state, or {@code null} when unavailable. */
    public static Snapshot snapshot() {
        resolve();
        if (trackerClass == null || snapshot == null) {
            return null;
        }
        try {
            Object value = snapshot.invoke(null);
            if (value == null) {
                return null;
            }
            String title = stringOf(getTitle, value);
            String subtitle = stringOf(getSubtitle, value);
            float progress = floatOf(getProgress, value, 0.0F);
            return new Snapshot(title, subtitle, progress);
        } catch (Throwable failure) {
            LOG.debug("读取 EFR loading snapshot 失败", failure);
            return null;
        }
    }

    /** EFR spawn-chunk progress in [0,1], or -1 when unavailable. */
    public static float spawnProgress() {
        resolve();
        if (spawnProgress == null) {
            return -1.0F;
        }
        try {
            Object value = spawnProgress.invoke(null);
            return value instanceof Number ? ((Number) value).floatValue() : -1.0F;
        } catch (Throwable failure) {
            return -1.0F;
        }
    }

    /** EFR client chunk-map progress in [0,1], or -1 when unavailable. */
    public static float chunkProgress() {
        resolve();
        if (chunkProgress == null) {
            return -1.0F;
        }
        try {
            Object value = chunkProgress.invoke(null);
            return value instanceof Number ? ((Number) value).floatValue() : -1.0F;
        } catch (Throwable failure) {
            return -1.0F;
        }
    }

    private static String stringOf(Method method, Object target) {
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(target);
            return value == null ? null : value.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float floatOf(Method method, Object target, float fallback) {
        if (method == null) {
            return fallback;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Number ? ((Number) value).floatValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        synchronized (EfrCompat.class) {
            if (resolved) {
                return;
            }
            try {
                ClassLoader loader = EfrCompat.class.getClassLoader();
                Class<?> tracker = classOrNull(TRACKER, loader);
                if (tracker != null) {
                    trackerClass = tracker;
                    isActive = methodOrNull(tracker, "isActive");
                    snapshot = methodOrNull(tracker, "snapshot");
                    Class<?> snapshotType = snapshot == null ? null : snapshot.getReturnType();
                    if (snapshotType != null) {
                        getTitle = methodOrNull(snapshotType, "getTitle");
                        getSubtitle = methodOrNull(snapshotType, "getSubtitle");
                        getProgress = methodOrNull(snapshotType, "getProgress");
                    }
                }
                Class<?> spawn = classOrNull(SPAWN_PROGRESS, loader);
                if (spawn != null) {
                    spawnProgress = methodOrNull(spawn, "getProgress");
                }
                Class<?> chunk = classOrNull("ganymedes01.etfuturum.client.ChunkLoadingProgress", loader);
                if (chunk != null) {
                    chunkProgress = methodOrNull(chunk, "getRawProgress");
                }
            } catch (Throwable failure) {
                LOG.debug("解析 EFR loading 兼容层失败", failure);
            } finally {
                resolved = true;
            }
        }
    }

    private static Class<?> classOrNull(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            Method method = owner.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Immutable EFR loading snapshot. */
    public static final class Snapshot {

        public final String title;
        public final String subtitle;
        public final float progress;

        Snapshot(String title, String subtitle, float progress) {
            this.title = title;
            this.subtitle = subtitle;
            this.progress = progress;
        }
    }
}
