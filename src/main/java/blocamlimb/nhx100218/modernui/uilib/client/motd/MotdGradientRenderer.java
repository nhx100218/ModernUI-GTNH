package blocamlimb.nhx100218.modernui.uilib.client.motd;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import blocamlimb.nhx100218.modernui.uilib.font.FontService;
import blocamlimb.nhx100218.modernui.uilib.util.AngelicaRendererCompat;

/**
 * Draws the server-list MOTD with per-glyph colours when the active renderer
 * supports it. On Angelica's SDL GPU backend (Vulkan/D3D12) the vanilla
 * MiniMOTD fallback is kept instead.
 */
public final class MotdGradientRenderer {

    private MotdGradientRenderer() {}

    public static boolean shouldReplace(String motd) {
        return MotdGradient.hasMarker(motd) && MotdGradient.hasVisibleText(motd) && canRenderGradient();
    }

    public static boolean canRenderGradient() {
        return !AngelicaRendererCompat.isSdlGpuActive() && FontService.isRenderRuntimeSupportedOnThisSide();
    }

    public static boolean draw(FontRenderer font, String motd, int x, int y, int maxWidth, int maxLines) {
        List<MotdGradient.Glyph> glyphs = MotdGradient.parse(motd);
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
