package blocamlimb.nhx100218.modernui.enhance.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRenderer;

/**
 * ModernUI replacement for the debug-options tooltip.
 *
 * <p>Instead of drawing a custom rectangle, this reuses the shared ModernUI
 * tooltip renderer: it already provides the SDF-antialiased rounded corners,
 * gradient border, adaptive colours and SDF text.</p>
 */
public final class F3OptionsTooltip {

    private static List<String> lines;
    private static int mouseX;
    private static int mouseY;

    private F3OptionsTooltip() {
    }

    public static void capture(List<String> tooltipLines, int x, int y) {
        if (tooltipLines == null || tooltipLines.isEmpty() || !TooltipRenderer.isEnabled()) {
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
        if (tooltip == null || tooltip.isEmpty() || minecraft == null || minecraft.currentScreen == null) {
            return;
        }
        TooltipRenderer.draw(tooltip, mouseX, mouseY, minecraft.fontRenderer,
                minecraft.currentScreen.width, minecraft.currentScreen.height);
    }
}
