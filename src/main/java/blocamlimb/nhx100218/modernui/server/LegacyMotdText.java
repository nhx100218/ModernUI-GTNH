package blocamlimb.nhx100218.modernui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small MiniMessage-subset renderer for the 1.7.10 server-list MOTD.
 *
 * <p>1.7.10 clients only understand the sixteen legacy colours. Gradient
 * colours are therefore interpolated per character (same sRGB math as
 * Adventure's gradient tag) and then downsampled to the nearest named colour
 * (same HSV distance as MiniMOTD's fallback).</p>
 *
 * <p>Supported tags: {@code <gradient:#rrggbb:#rrggbb>...</gradient>},
 * {@code <rainbow>...</rainbow>}, named colours, {@code <#rrggbb>}, bold,
 * italic, underlined, and legacy {@code &} codes.</p>
 */
public final class LegacyMotdText {

    /** Invisible legacy-reset prefix; lets a ModernUI client recognise our MOTD. */
    public static final String MARKER = "\u00a7r\u00a7r\u00a7r";

    private static final char[] CODES = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f' };
    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };

    private static final String[] NAMES = { "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white" };

    private LegacyMotdText() {}

    public static String render(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        renderSegment(input, out);
        return out.toString();
    }

    private static void renderSegment(String text, StringBuilder out) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length() && isLegacyCode(text.charAt(i + 1))) {
                out.append('\u00a7').append(Character.toLowerCase(text.charAt(i + 1)));
                i += 2;
                continue;
            }
            if (c == '<') {
                int close = text.indexOf('>', i);
                if (close < 0) {
                    out.append(c);
                    i++;
                    continue;
                }
                String tag = text.substring(i + 1, close).trim();
                String lower = tag.toLowerCase(Locale.ROOT);
                if (lower.startsWith("gradient:")) {
                    int end = text.indexOf("</gradient>", close + 1);
                    if (end < 0) {
                        end = text.length();
                    }
                    String body = text.substring(close + 1, end);
                    appendGradient(body, parseColors(tag.substring("gradient:".length())), out);
                    i = end + "</gradient>".length();
                    continue;
                }
                if (lower.equals("rainbow") || lower.startsWith("rainbow:")) {
                    int end = text.indexOf("</rainbow>", close + 1);
                    if (end < 0) {
                        end = text.length();
                    }
                    appendRainbow(text.substring(close + 1, end), out);
                    i = end + "</rainbow>".length();
                    continue;
                }
                if (lower.equals("bold") || lower.equals("b")) {
                    out.append('\u00a7').append('l');
                    i = close + 1;
                    continue;
                }
                if (lower.equals("italic") || lower.equals("i") || lower.equals("em")) {
                    out.append('\u00a7').append('o');
                    i = close + 1;
                    continue;
                }
                if (lower.equals("underlined") || lower.equals("u")) {
                    out.append('\u00a7').append('n');
                    i = close + 1;
                    continue;
                }
                int color = parseColor(tag);
                if (color >= 0) {
                    out.append('\u00a7').append(nearestCode(color));
                } else if (lower.startsWith("/")) {
                    out.append('\u00a7').append('r');
                }
                i = close + 1;
                continue;
            }
            out.append(c);
            i++;
        }
    }

    private static void appendGradient(String body, List<Integer> stops, StringBuilder out) {
        String plain = stripTags(body);
        if (plain.isEmpty()) {
            return;
        }
        int[] colors = new int[stops.size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = stops.get(i).intValue();
        }
        int length = plain.codePointCount(0, plain.length());
        char lastCode = 0;
        for (int i = 0; i < length; i++) {
            int cp = plain.codePointAt(plain.offsetByCodePoints(0, i));
            int rgb = gradientAt(colors, i, length);
            char code = nearestCode(rgb);
            if (code != lastCode) {
                out.append('\u00a7').append(code);
                lastCode = code;
            }
            out.appendCodePoint(cp);
        }
    }

    private static void appendRainbow(String body, StringBuilder out) {
        String plain = stripTags(body);
        if (plain.isEmpty()) {
            return;
        }
        int length = plain.codePointCount(0, plain.length());
        char lastCode = 0;
        for (int i = 0; i < length; i++) {
            int cp = plain.codePointAt(plain.offsetByCodePoints(0, i));
            float hue = length <= 1 ? 0.0F : (float) i / (float) (length - 1);
            int rgb = hsvToRgb(hue, 1.0F, 1.0F);
            char code = nearestCode(rgb);
            if (code != lastCode) {
                out.append('\u00a7').append(code);
                lastCode = code;
            }
            out.appendCodePoint(cp);
        }
    }

    private static int gradientAt(int[] colors, int index, int length) {
        if (colors.length == 0) {
            return 0xFFFFFF;
        }
        if (colors.length == 1 || length <= 1) {
            return colors[0];
        }
        double position = (double) index * (double) (colors.length - 1) / (double) (length - 1);
        int low = (int) Math.floor(position);
        int high = (int) Math.ceil(position);
        if (low < 0) {
            low = 0;
        }
        if (high >= colors.length) {
            high = colors.length - 1;
        }
        float t = (float) (position - (double) low);
        return lerp(colors[low], colors[high], t);
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

    public static boolean hasVisibleText(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7') {
                i++;
                continue;
            }
            if (c != '\n' && c != '\r' && !Character.isWhitespace(c) && c != '\u00a0') {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> parseColors(String value) {
        List<Integer> colors = new ArrayList<Integer>();
        String[] parts = value.split(":");
        for (String part : parts) {
            int color = parseColor(part.trim());
            if (color >= 0) {
                colors.add(Integer.valueOf(color));
            }
        }
        if (colors.isEmpty()) {
            colors.add(Integer.valueOf(0xFFFFFF));
            colors.add(Integer.valueOf(0x000000));
        }
        if (colors.size() == 1) {
            colors.add(colors.get(0));
        }
        return colors;
    }

    private static int parseColor(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(v)) {
                return COLORS[i];
            }
        }
        if (v.startsWith("#") && v.length() == 7) {
            try {
                return Integer.parseInt(v.substring(1), 16);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static String stripTags(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '<') {
                int close = text.indexOf('>', i);
                if (close < 0) {
                    out.append(c);
                    i++;
                } else {
                    i = close + 1;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static char nearestCode(int rgb) {
        float hue = hue(rgb);
        float sat = saturation(rgb);
        float val = value(rgb);
        int best = 15;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < COLORS.length; i++) {
            float h = hue(COLORS[i]);
            float s = saturation(COLORS[i]);
            float v = value(COLORS[i]);
            double hueDistance = 3.0D * Math.min(Math.abs(hue - h), 1.0D - Math.abs(hue - h));
            double distance = hueDistance * hueDistance + (sat - s) * (sat - s) + (val - v) * (val - v);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return CODES[best];
    }

    private static float hue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max == 0) {
            return 0.0F;
        }
        return hue(r, g, b, max);
    }

    private static float saturation(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        return max == 0 ? 0.0F : (float) (max - Math.min(r, Math.min(g, b))) / (float) max;
    }

    private static float value(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (float) Math.max(r, Math.max(g, b)) / 255.0F;
    }

    private static float hue(int r, int g, int b, int max) {
        float delta = (float) (max - Math.min(r, Math.min(g, b)));
        if (delta == 0.0F) {
            return 0.0F;
        }
        float h;
        if (max == r) {
            h = ((float) (g - b) / delta) % 6.0F;
        } else if (max == g) {
            h = (float) (b - r) / delta + 2.0F;
        } else {
            h = (float) (r - g) / delta + 4.0F;
        }
        h /= 6.0F;
        return h < 0.0F ? h + 1.0F : h;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int sector = (int) Math.floor(h);
        float f = h - (float) sector;
        int p = Math.round(value * (1.0F - saturation) * 255.0F);
        int q = Math.round(value * (1.0F - saturation * f) * 255.0F);
        int t = Math.round(value * (1.0F - saturation * (1.0F - f)) * 255.0F);
        int v = Math.round(value * 255.0F);
        switch (sector) {
            case 0:
                return (v << 16) | (t << 8) | p;
            case 1:
                return (q << 16) | (v << 8) | p;
            case 2:
                return (p << 16) | (v << 8) | t;
            case 3:
                return (p << 16) | (q << 8) | v;
            case 4:
                return (t << 16) | (p << 8) | v;
            default:
                return (v << 16) | (p << 8) | q;
        }
    }

    private static boolean isLegacyCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f') || lower == 'k' || lower == 'l'
                || lower == 'm' || lower == 'n' || lower == 'o' || lower == 'r';
    }
}
