package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;

/**
 * 26.3 command suggestion list.
 *
 * <p>Matches the vanilla 26.3 geometry: 12px rows, translucent black background,
 * selected row in yellow, other rows in grey, and a dashed black edge when the
 * list is scrolled.</p>
 */
public final class CommandCompletionRenderer {

    public static final int ROW_HEIGHT = 12;
    private static final int BACKGROUND = 0x80000000;
    private static final int SELECTED_TEXT = 0xFFFFFF00;
    private static final int NORMAL_TEXT = 0xFFAAAAAA;
    private static final int BORDER = 0xFF000000;

    private CommandCompletionRenderer() {
    }

    public static int preferredHeight(CommandCompletionModel model, int maxRows) {
        if (!model.isOpen()) {
            return 0;
        }
        int rows = Math.min(model.entries().size(), maxRows);
        return rows * ROW_HEIGHT + 2;
    }

    public static void render(Minecraft mc, CommandCompletionModel model, int left, int top, int width,
            int maxRows) {
        if (mc == null || !model.isOpen()) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        List<CommandCompletionEntry> entries = model.entries();
        int rows = Math.min(entries.size(), maxRows);
        int height = rows * ROW_HEIGHT + 2;
        ModernGuiRenderer.fill(left, top, left + width, top + height, BACKGROUND);

        int start = model.scrollOffset();
        boolean moreAbove = start > 0;
        boolean moreBelow = start + rows < entries.size();
        if (moreAbove) {
            dashedLine(left, top, width, BORDER);
        }
        if (moreBelow) {
            dashedLine(left, top + height - 1, width, BORDER);
        }

        int selected = model.selectedIndex();
        for (int i = 0; i < rows; i++) {
            int index = start + i;
            if (index >= entries.size()) {
                break;
            }
            int rowTop = top + 1 + i * ROW_HEIGHT;
            CommandCompletionEntry entry = entries.get(index);
            int color = index == selected ? SELECTED_TEXT : NORMAL_TEXT;
            font.drawStringWithShadow(entry.text, left + 1, rowTop + 2, color);
        }
    }

    private static void dashedLine(int left, int y, int width, int color) {
        for (int x = 0; x < width; x += 2) {
            ModernGuiRenderer.fill(left + x, y, left + Math.min(width, x + 1), y + 1, color);
        }
    }
}
