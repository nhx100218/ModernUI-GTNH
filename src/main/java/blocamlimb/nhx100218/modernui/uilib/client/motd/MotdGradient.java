package blocamlimb.nhx100218.modernui.uilib.client.motd;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;

import blocamlimb.nhx100218.modernui.server.LegacyMotdText;

/**
 * Client-side reconstruction of the smooth MOTD gradient from the legacy
 * colour runs sent by the server.
 *
 * <p>The server sends the MiniMOTD fallback (nearest legacy colours) plus an
 * invisible marker. This parser extracts the per-character colours, then
 * interpolates between consecutive colour runs so ModernUI can draw a smooth
 * gradient with the font renderer. Vanilla clients keep the fallback.</p>
 */
public final class MotdGradient {

    private static final int DEFAULT_RGB = 0x808080;

    private MotdGradient() {}

    public static boolean hasMarker(String motd) {
        return motd != null && motd.startsWith(LegacyMotdText.MARKER);
    }

    public static List<Glyph> parse(String motd) {
        List<Glyph> glyphs = new ArrayList<Glyph>();
        if (motd == null) {
            return glyphs;
        }
        String text = hasMarker(motd) ? motd.substring(LegacyMotdText.MARKER.length()) : motd;
        String exactPayload = null;
        int payloadStart = text.indexOf(LegacyMotdText.EXACT_MARKER);
        if (payloadStart >= 0) {
            exactPayload = text.substring(payloadStart + LegacyMotdText.EXACT_MARKER.length());
            text = text.substring(0, payloadStart);
        }
        int rgb = DEFAULT_RGB;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean exactColors = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '\u00a7' || c == '&') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'x') {
                    int hex = parseHexColor(text, i);
                    if (hex >= 0) {
                        rgb = hex;
                        exactColors = true;
                        i += 14;
                        continue;
                    }
                }
                int color = legacyColor(code);
                if (color >= 0) {
                    rgb = color;
                } else if (code == 'r') {
                    rgb = DEFAULT_RGB;
                    bold = false;
                    italic = false;
                    underlined = false;
                } else if (code == 'l') {
                    bold = true;
                } else if (code == 'o') {
                    italic = true;
                } else if (code == 'n') {
                    underlined = true;
                } else if (code == 'm' || code == 'k') {
                    // strike/obfuscated are not reproduced per glyph.
                }
                i += 2;
                continue;
            }
            int cp = text.codePointAt(i);
            glyphs.add(new Glyph(cp, rgb, bold, italic, underlined));
            i += Character.charCount(cp);
        }
        if (exactPayload != null) {
            applyExactPayload(glyphs, exactPayload);
        } else if (!exactColors) {
            smooth(glyphs);
        }
        return glyphs;
    }

    /**
     * Parses the legacy MOTD and overrides its colours with the exact
     * {@code modernui_rgb} field from the status response.
     */
    public static List<Glyph> parse(String motd, JsonArray exactColors) {
        List<Glyph> glyphs = parse(motd);
        if (exactColors == null || exactColors.size() == 0) {
            return glyphs;
        }
        int count = Math.min(glyphs.size(), exactColors.size());
        for (int i = 0; i < count; i++) {
            int rgb = parseHex(exactColors.get(i).getAsString());
            if (rgb >= 0) {
                glyphs.get(i).rgb = rgb;
            }
        }
        return glyphs;
    }

    private static int parseHex(String value) {
        if (value == null) {
            return -1;
        }
        String text = value.startsWith("#") ? value.substring(1) : value;
        if (text.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(text, 16);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Applies the server's invisible per-glyph RGB payload.
     *
     * <p>The payload is a sequence of {@code §x§R§R§G§G§B§B} colours, one per
     * glyph, in display order. Vanilla 1.7.10 skips these codes, so the same
     * legacy string remains readable without ModernUI.</p>
     */
    private static void applyExactPayload(List<Glyph> glyphs, String payload) {
        int glyphIndex = 0;
        int i = 0;
        while (i < payload.length() && glyphIndex < glyphs.size()) {
            if ((payload.charAt(i) == '\u00a7' || payload.charAt(i) == '&')
                    && i + 1 < payload.length()
                    && Character.toLowerCase(payload.charAt(i + 1)) == 'x') {
                int hex = parseHexColor(payload, i);
                if (hex >= 0) {
                    glyphs.get(glyphIndex).rgb = hex;
                    glyphIndex++;
                    i += 14;
                    continue;
                }
            }
            i++;
        }
    }

    /** Parses the 1.16+ {@code §x§R§R§G§G§B§B} sequence at {@code start}. */
    private static int parseHexColor(String text, int start) {
        int value = 0;
        int digits = 0;
        int i = start + 2;
        while (digits < 6 && i + 1 < text.length()) {
            if (text.charAt(i) != '\u00a7') {
                return -1;
            }
            int digit = Character.digit(text.charAt(i + 1), 16);
            if (digit < 0) {
                return -1;
            }
            value = (value << 4) | digit;
            digits++;
            i += 2;
        }
        return digits == 6 ? value : -1;
    }

    public static boolean hasVisibleText(String motd) {
        for (Glyph glyph : parse(motd)) {
            if (glyph.codePoint != '\n' && glyph.codePoint != '\r' && !Character.isWhitespace(glyph.codePoint)
                    && glyph.codePoint != 0x00A0) {
                return true;
            }
        }
        return false;
    }

    private static void smooth(List<Glyph> glyphs) {
        if (glyphs.size() < 3) {
            return;
        }
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> colors = new ArrayList<Integer>();
        for (int i = 0; i < glyphs.size(); i++) {
            int rgb = glyphs.get(i).rgb;
            if (i == 0 || rgb != glyphs.get(i - 1).rgb) {
                starts.add(Integer.valueOf(i));
                colors.add(Integer.valueOf(rgb));
            }
        }
        if (starts.size() <= 1) {
            return;
        }
        for (int run = 0; run + 1 < starts.size(); run++) {
            int start = starts.get(run).intValue();
            int next = starts.get(run + 1).intValue();
            int end = next - 1;
            int from = colors.get(run).intValue();
            int to = colors.get(run + 1).intValue();
            int length = end - start + 1;
            for (int i = start; i <= end; i++) {
                float t = (float) (i - start + 1) / (float) (length + 1);
                glyphs.get(i).rgb = lerp(from, to, t);
            }
        }
    }

    private static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round((float) ar + t * (float) (br - ar));
        int g = Math.round((float) ag + t * (float) (bg - ag));
        int bl = Math.round((float) ab + t * (float) (bb - ab));
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    private static int legacyColor(char code) {
        switch (code) {
            case '0':
                return 0x000000;
            case '1':
                return 0x0000AA;
            case '2':
                return 0x00AA00;
            case '3':
                return 0x00AAAA;
            case '4':
                return 0xAA0000;
            case '5':
                return 0xAA00AA;
            case '6':
                return 0xFFAA00;
            case '7':
                return 0xAAAAAA;
            case '8':
                return 0x555555;
            case '9':
                return 0x5555FF;
            case 'a':
                return 0x55FF55;
            case 'b':
                return 0x55FFFF;
            case 'c':
                return 0xFF5555;
            case 'd':
                return 0xFF55FF;
            case 'e':
                return 0xFFFF55;
            case 'f':
                return 0xFFFFFF;
            default:
                return -1;
        }
    }

    public static final class Glyph {

        public final int codePoint;
        public int rgb;
        public final boolean bold;
        public final boolean italic;
        public final boolean underlined;

        Glyph(int codePoint, int rgb, boolean bold, boolean italic, boolean underlined) {
            this.codePoint = codePoint;
            this.rgb = rgb;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
        }
    }
}
