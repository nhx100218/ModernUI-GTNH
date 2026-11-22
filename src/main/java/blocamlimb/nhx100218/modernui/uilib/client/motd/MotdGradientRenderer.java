package blocamlimb.nhx100218.modernui.uilib.client.motd;

import java.util.List;

import com.google.gson.JsonArray;

import net.minecraft.client.gui.FontRenderer;

import blocamlimb.nhx100218.modernui.uilib.font.FontService;

/**
 * Draws the server-list MOTD with per-glyph colours when the font runtime is
 * available. The font pipeline keeps per-call colours on both the GL and SDL
 * GPU backends, so the gradient is no longer limited to OpenGL.
 */
public final class MotdGradientRenderer {

    private MotdGradientRenderer() {}

    public static boolean shouldReplace(String motd) {
        return MotdGradient.hasMarker(motd) && MotdGradient.hasVisibleText(motd) && canRenderGradient();
    }

    public static boolean shouldReplace(String motd, JsonArray exactColors) {
        return (exactColors != null && exactColors.size() > 0
                ? MotdGradient.hasMarker(motd)
                : MotdGradient.hasMarker(motd) && MotdGradient.hasVisibleText(motd))
                && canRenderGradient();
    }

    public static boolean canRenderGradient() {
        return FontService.isRenderRuntimeSupportedOnThisSide();
    }

    public static boolean draw(FontRenderer font, String motd, int x, int y, int maxWidth, int maxLines) {
        return draw(font, motd, null, x, y, maxWidth, maxLines);
    }

    public static boolean draw(FontRenderer font, String motd, JsonArray exactColors,
            int x, int y, int maxWidth, int maxLines) {
        List<MotdGradient.Glyph> glyphs = MotdGradient.parse(motd, exactColors);
        if (glyphs.isEmpty()) {
            return false;
        }
        int cursor = x;
        int lineY = y;
        int line = 0;
        boolean drew = false;
        for (MotdGradient.Glyph glyph : glyphs) {
            if (glyph.codePoint == '\n') {
                if (++line >= maxLines) {
                    break;
                }
                cursor = x;
                lineY += font.FONT_HEIGHT;
                continue;
            }
            String character = new String(Character.toChars(glyph.codePoint));
            StringBuilder styled = new StringBuilder(character.length() + 6);
            if (glyph.bold) {
                styled.append('\u00a7').append('l');
            }
            if (glyph.italic) {
                styled.append('\u00a7').append('o');
            }
            if (glyph.underlined) {
                styled.append('\u00a7').append('n');
            }
            styled.append(character);
            int width = font.getStringWidth(styled.toString());
            if (cursor + width > x + maxWidth && cursor > x) {
                if (++line >= maxLines) {
                    break;
                }
                cursor = x;
                lineY += font.FONT_HEIGHT;
            }
            // drawString returns the absolute end-x, not the glyph advance.
            font.drawString(styled.toString(), cursor, lineY, 0xFF000000 | glyph.rgb, false);
            cursor += width;
            drew = true;
        }
        return drew;
    }
}
