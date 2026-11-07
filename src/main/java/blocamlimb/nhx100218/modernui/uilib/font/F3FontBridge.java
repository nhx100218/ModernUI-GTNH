package blocamlimb.nhx100218.modernui.uilib.font;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import blocamlimb.nhx100218.modernui.uilib.font.api.DefaultFontRendererAdapter;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;

/**
 * Angelica 新版 F3（DebugScreenOverlay 的批量字体路径）到 ModernUI 字体的桥。
 *
 * <p>Angelica 2.2.44 的 F3 不再调用 {@code FontRenderer.drawString}，而是通过
 * {@code DebugText.Batched -> BatchingFontRenderer} 直接烘焙字形网格。ModernUI 的
 * {@code MixinFontRenderer} 因此完全接管不到这段文字。</p>
 *
 * <p>桥接策略：在 {@code DebugText.Batched.draw} 采集阶段拦下文本，记录文本、坐标、
 * 颜色和当时的固定管线矩阵，不写入 Angelica 的字形网格（同一次 capture 里的面板
 * 矩形仍保留）；{@code DebugScreenOverlay.draw} 返回后，只对本帧实际显示的
 * chrome/分组网格补画 ModernUI 文本。这样分组布局、面板、图表全部保留，只替换字形。</p>
 *
 * <p>所有 Angelica 类型均以 {@link Object} 接收，桥本身不链接 Angelica 类；
 * Angelica 缺席时这些方法不会被调用。</p>
 */
public final class F3FontBridge {

    private static final Logger LOG = LogManager.getLogger("ModernUI/F3FontBridge");

    private static final Field DEBUG_TEXT_BATCHER = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugText$Batched", "batcher");
    private static final Field BATCHER_CAPTURING = fieldOf(
            "com.gtnewhorizons.angelica.client.font.BatchingFontRenderer", "capturing");

