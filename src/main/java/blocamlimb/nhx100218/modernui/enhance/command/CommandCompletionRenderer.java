package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;

/**
 * 26.3-style completion list renderer: dark rounded-looking panel, highlighted
 * selected row and a dim description column.
 *
 * <p>Prepared for the future chat hook; callers supply the anchor and width.</p>
 */
public final class CommandCompletionRenderer {

    private static final int ROW_HEIGHT = 14;
    private static final int PADDING = 3;

    private CommandCompletionRenderer() {
    }

    public static int preferredHeight(CommandCompletionModel model, int maxRows) {
        if (!model.isOpen()) {
            return 0;
        }
        int rows = Math.min(model.entries().size(), maxRows);
        return rows * ROW_HEIGHT + PADDING * 2;
    }

    public static void render(Minecraft mc, CommandCompletionModel model, int left, int top, int width,
            int maxRows) {
        if (mc == null || !model.isOpen()) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        List<CommandCompletionEntry> entries = model.entries();
        int rows = Math.min(entries.size(), maxRows);
        int height = rows * ROW_HEIGHT + PADDING * 2;
        ModernGuiRenderer.fill(left, top, left + width, top + height, 0xE6101014);
        ModernGuiRenderer.fill(left, top, left + width, top + 1, 0x33FFFFFF);
        ModernGuiRenderer.fill(left, top + height - 1, left + width, top + height, 0x33FFFFFF);

        int selected = model.selectedIndex();
        for (int i = 0; i < rows; i++) {
            int rowTop = top + PADDING + i * ROW_HEIGHT;
            if (i == selected) {
                ModernGuiRenderer.fill(left + 1, rowTop, left + width - 1, rowTop + ROW_HEIGHT,
                        0x553F51B5);
            }
            CommandCompletionEntry entry = entries.get(i);
            font.drawStringWithShadow(entry.text, left + 5, rowTop + 3, 0xFFFFFFFF);
            if (!entry.description.isEmpty()) {
                String description = entry.description;
                int max = width - 12 - font.getStringWidth(entry.text) - 8;
                if (max > 12 && font.getStringWidth(description) > max) {
                    description = font.trimStringToWidth(description, max - font.getStringWidth("...")) + "...";
                }
                if (max > 0) {
                    font.drawStringWithShadow(description, left + width - font.getStringWidth(description) - 5,
                            rowTop + 3, 0xFF9E9E9E);
                }
            }
        }
    }
}
