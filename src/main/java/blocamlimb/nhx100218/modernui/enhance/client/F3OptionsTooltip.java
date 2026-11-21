package blocamlimb.nhx100218.modernui.enhance.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.uilib.font.api.DefaultFontRendererAdapter;
import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiFontStyle;
import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiFontWeight;
import blocamlimb.nhx100218.modernui.uilib.ui.text.TextContentMode;

/**
 * ModernUI replacement for the debug-options tooltip.
 *
 * <p>The screen's native tooltip is cancelled when it tries to draw, and this
 * helper redraws the same Angelica colour block with SDF text after the screen.
 * The text was still using Angelica's bitmap path because tooltip drawing
 * happens in the second batched layer.</p>
 */
public final class F3OptionsTooltip {

    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER = 0x505000FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int OFFSET = 12;

    private static List<String> lines;
    private static int mouseX;
    private static int mouseY;

    private F3OptionsTooltip() {
    }

    public static void capture(List<String> tooltipLines, int x, int y) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            lines = null;
            return;
        }
        lines = new ArrayList<String>(tooltipLines);
        mouseX = x;
        mouseY = y;
    }

    public static void draw(Minecraft minecraft) {
        List<String> tooltip = lines;
        lines = null;
        if (tooltip == null || tooltip.isEmpty() || minecraft == null) {
            return;
        }
        DefaultFontRendererAdapter adapter = DefaultFontRendererAdapter.getInstance();
        int width = 0;
        for (String line : tooltip) {
            width = Math.max(width, adapter.getStringWidth(strip(line)));
        }
        int height = tooltip.size() > 1 ? 10 + (tooltip.size() - 1) * 10 : 8;
        int x = mouseX + OFFSET;
        int y = mouseY - OFFSET;
        int screenWidth = minecraft.currentScreen == null
                ? minecraft.displayWidth : minecraft.currentScreen.width;
        int screenHeight = minecraft.currentScreen == null
                ? minecraft.displayHeight : minecraft.currentScreen.height;
        if (x + width > screenWidth) {
            x -= 28 + width;
        }
        x = Math.max(4, x);
        if (y + height + 6 > screenHeight) {
            y = screenHeight - height - 6;
        }
        y = Math.max(4, y);

        ModernGuiRenderer.beginUi();
        ModernGuiRenderer.fill(x - 3, y - 4, x + width + 3, y - 3, BACKGROUND);
        ModernGuiRenderer.fill(x - 3, y + height + 3, x + width + 3, y + height + 4, BACKGROUND);
        ModernGuiRenderer.fill(x - 3, y - 3, x + width + 3, y + height + 3, BACKGROUND);
        ModernGuiRenderer.fill(x - 4, y - 3, x - 3, y + height + 3, BACKGROUND);
        ModernGuiRenderer.fill(x + width + 3, y - 3, x + width + 4, y + height + 3, BACKGROUND);
        ModernGuiRenderer.fill(x - 3, y - 2, x - 2, y + height + 2, BORDER);
        ModernGuiRenderer.fill(x + width + 2, y - 2, x + width + 3, y + height + 2, BORDER);
        ModernGuiRenderer.fill(x - 3, y - 3, x + width + 3, y - 2, BORDER);
        ModernGuiRenderer.fill(x - 3, y + height + 2, x + width + 3, y + height + 3, BORDER);
        ModernGuiRenderer.beginUi();

        int textY = y;
        for (int i = 0; i < tooltip.size(); i++) {
            adapter.drawString(strip(tooltip.get(i)), x, textY, TEXT, false,
                    TextContentMode.UILIB_RAW, UiFontWeight.NORMAL, UiFontStyle.NORMAL);
            textY += i == 0 ? 12 : 10;
        }
    }

    private static String strip(String text) {
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                if (out == null) {
                    out = new StringBuilder(text.length());
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
}
