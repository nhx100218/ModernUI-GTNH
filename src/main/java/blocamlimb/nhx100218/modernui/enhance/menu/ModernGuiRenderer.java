package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * Small direct-GL drawing kit for the menu and loading screens.
 *
 * <p>These screens run while ModernUI's scene runtime is not mounted. The helpers
 * therefore use immediate-mode quads directly and leave the caller's matrix and
 * attribute state alone. Do not route these calls through Tessellator wrappers:
 * the Angelica SDL-GPU backend does not replay them reliably.</p>
 */
public final class ModernGuiRenderer {

    private ModernGuiRenderer() {
    }

    /** Enters the 2D UI state used by this package. */
    public static void beginUi() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Fills an ARGB rectangle with direct quads. */
    public static void fill(int left, int top, int right, int bottom, int argb) {
        if (right <= left || bottom <= top) {
            return;
        }
        float alpha = (argb >>> 24 & 255) / 255.0F;
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    /** Fills a vertical two-colour gradient. */
    public static void fillVerticalGradient(int left, int top, int right, int bottom, int topArgb, int bottomArgb) {
        if (right <= left || bottom <= top) {
            return;
        }
        float ta = (topArgb >>> 24 & 255) / 255.0F;
        float tr = (topArgb >> 16 & 255) / 255.0F;
        float tg = (topArgb >> 8 & 255) / 255.0F;
        float tb = (topArgb & 255) / 255.0F;
        float ba = (bottomArgb >>> 24 & 255) / 255.0F;
        float br = (bottomArgb >> 16 & 255) / 255.0F;
        float bg = (bottomArgb >> 8 & 255) / 255.0F;
        float bb = (bottomArgb & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(br, bg, bb, ba);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glColor4f(tr, tg, tb, ta);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    /** Draws a texture rectangle with normalised texture coordinates. */
    public static void textureRect(ResourceLocation texture, float left, float top, float right, float bottom,
            float u0, float v0, float u1, float v1, int argb) {
        float alpha = (argb >>> 24 & 255) / 255.0F;
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v1);
        GL11.glVertex2f(left, bottom);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex2f(right, bottom);
        GL11.glTexCoord2f(u1, v0);
        GL11.glVertex2f(right, top);
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    /**
     * Draws one square sprite from a sheet.
     *
     * @param sheet      sprite sheet
     * @param left       destination left
     * @param top        destination top
     * @param size       destination size
     * @param u          sprite left in sheet pixels
     * @param v          sprite top in sheet pixels
     * @param sheetSize  sheet width/height in pixels
     * @param argb       tint
     */
    public static void sprite(ResourceLocation sheet, float left, float top, float size, float u, float v,
            float sheetSize, int argb) {
        if (size <= 0.0F || sheetSize <= 0.0F) {
            return;
        }
        textureRect(sheet, left, top, left + size, top + size, u / sheetSize, v / sheetSize,
                (u + size) / sheetSize, (v + size) / sheetSize, argb);
    }

    /** Draws a centered string with shadow. */
    public static void centeredString(FontRenderer font, String text, int centerX, int y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, argb);
    }

    /** Draws a centered string scaled around its center. */
    public static void centeredStringScaled(FontRenderer font, String text, float centerX, float centerY,
            float scale, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, -font.FONT_HEIGHT / 2, argb);
        GL11.glPopMatrix();
    }

    /** White with the supplied alpha, used for translucent tints. */
    public static int white(int alpha) {
        return (alpha & 255) << 24 | 0xFFFFFF;
    }

    /** ARGB without alpha manipulation. */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
    }
}
