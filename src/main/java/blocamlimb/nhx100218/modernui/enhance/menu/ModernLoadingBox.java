package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

/**
 * Small 26.3-style status box used while resources are being prepared or a
 * world is being saved. It deliberately draws over the current frame instead of
 * replacing it with the full panorama/loading screen, so the dirt background
 * never flashes in front of the world.
 */
public final class ModernLoadingBox {

    private ModernLoadingBox() {
    }

    /**
     * Draws a centred black box with a white border and one line of text.
     *
     * @param mc   client
     * @param width  scaled screen width
     * @param height scaled screen height
     * @param text   already-localised message
     */
    public static void render(Minecraft mc, int width, int height, String text) {
        setupOrtho(width, height);
        try {
            int boxWidth = Math.max(220, Math.min(360, width / 3));
            int boxHeight = Math.max(56, mc.fontRenderer.FONT_HEIGHT + 30);
            int left = width / 2 - boxWidth / 2;
            int top = height / 2 - boxHeight / 2;
            int right = left + boxWidth;
            int bottom = top + boxHeight;

            // Black panel and the 2px white frame from the reference overlay.
            ModernGuiRenderer.fill(left, top, right, bottom, 0xF0000000);
            ModernGuiRenderer.fill(left, top, right, top + 2, 0xFFFFFFFF);
            ModernGuiRenderer.fill(left, bottom - 2, right, bottom, 0xFFFFFFFF);
            ModernGuiRenderer.fill(left, top, left + 2, bottom, 0xFFFFFFFF);
            ModernGuiRenderer.fill(right - 2, top, right, bottom, 0xFFFFFFFF);

            int textY = top + boxHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2;
            ModernGuiRenderer.centeredString(mc.fontRenderer, text, width / 2, textY, 0xFFFFFFFF);
        } finally {
            restoreOrtho();
        }
    }

    private static void setupOrtho(int width, int height) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        ModernGuiRenderer.beginUi();
    }

    private static void restoreOrtho() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
