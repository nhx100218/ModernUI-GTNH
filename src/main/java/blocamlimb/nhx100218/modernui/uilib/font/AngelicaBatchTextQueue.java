package blocamlimb.nhx100218.modernui.uilib.font;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import blocamlimb.nhx100218.modernui.uilib.font.api.DefaultFontRendererAdapter;

/**
 * Collects Angelica's batched debug text and replays it with ModernUI's SDF font
 * after the host finished its geometry.
 *
 * <p>Angelica's F3 options screen and FPS graph draw text through
 * {@code FontRenderer.angelica$drawStringBatched}. Drawing SDF text at that call
 * site would put it below the batch's panels, so the text is queued and flushed
 * at the end of the screen/overlay draw.</p>
 */
public final class AngelicaBatchTextQueue {

    private static final String DEBUG_OPTIONS_SCREEN =
            "com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen";
    private static final List<Entry> ENTRIES = new ArrayList<Entry>();

    private AngelicaBatchTextQueue() {
    }

    public static void begin() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    /** Whether the current draw is one this queue owns. */
    public static boolean shouldCapture() {
        if (F3FontBridge.isDrawingFrame()) {
            return true;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.currentScreen != null
                && DEBUG_OPTIONS_SCREEN.equals(minecraft.currentScreen.getClass().getName());
    }

    public static void queue(String text, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        synchronized (ENTRIES) {
            if (ENTRIES.size() >= 4096) {
                return;
            }
            ENTRIES.add(new Entry(text, x, y, color, shadow));
        }
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
