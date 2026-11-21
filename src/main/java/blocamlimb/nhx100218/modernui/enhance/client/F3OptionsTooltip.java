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

    private static final int BACKGROUND = 0xE6101014;
    private static final int BORDER = 0x33FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int OFFSET = 12;

    private static List<String> lines;
    private static int mouseX;
    private static int mouseY;
    private static boolean LOGGED_CAPTURE;
    private static boolean LOGGED_DRAW;

    private F3OptionsTooltip() {
    }

    public static void capture(List<String> tooltipLines, int x, int y) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            lines = null;
            return;
        }
        if (!LOGGED_CAPTURE) {
            LOGGED_CAPTURE = true;
            blocamlimb.nhx100218.modernui.uilib.MyMod.LOG.info(
                    "[F3DIAG] tooltip capture lines={} first='{}'", Integer.valueOf(tooltipLines.size()),
                    tooltipLines.get(0));
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
        if (!LOGGED_DRAW) {
            LOGGED_DRAW = true;
            blocamlimb.nhx100218.modernui.uilib.MyMod.LOG.info(
                    "[F3DIAG] tooltip draw lines={} first='{}'", Integer.valueOf(tooltip.size()), tooltip.get(0));
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
        // ModernUI tooltip panel: dark translucent body with a 1px light border.
        ModernGuiRenderer.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
        ModernGuiRenderer.fill(x, y, x + width, y + height, BACKGROUND);
        ModernGuiRenderer.beginUi();

        int textY = y;
        for (int i = 0; i < tooltip.size(); i++) {
            adapter.drawString(strip(tooltip.get(i)), x, textY, TEXT, false,
                    TextContentMode.UILIB_RAW, UiFontWeight.BOLD, UiFontStyle.NORMAL);
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
