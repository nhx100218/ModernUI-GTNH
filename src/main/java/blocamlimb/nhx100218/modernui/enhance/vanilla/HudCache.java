package blocamlimb.nhx100218.modernui.enhance.vanilla;

import java.lang.reflect.Field;

import blocamlimb.nhx100218.modernui.enhance.MyMod;

/**
 * Angelica HUD 缓存桥：GTNH 下 HUD（快捷栏/聊天等）会被
 * {@code com.gtnewhorizons.angelica.hudcaching.HUDCaching} 缓存到离屏帧缓冲，
 * 因此我们在逐帧绘制里做的位移/缓动只有在缓存“脏”时才会被重建，表现为动画不可见。
 *
 * <p>本类把该缓存的 {@code dirty} 置位、并把 {@code nextHudRefresh} 归零，强制下一帧重建。
 * 全部走反射，Angelica 不存在时静默降级。首次解析结果会打日志便于排查。</p>
 */
public final class HudCache {

    private static boolean resolved;
    private static Field dirtyField;
    private static Field refreshField;
    private static boolean loggedUnavailable;

    private HudCache() {}

    public static boolean available() {
        resolve();
        return dirtyField != null;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> type = Class.forName("com.gtnewhorizons.angelica.hudcaching.HUDCaching");
            dirtyField = type.getDeclaredField("dirty");
            dirtyField.setAccessible(true);
            try {
                refreshField = type.getDeclaredField("nextHudRefresh");
                refreshField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                refreshField = null;
            }
            MyMod.LOG.info("[HudCache] Angelica HUD 缓存已接管（dirty/nextHudRefresh）");
        } catch (Throwable failure) {
            dirtyField = null;
            refreshField = null;
            if (!loggedUnavailable) {
                loggedUnavailable = true;
                MyMod.LOG.info("[HudCache] 未发现 Angelica HUD 缓存，跳过（{}）", failure.toString());
            }
        }
    }

    /** 标记 HUD 缓存为脏（下一帧重建）。非 Angelica 环境下无副作用。 */
    public static void markDirty() {
        resolve();
        if (dirtyField == null) {
            return;
        }
        try {
            dirtyField.setBoolean(null, true);
            if (refreshField != null) {
                refreshField.setLong(null, 0L);
            }
        } catch (Throwable ignored) {
            // 防御性
        }
    }
}
