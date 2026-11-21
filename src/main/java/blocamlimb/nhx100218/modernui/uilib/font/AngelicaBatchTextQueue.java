package blocamlimb.nhx100218.modernui.uilib.font;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.uilib.MyMod;
import blocamlimb.nhx100218.modernui.uilib.font.api.DefaultFontRendererAdapter;
import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiFontStyle;
import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiFontWeight;
import blocamlimb.nhx100218.modernui.uilib.ui.text.TextContentMode;

/**
 * Collects Angelica's batched debug text and replays it with ModernUI's SDF font
 * after the host finished its geometry.
 *
 * <p>Used by the FPS/TPS graph labels. The F3 overlay itself keeps Angelica's
 * renderer (see {@code MixinAngelicaDebugTextColor}) to avoid cached-mesh
 * flicker.</p>
 */
public final class AngelicaBatchTextQueue {

    private static final List<Entry> ENTRIES = new ArrayList<Entry>();
    private static boolean diagLogged;

    private AngelicaBatchTextQueue() {
    }

    public static void begin() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    /** Whether the current draw is one this queue owns. */
    public static boolean shouldCapture() {
        return F3FontBridge.isDrawingFrame() || F3FontBridge.isScreenTextCapture();
    }

    public static void queue(String text, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        synchronized (ENTRIES) {
            if (ENTRIES.size() >= 4096) {
                return;
            }
            String plain = stripFormatting(text);
            if (plain.isEmpty()) {
                return;
            }
            // Debug text is always rendered pure white by ModernUI; the native
            // Angelica colours (0xFFE0E0E0 / 0xFFD0D0D0) and §7 codes look grey.
            ENTRIES.add(new Entry(plain, x, y, normalize(color), shadow));
        }
    }

    /** Removes Minecraft § formatting codes so they cannot tint the SDF text. */
    private static String stripFormatting(String text) {
        int length = text.length();
        StringBuilder out = null;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < length) {
                if (out == null) {
                    out = new StringBuilder(length);
                    out.append(text, 0, i);
                }
                i++;
                continue;
            }
            if (out != null) {
                out.append(c);
            }
        }
        return out == null ? text : out.toString();
    }

    /**
     * Angelica's debug text is often a light grey with a reduced alpha. ModernUI
     * renders that as washed-out grey; make debug text opaque and promote neutral
     * labels to pure white while keeping semantic colours.
     */
    private static int normalize(int color) {
        int argb = color == 0 ? 0xFFFFFFFF : (color | 0xFF000000);
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        if (red >= 0x80 && Math.abs(red - green) <= 12 && Math.abs(green - blue) <= 12) {
            return 0xFFFFFFFF;
        }
        return argb;
    }

    public static void flush() {
        List<Entry> snapshot;
        synchronized (ENTRIES) {
            if (ENTRIES.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<Entry>(ENTRIES);
            ENTRIES.clear();
        }
        DefaultFontRendererAdapter adapter = DefaultFontRendererAdapter.getInstance();
        // The host may leave depth test / alpha test enabled after its geometry.
        // SDF quads would then be depth-rejected and only the native shadow stays
        // visible, which reads as grey "missing" text.
        ModernGuiRenderer.beginUi();
        if (!diagLogged) {
            diagLogged = true;
            java.nio.FloatBuffer current = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, current);
            MyMod.LOG.info("[F3DIAG] flush entries={} first='{}' color={} glColor=({},{},{},{}) blend={} depth={} alphaTest={}",
                    Integer.valueOf(snapshot.size()), snapshot.get(0).text,
                    Integer.toHexString(snapshot.get(0).color),
                    Float.valueOf(current.get(0)), Float.valueOf(current.get(1)),
                    Float.valueOf(current.get(2)), Float.valueOf(current.get(3)),
                    Boolean.valueOf(GL11.glIsEnabled(GL11.GL_BLEND)),
                    Boolean.valueOf(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)),
                    Boolean.valueOf(GL11.glIsEnabled(GL11.GL_ALPHA_TEST)));
        }
        for (Entry entry : snapshot) {
            try {
                adapter.drawString(entry.text, entry.x, entry.y, entry.color, entry.shadow,
                        TextContentMode.UILIB_RAW, UiFontWeight.BOLD, UiFontStyle.NORMAL);
            } catch (Throwable ignored) {
                // Never let debug text break the host draw.
            }
        }
    }

    private static final class Entry {

        private final String text;
        private final int x;
        private final int y;
        private final int color;
        private final boolean shadow;

        private Entry(String text, int x, int y, int color, boolean shadow) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.shadow = shadow;
        }
    }
}
