package blocamlimb.nhx100218.modernui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A small MiniMessage-subset renderer for the 1.7.10 server-list MOTD.
 *
 * <p>1.7.10 clients only understand the sixteen legacy colours. Gradient
 * colours are therefore interpolated per character (same sRGB math as
 * Adventure's gradient tag) and then downsampled to the nearest named colour
 * (same HSV distance as MiniMOTD's fallback). For 1.16+ clients the same
 * per-character colours are emitted as a component with RGB {@code color}
 * values, so the gradient is exact instead of quantised.</p>
 *
 * <p>Supported tags: {@code <gradient:#rrggbb:#rrggbb>...</gradient>},
 * {@code <rainbow>...</rainbow>}, named colours, {@code <#rrggbb>}, bold,
 * italic, underlined, and legacy {@code &} codes.</p>
 */
public final class LegacyMotdText {

    /** Invisible legacy-reset prefix; lets a ModernUI client recognise our MOTD. */
    public static final String MARKER = "\u00a7r\u00a7r\u00a7r";

    /**
     * Invisible payload marker for the exact per-glyph RGB colours.
     *
     * <p>Vanilla 1.7.10 treats every {@code §x} sequence as a reset and skips
     * it, so the appended payload has no visible width or text. ModernUI's
     * client parser consumes it and draws the same RGB values the 1.16+ JSON
     * component carries.</p>
     */
    public static final String EXACT_MARKER = "\u00a7r\u00a7r\u00a7r\u00a7r";

    private static final char[] CODES = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f' };
    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };

    private static final String[] NAMES = { "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white" };

    private LegacyMotdText() {}

    public static String render(String input) {
        return render(input, false);
    }

    /**
     * @param hexColors true for 1.16+ clients, which understand
     *                  {@code §x§R§R§G§G§B§B} RGB colours
     */
    public static String render(String input, boolean hexColors) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        LegacySink sink = new LegacySink(hexColors);
        renderSegment(input, sink);
        return sink.toString();
    }

    /** Builds a modern component from one or two MOTD lines. */
    public static JsonObject renderJson(String line1, String line2) {
        JsonSink sink = new JsonSink();
        renderSegment(line1 == null ? "" : line1, sink);
        if (line2 != null && !line2.isEmpty()) {
            sink.newline();
            renderSegment(line2, sink);
        }
        return sink.toJson();
    }

    /**
     * Builds the invisible exact-colour payload appended to the legacy MOTD.
     *
     * <p>The payload is derived from the same component JSON sent to 1.16+
     * clients, including its legacy-run smoothing. A ModernUI 1.7.10 client can
     * therefore draw exactly the same gradient as 26.3, while vanilla 1.7.10
     * clients keep the normal 16-colour text.</p>
     */
    public static String exactColorPayload(String line1, String line2) {
        JsonObject json = renderJson(line1, line2);
        JsonArray extra = json.getAsJsonArray("extra");
        StringBuilder payload = new StringBuilder(EXACT_MARKER);
        if (extra == null) {
            return payload.toString();
        }
        for (int i = 0; i < extra.size(); i++) {
            JsonObject part = extra.get(i).getAsJsonObject();
            String text = part.has("text") ? part.get("text").getAsString() : "";
            int rgb = 0xFFFFFF;
            if (part.has("color")) {
                rgb = parseJsonColor(part.get("color").getAsString());
            }
            for (int offset = 0; offset < text.length();) {
                int codePoint = text.codePointAt(offset);
                appendHexColor(payload, rgb);
                offset += Character.charCount(codePoint);
            }
        }
        return payload.toString();
    }

    private static int parseJsonColor(String value) {
        if (value != null && value.startsWith("#") && value.length() == 7) {
            try {
                return Integer.parseInt(value.substring(1), 16);
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 0xFFFFFF;
    }

    private interface Sink {

        void color(int rgb);

        void format(char code);

        void reset();

        void text(int codePoint);

        void newline();
    }

    private static void renderSegment(String text, Sink sink) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length() && isLegacyCode(text.charAt(i + 1))) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                int color = legacyColor(code);
                if (color >= 0) {
                    sink.color(color);
                } else if (code == 'r') {
                    sink.reset();
                } else {
                    sink.format(code);
                }
                i += 2;
                continue;
            }
            if (c == '<') {
                int close = text.indexOf('>', i);
                if (close < 0) {
                    sink.text(c);
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
                    appendGradient(body, parseColors(tag.substring("gradient:".length())), sink);
                    i = end + "</gradient>".length();
                    continue;
                }
                if (lower.equals("rainbow") || lower.startsWith("rainbow:")) {
                    int end = text.indexOf("</rainbow>", close + 1);
                    if (end < 0) {
                        end = text.length();
                    }
                    appendRainbow(text.substring(close + 1, end), sink);
                    i = end + "</rainbow>".length();
                    continue;
                }
                if (lower.equals("bold") || lower.equals("b")) {
                    sink.format('l');
                    i = close + 1;
                    continue;
                }
                if (lower.equals("italic") || lower.equals("i") || lower.equals("em")) {
                    sink.format('o');
                    i = close + 1;
                    continue;
                }
                if (lower.equals("underlined") || lower.equals("u")) {
                    sink.format('n');
                    i = close + 1;
                    continue;
                }
                int color = parseColor(tag);
                if (color >= 0) {
                    sink.color(color);
                } else if (lower.startsWith("/")) {
                    sink.reset();
                }
                i = close + 1;
                continue;
            }
            sink.text(c);
            i++;
        }
    }

    private static void appendGradient(String body, List<Integer> stops, Sink sink) {
        String plain = stripTags(body);
        if (plain.isEmpty()) {
            return;
        }
        int[] colors = new int[stops.size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = stops.get(i).intValue();
        }
        int length = plain.codePointCount(0, plain.length());
        for (int i = 0; i < length; i++) {
            int cp = plain.codePointAt(plain.offsetByCodePoints(0, i));
            sink.color(gradientAt(colors, i, length));
            sink.text(cp);
        }
    }

    private static void appendRainbow(String body, Sink sink) {
        String plain = stripTags(body);
        if (plain.isEmpty()) {
            return;
        }
        int length = plain.codePointCount(0, plain.length());
        for (int i = 0; i < length; i++) {
            int cp = plain.codePointAt(plain.offsetByCodePoints(0, i));
            float hue = length <= 1 ? 0.0F : (float) i / (float) (length - 1);
            sink.color(hsvToRgb(hue, 1.0F, 1.0F));
            sink.text(cp);
        }
    }

    private static final class LegacySink implements Sink {

        private final StringBuilder out = new StringBuilder();
        private final boolean hexColors;
        private char lastCode;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strike;
        private boolean obfuscated;

        LegacySink(boolean hexColors) {
            this.hexColors = hexColors;
        }

        @Override
        public void color(int rgb) {
            if (hexColors) {
                appendHexColor(out, rgb);
            } else {
                char code = nearestCode(rgb);
                if (code != lastCode) {
                    out.append('\u00a7').append(code);
                    lastCode = code;
                }
            }
            // Legacy colour codes reset decorations; re-apply the active ones.
            if (bold) {
                out.append('\u00a7').append('l');
            }
            if (italic) {
                out.append('\u00a7').append('o');
            }
            if (underlined) {
                out.append('\u00a7').append('n');
            }
            if (strike) {
                out.append('\u00a7').append('m');
            }
            if (obfuscated) {
                out.append('\u00a7').append('k');
            }
        }

        @Override
        public void format(char code) {
            switch (code) {
                case 'l':
                    bold = true;
                    break;
                case 'o':
                    italic = true;
                    break;
                case 'n':
                    underlined = true;
                    break;
                case 'm':
                    strike = true;
                    break;
                case 'k':
                    obfuscated = true;
                    break;
                default:
                    return;
            }
            out.append('\u00a7').append(code);
        }

        @Override
        public void reset() {
            out.append('\u00a7').append('r');
            lastCode = 0;
            bold = false;
            italic = false;
            underlined = false;
            strike = false;
            obfuscated = false;
        }

        @Override
        public void text(int codePoint) {
            out.appendCodePoint(codePoint);
        }

        @Override
        public void newline() {
            out.append('\n');
        }

        @Override
        public String toString() {
            return out.toString();
        }
    }

    private static final class JsonSink implements Sink {

        private final List<Run> runs = new ArrayList<Run>();
        private Run current;
        private int rgb = -1;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strike;
        private boolean obfuscated;

        @Override
        public void color(int rgb) {
            this.rgb = rgb;
            // MiniMessage colour tags do not clear bold/italic/underline.
            ensureRun();
        }

        @Override
        public void format(char code) {
            switch (code) {
                case 'l':
                    bold = true;
                    break;
                case 'o':
                    italic = true;
                    break;
                case 'n':
                    underlined = true;
                    break;
                case 'm':
                    strike = true;
                    break;
                case 'k':
                    obfuscated = true;
                    break;
                default:
                    return;
            }
            ensureRun();
        }

        @Override
        public void reset() {
            rgb = -1;
            bold = false;
            italic = false;
            underlined = false;
            strike = false;
            obfuscated = false;
        }

        @Override
        public void text(int codePoint) {
            ensureRun().text.appendCodePoint(codePoint);
        }

        @Override
        public void newline() {
            ensureRun().text.append('\n');
        }

        private Run ensureRun() {
            if (current == null || current.rgb != rgb || current.bold != bold || current.italic != italic
                    || current.underlined != underlined || current.strike != strike
                    || current.obfuscated != obfuscated) {
                current = new Run(rgb, bold, italic, underlined, strike, obfuscated);
                runs.add(current);
            }
            return current;
        }

        JsonObject toJson() {
            List<StyledGlyph> glyphs = flatten();
            smoothLegacyRuns(glyphs);
            JsonObject root = new JsonObject();
            root.addProperty("text", "");
            JsonArray extra = new JsonArray();
            StringBuilder text = null;
            StyledGlyph currentStyle = null;
            for (StyledGlyph glyph : glyphs) {
                if (text == null || currentStyle == null || !currentStyle.sameStyle(glyph)) {
                    if (text != null) {
                        addPart(extra, text, currentStyle);
                    }
                    text = new StringBuilder();
                    currentStyle = glyph;
                }
                text.appendCodePoint(glyph.codePoint);
            }
            if (text != null) {
                addPart(extra, text, currentStyle);
            }
            root.add("extra", extra);
            return root;
        }

        private List<StyledGlyph> flatten() {
            List<StyledGlyph> glyphs = new ArrayList<StyledGlyph>();
            for (Run run : runs) {
                for (int i = 0; i < run.text.length();) {
                    int cp = run.text.codePointAt(i);
                    glyphs.add(new StyledGlyph(cp, run.rgb, run.bold, run.italic, run.underlined, run.strike,
                            run.obfuscated));
                    i += Character.charCount(cp);
                }
            }
            return glyphs;
        }

        /**
         * The server config may already be 16-colour MiniMOTD fallback text.
         * Interpolate between its colour runs exactly like the 1.7.10 client
         * does, otherwise 26.3 can only show hard colour blocks. Exact RGB
         * gradients (one colour per glyph) are left untouched.
         */
        private static void smoothLegacyRuns(List<StyledGlyph> glyphs) {
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
            if (starts.size() <= 1 || starts.size() * 4 >= glyphs.size() * 3) {
                return;
            }
            for (int run = 0; run + 1 < starts.size(); run++) {
                int start = starts.get(run).intValue();
                int end = starts.get(run + 1).intValue() - 1;
                int from = colors.get(run).intValue();
                int to = colors.get(run + 1).intValue();
                int length = end - start + 1;
                for (int i = start; i <= end; i++) {
                    float t = (float) (i - start + 1) / (float) (length + 1);
                    glyphs.get(i).rgb = lerp(from, to, t);
                }
            }
        }

        private static void addPart(JsonArray extra, StringBuilder text, StyledGlyph style) {
            if (text.length() == 0 || style == null) {
                return;
            }
            JsonObject part = new JsonObject();
            part.addProperty("text", text.toString());
            if (style.rgb >= 0) {
                part.addProperty("color", String.format(Locale.ROOT, "#%06x", Integer.valueOf(style.rgb)));
            }
            if (style.bold) {
                part.addProperty("bold", Boolean.TRUE);
            }
            if (style.italic) {
                part.addProperty("italic", Boolean.TRUE);
            }
            if (style.underlined) {
                part.addProperty("underlined", Boolean.TRUE);
            }
            if (style.strike) {
                part.addProperty("strikethrough", Boolean.TRUE);
            }
            if (style.obfuscated) {
                part.addProperty("obfuscated", Boolean.TRUE);
            }
            extra.add(part);
        }
    }

    private static final class StyledGlyph {

        final int codePoint;
        int rgb;
        final boolean bold;
        final boolean italic;
        final boolean underlined;
        final boolean strike;
        final boolean obfuscated;

        StyledGlyph(int codePoint, int rgb, boolean bold, boolean italic, boolean underlined, boolean strike,
                boolean obfuscated) {
            this.codePoint = codePoint;
            this.rgb = rgb;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
            this.strike = strike;
            this.obfuscated = obfuscated;
        }

        boolean sameStyle(StyledGlyph other) {
            return rgb == other.rgb && bold == other.bold && italic == other.italic
                    && underlined == other.underlined && strike == other.strike
                    && obfuscated == other.obfuscated;
        }
    }

    private static final class Run {

        final StringBuilder text = new StringBuilder();
        final int rgb;
        final boolean bold;
        final boolean italic;
        final boolean underlined;
        final boolean strike;
        final boolean obfuscated;

        Run(int rgb, boolean bold, boolean italic, boolean underlined, boolean strike, boolean obfuscated) {
            this.rgb = rgb;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
            this.strike = strike;
            this.obfuscated = obfuscated;
        }
    }

    private static void appendHexColor(StringBuilder out, int rgb) {
        out.append('\u00a7').append('x');
        appendHexPair(out, (rgb >> 16) & 0xFF);
        appendHexPair(out, (rgb >> 8) & 0xFF);
        appendHexPair(out, rgb & 0xFF);
    }

    private static void appendHexPair(StringBuilder out, int value) {
        out.append('\u00a7').append(Character.forDigit((value >> 4) & 0xF, 16));
        out.append('\u00a7').append(Character.forDigit(value & 0xF, 16));
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
                if (i + 1 < text.length() && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')
                        && i + 13 < text.length()) {
                    // §x§R§R§G§G§B§B is a colour sequence, never visible text.
                    i += 13;
                } else {
                    i++;
                }
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

    private static int legacyColor(char code) {
        switch (code) {
            case '0':
                return COLORS[0];
            case '1':
                return COLORS[1];
            case '2':
                return COLORS[2];
            case '3':
                return COLORS[3];
            case '4':
                return COLORS[4];
            case '5':
                return COLORS[5];
            case '6':
                return COLORS[6];
            case '7':
                return COLORS[7];
            case '8':
                return COLORS[8];
            case '9':
                return COLORS[9];
            case 'a':
                return COLORS[10];
            case 'b':
                return COLORS[11];
            case 'c':
                return COLORS[12];
            case 'd':
                return COLORS[13];
            case 'e':
                return COLORS[14];
            case 'f':
                return COLORS[15];
            default:
                return -1;
        }
    }

    private static boolean isLegacyCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f') || lower == 'k' || lower == 'l'
                || lower == 'm' || lower == 'n' || lower == 'o' || lower == 'r';
    }
}
