package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * 26.3 command suggestion list.
 *
 * <p>Matches vanilla 26.3 {@code CommandSuggestions.SuggestionsList}: 12px
 * rows, {@code 0xD0000000} background, selected row in yellow, other rows in
 * grey, and white dashed 1px edges while the list is scrolled. The panel is
 * drawn through the vanilla tessellator so Angelica renders it in-world.</p>
 */
public final class CommandCompletionRenderer {

    public static final int ROW_HEIGHT = 12;
    private static final int BACKGROUND = 0xD0000000;
    private static final int SELECTED_TEXT = 0xFFFFFF00;
    private static final int NORMAL_TEXT = 0xFFAAAAAA;
    private static final int DASH = 0xFFFFFFFF;

    private CommandCompletionRenderer() {
    }

    public static int preferredHeight(CommandCompletionModel model, int maxRows) {
        if (model == null || !model.isOpen()) {
            return 0;
        }
        int rows = Math.min(model.entries().size(), maxRows);
        return rows * ROW_HEIGHT;
    }

    public static void render(Minecraft mc, CommandCompletionModel model, int left, int top, int width,
            int maxRows) {
        if (mc == null || model == null || !model.isOpen() || width <= 0) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        List<CommandCompletionEntry> entries = model.entries();
        int rows = Math.min(entries.size(), maxRows);
        int height = rows * ROW_HEIGHT;
        int start = model.scrollOffset();
        boolean moreAbove = start > 0;
        boolean moreBelow = start + rows < entries.size();
        boolean scrolled = moreAbove || moreBelow;

        // 26.3 fills every row with the same colour and keeps a 1px strip above
        // and below the panel while it can scroll, so the dashes sit on the
        // panel colour instead of on the world.
        int panelTop = scrolled ? top - 1 : top;
        int panelBottom = scrolled ? top + height + 1 : top + height;
        Gui.drawRect(left, panelTop, left + width, panelBottom, BACKGROUND);
        if (moreAbove) {
            dashedLine(left, top - 1, width);
        }
        if (moreBelow) {
            dashedLine(left, top + height, width);
        }

        int selected = model.selectedIndex();
        for (int i = 0; i < rows; i++) {
            int index = start + i;
            if (index >= entries.size()) {
                break;
            }
            String text = displayText(entries.get(index));
            int color = index == selected ? SELECTED_TEXT : NORMAL_TEXT;
            font.drawStringWithShadow(text, left + 1, top + 2 + i * ROW_HEIGHT, color);
        }
    }

    /** Command candidates are shown without the leading slash, like 26.3. */
    public static String displayText(CommandCompletionEntry entry) {
        if (entry == null || entry.text == null) {
            return "";
        }
        String text = entry.text;
        return text.startsWith("/") ? text.substring(1) : text;
    }

    private static void dashedLine(int left, int y, int width) {
        float alpha = (DASH >>> 24 & 255) / 255.0F;
        float red = (DASH >> 16 & 255) / 255.0F;
        float green = (DASH >> 8 & 255) / 255.0F;
        float blue = (DASH & 255) / 255.0F;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(red, green, blue, alpha);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int x = 0; x < width; x += 2) {
            tessellator.addVertex(left + x, y + 1, 0.0D);
            tessellator.addVertex(left + x + 1, y + 1, 0.0D);
            tessellator.addVertex(left + x + 1, y, 0.0D);
            tessellator.addVertex(left + x, y, 0.0D);
        }
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
