package blocamlimb.nhx100218.modernui.uilib.font;

import java.util.ArrayList;
import java.util.List;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.uilib.font.api.DefaultFontRendererAdapter;

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

    private AngelicaBatchTextQueue() {
    }

    public static void begin() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    /** Whether the current draw is one this queue owns (FPS/TPS graph labels). */
    public static boolean shouldCapture() {
        return F3FontBridge.isDrawingFrame();
    }

    public static void queue(String text, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        synchronized (ENTRIES) {
            if (ENTRIES.size() >= 4096) {
                return;
            }
            ENTRIES.add(new Entry(text, x, y, normalize(color), shadow));
        }
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
        for (Entry entry : snapshot) {
            try {
                adapter.drawString(entry.text, entry.x, entry.y, entry.color, entry.shadow);
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