    private static final Field OVERLAY_PLACED = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "placed");
    private static final Field OVERLAY_GROUP_COUNT = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "groupCount");
    private static final Field OVERLAY_MESHES = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "meshes");
    private static final Field OVERLAY_GROUP_DIRTY = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "groupDirty");
    private static final Field OVERLAY_CHROME = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "chrome");
    private static final Field OVERLAY_CHROME_DIRTY = fieldOf(
            "com.gtnewhorizons.angelica.debug.screen.DebugScreenOverlay", "chromeDirty");

    private static final Map<Object, List<Entry>> RECORDED = Collections
            .synchronizedMap(new IdentityHashMap<Object, List<Entry>>());
    private static final ThreadLocal<FloatBuffer> MATRIX_SCRATCH = new ThreadLocal<FloatBuffer>() {
        @Override
        protected FloatBuffer initialValue() {
            return BufferUtils.createFloatBuffer(16);
        }
    };

    /** 当前正在采集的网格；变化时说明开始了新一轮捕获，需要清掉该网格旧文本。 */
    private static Object lastCapturing;
    private static boolean drawWarned;
    private static boolean measureWarned;
    private static boolean recordedLogged;
    private static boolean flushedLogged;

    private F3FontBridge() {
    }

    /** 是否由 ModernUI 接管 F3 文本（用户开启自定义字体替换时）。 */
    public static boolean shouldReplace() {
        return FontConfig.replaceOrigin;
    }

    /**
     * 在捕获阶段记录一次文本；返回 true 表示调用方应取消 Angelica 的原生字形写入。
     *
     * @param textAdapter {@code DebugText.Batched} 实例
     * @param text        完整文本
     * @param start       起始下标
     * @param end         结束下标
     * @param x           绘制 X
     * @param y           绘制 Y
     * @param color       ARGB 颜色
     * @return 是否已接管
     */
    public static boolean recordIfActive(Object textAdapter, CharSequence text, int start, int end,
            float x, float y, int color) {
        if (!shouldReplace() || textAdapter == null || text == null) {
            return false;
        }
        try {
            Object batcher = readField(DEBUG_TEXT_BATCHER, textAdapter);
            if (batcher == null) {
                return false;
            }
            Object mesh = readField(BATCHER_CAPTURING, batcher);
            if (mesh == null) {
                return false;
            }
            if (mesh != lastCapturing) {
                lastCapturing = mesh;
                RECORDED.remove(mesh);
            }
            int from = Math.max(0, Math.min(start, text.length()));
            int to = Math.max(from, Math.min(end, text.length()));
            String value = text.subSequence(from, to).toString();
            if (value.isEmpty()) {
                return false;
            }
            record(mesh, value, x, y, normalizeColor(color));
            if (!recordedLogged) {
                recordedLogged = true;
                LOG.info("F3 文本桥接已接管 Angelica 批量字形（sample=\"{}\"）", value);
            }
            return true;
        } catch (Throwable failure) {
            if (!drawWarned) {
                drawWarned = true;
                LOG.warn("F3 文本桥接记录失败，回退 Angelica 原生字形：{}", failure.toString());
            }
            return false;
        }
    }

    /** 文本宽度（ModernUI 度量）；失败返回 null 表示保留 Angelica 度量。 */
    public static Float tryMeasure(CharSequence text, int start, int end) {
        if (!shouldReplace() || text == null) {
            return null;
        }
        try {
            int from = Math.max(0, Math.min(start, text.length()));
            int to = Math.max(from, Math.min(end, text.length()));
            return Float.valueOf(DefaultFontRendererAdapter.getInstance()
                    .getStringWidth(text.subSequence(from, to).toString()));
        } catch (Throwable failure) {
            if (!measureWarned) {
                measureWarned = true;
                LOG.warn("F3 文本度量桥接失败，回退 Angelica 原生度量：{}", failure.toString());
            }
            return null;
        }
    }

    /** 单个字符宽度（ModernUI 度量）；失败返回 null。 */
    public static Float tryAdvance(char character) {
        if (!shouldReplace() || character == '\u00a7') {
            return null;
        }
        try {
            return Float.valueOf(DefaultFontRendererAdapter.getInstance()
                    .getStringWidth(String.valueOf(character)));
        } catch (Throwable failure) {
            if (!measureWarned) {
                measureWarned = true;
                LOG.warn("F3 文本度量桥接失败，回退 Angelica 原生度量：{}", failure.toString());
            }
            return null;
        }
    }

    /** 开始一帧 F3 绘制：下一轮 capture 必须重新清理对应网格。 */
    public static void beginFrame() {
        lastCapturing = null;
    }

    /**
     * F3 绘制结束：只对当前实际显示的 chrome / 分组网格补画 ModernUI 文本。
     *
     * @param overlay {@code DebugScreenOverlay} 实例；null 时只清理捕获状态
     */
    public static void flushFrame(Object overlay) {
        try {
            if (overlay == null) {
                return;
            }
            FloatBuffer matrix = MATRIX_SCRATCH.get();
            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            if (depthTest) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            try {
                int drawn = drawChrome(overlay, matrix) + drawGroups(overlay, matrix);
                if (drawn > 0 && !flushedLogged) {
                    flushedLogged = true;
                    LOG.info("F3 文本桥接已补画 ModernUI 文本：entries={}", Integer.valueOf(drawn));
                }
            } finally {
                if (depthTest) {
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                }
            }
        } catch (Throwable failure) {
            if (!drawWarned) {
                drawWarned = true;
                LOG.warn("F3 文本桥接绘制失败，后续尝试继续：{}", failure.toString());
            }
        } finally {
            lastCapturing = null;
        }
    }

    private static int drawChrome(Object overlay, FloatBuffer matrix) throws IllegalAccessException {
        Object chromeDirty = readField(OVERLAY_CHROME_DIRTY, overlay);
        if (chromeDirty instanceof Boolean && ((Boolean) chromeDirty).booleanValue()) {
            // 该帧正在重新捕获 chrome，面板本身未绘制，不能单独补文字。
            return 0;
        }
        return drawMesh(readField(OVERLAY_CHROME, overlay), matrix);
    }

    private static int drawGroups(Object overlay, FloatBuffer matrix) throws IllegalAccessException {
        Object placedValue = readField(OVERLAY_PLACED, overlay);
        Object meshesValue = readField(OVERLAY_MESHES, overlay);
        if (!(placedValue instanceof boolean[]) || !(meshesValue instanceof Object[])) {
            return 0;
        }
        boolean[] placed = (boolean[]) placedValue;
        Object[] meshes = (Object[]) meshesValue;
        Object dirtyValue = readField(OVERLAY_GROUP_DIRTY, overlay);
        boolean[] dirty = dirtyValue instanceof boolean[] ? (boolean[]) dirtyValue : null;
        int groupCount = readInt(OVERLAY_GROUP_COUNT, overlay);
        int count = Math.min(groupCount, Math.min(placed.length, meshes.length));
        int drawn = 0;
        for (int index = 0; index < count; index++) {
            if (!placed[index]) {
                continue;
            }
            if (dirty != null && index < dirty.length && dirty[index]) {
                // 该分组正在重新捕获，面板未绘制。
                continue;
            }
            drawn += drawMesh(meshes[index], matrix);
        }
        return drawn;
    }

    private static int drawMesh(Object mesh, FloatBuffer matrix) {
        if (mesh == null) {
            return 0;
        }
        List<Entry> entries = RECORDED.get(mesh);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        List<Entry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<Entry>(entries);
        }
        for (int index = 0; index < snapshot.size(); index++) {
            drawEntry(snapshot.get(index), matrix);
        }
        return snapshot.size();
    }

    /** 某个网格开始重新捕获：丢弃旧文本。 */
    public static void clear(Object mesh) {
        if (mesh != null) {
            RECORDED.remove(mesh);
        }
    }

    /** abortCapture 时清掉当前捕获网格的残留文本。 */
    public static void clearCurrentCapture(Object batcher) {
        if (batcher == null) {
            return;
        }
        try {
            clear(readField(BATCHER_CAPTURING, batcher));
        } catch (Throwable ignored) {
            // 清理失败不影响主流程；下一次 capture 会覆盖。
        }
    }

    private static void record(Object mesh, String text, float x, float y, int color) {
        Entry entry = new Entry(text, x, y, color);
        List<Entry> entries = RECORDED.get(mesh);
        if (entries == null) {
            entries = new ArrayList<Entry>();
            RECORDED.put(mesh, entries);
        }
        synchronized (entries) {
            entries.add(entry);
        }
    }

    private static void drawEntry(Entry entry, FloatBuffer matrix) {
        boolean pushed = false;
        try {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            pushed = true;
            loadMatrix(GL11.GL_PROJECTION, entry.projection, matrix);
            loadMatrix(GL11.GL_MODELVIEW, entry.modelview, matrix);
            DefaultFontRendererAdapter.getInstance()
                    .drawString(entry.text, (int) entry.x, (int) entry.y, entry.color, false);
        } catch (Throwable failure) {
            if (!drawWarned) {
                drawWarned = true;
                LOG.warn("F3 文本桥接绘制失败，后续尝试继续：{}", failure.toString());
            }
        } finally {
            if (pushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }
        }
    }

    private static void loadMatrix(int mode, float[] values, FloatBuffer buffer) {
        GL11.glMatrixMode(mode);
        buffer.clear();
        buffer.put(values, 0, 16);
        buffer.flip();
        GL11.glLoadMatrix(buffer);
    }

    private static int normalizeColor(int color) {
        int argb = color;
        if ((argb & 0xFF000000) == 0 && argb != 0) {
            argb |= 0xFF000000;
        }
        return argb;
    }

    private static int readInt(Field field, Object target) throws IllegalAccessException {
        return field == null || target == null ? 0 : field.getInt(target);
    }

    private static Object readField(Field field, Object target) throws IllegalAccessException {
        return field == null || target == null ? null : field.get(target);
    }

    private static Field fieldOf(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable absent) {
            return null;
        }
    }

    private static final class Entry {

        private final String text;
        private final float x;
        private final float y;
        private final int color;
        private final float[] modelview = new float[16];
        private final float[] projection = new float[16];

        private Entry(String text, float x, float y, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            FloatBuffer buffer = MATRIX_SCRATCH.get();
            buffer.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            buffer.position(0);
            buffer.get(modelview);
            buffer.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
            buffer.position(0);
            buffer.get(projection);
        }
    }
}
