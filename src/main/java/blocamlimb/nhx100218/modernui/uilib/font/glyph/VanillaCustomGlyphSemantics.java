package blocamlimb.nhx100218.modernui.uilib.font.glyph;

/**
 * Semantic fallbacks for GTNH private-use custom glyphs.
 *
 * <p>These characters have no useful AWT/Unicode identity of their own, but they do have a clear
 * textual meaning. Expanding them to ordinary text and applying the existing sub/superscript style
 * keeps the character in the configured font (for example Inter Frozen) instead of falling back to
 * the vanilla bitmap page.</p>
 *
 * <p>Characters without a semantic equivalent are intentionally left untouched; the vanilla
 * unicode-page source handles those.</p>
 */
public final class VanillaCustomGlyphSemantics {

    public static final int STYLE_NONE = 0;
    public static final int STYLE_SUPERSCRIPT = 1;
    public static final int STYLE_SUBSCRIPT = 2;

    private VanillaCustomGlyphSemantics() {}

    /**
     * Returns the ordinary text this private-use character represents.
     *
     * @param character private-use character
     * @return replacement text, or null when there is no semantic equivalent
     */
    public static String textFor(char character) {
        if (character >= '\uE010' && character <= '\uE019') {
            return String.valueOf((char) ('0' + (character - '\uE010')));
        }
        switch (character) {
            case '\uE01A':
                return "0";
            case '\uE01B':
                return "\u2135"; // ALEF SYMBOL
            case '\uE01C':
                return "\u03C9"; // GREEK SMALL LETTER OMEGA
            case '\uE01D':
                return "\u300C"; // CJK LEFT CORNER BRACKET
            case '\uE020':
                return "?";
            default:
                return null;
        }
    }

    /**
     * Returns the baseline style the replacement text should use.
     *
     * @param character private-use character
     * @return {@link #STYLE_NONE}, {@link #STYLE_SUPERSCRIPT} or {@link #STYLE_SUBSCRIPT}
     */
    public static int styleFor(char character) {
        if (character >= '\uE010' && character <= '\uE019') {
            return STYLE_SUPERSCRIPT;
        }
        if (character == '\uE01A' || character == '\uE020') {
            return STYLE_SUBSCRIPT;
        }
        return STYLE_NONE;
    }
}
