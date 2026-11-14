package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

/**
 * 26.3-style resource/save status box.
 *
 * <p>Drawn over the current frame so the world or world-selection screen stays
 * visible. This is used only for the short resource-preparation and save
 * phases; the chunk-status loading screen takes over once the world client is
 * being loaded.</p>
 */
public final class ModernLoadingBox {

    private ModernLoadingBox() {
    }

    /**
     * @param mc     client
     * @param width  scaled width
     * @param height scaled height
     * @param text   localised status text
     */
    public static void render(Minecraft mc, int width, int height, String text) {
        setupOrtho(width, height);
        try {
            int boxWidth = Math.max(240, Math.min(380, width / 3));
            int boxHeight = Math.max(60, mc.fontRenderer.FONT_HEIGHT + 34);
            int left = width / 2 - boxWidth / 2;
            int top = height / 2 - boxHeight / 2;
            int right = left + boxWidth;
            int bottom = top + boxHeight;

            ModernGuiRenderer.fill(left, top, right, bottom, 0xFF080808);
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
