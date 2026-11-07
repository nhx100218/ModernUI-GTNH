package blocamlimb.nhx100218.modernui.uilib.font.util;

import java.lang.reflect.Method;

/**
 * Optional bridge to GTNHLib's custom glyph registry.
 *
 * <p>Several GTNH mods (GregTech in particular) register private-use characters such as
 * {@code \uE01A} as stand-ins for glyphs the vanilla font renderer cannot address directly.
 * GTNHLib exposes the mapping through {@code GlyphReplacements}; Angelica applies it in its own
 * font renderer. ModernUI replaces the vanilla renderer, so it has to consult the same registry
 * to keep those characters readable.</p>
 *
 * <p>The lookup is reflective on purpose: GTNHLib is optional and must not become a load-time
 * dependency of the font pipeline.</p>
 */
public final class GlyphReplacementCompat {

    private static final String REPLACEMENTS_CLASS = "com.gtnewhorizon.gtnhlib.util.font.GlyphReplacements";

    private static Method replacementMethod;
    private static boolean resolved;

    private GlyphReplacementCompat() {}

    /**
     * Returns the original glyph registered for a private-use replacement char.
     *
     * @param replacement the character found in the text
     * @return the original character, or {@code 0} when no mapping is available
     */
    public static char replacementFor(char replacement) {
        Method method = replacementMethod();
        if (method == null) {
            return 0;
        }
        try {
            Object result = method.invoke(null, Character.valueOf(replacement));
            return result instanceof Character ? ((Character) result).charValue() : 0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static Method replacementMethod() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> type = Class.forName(REPLACEMENTS_CLASS, false,
                        GlyphReplacementCompat.class.getClassLoader());
                replacementMethod = type.getMethod("getReplacementGlyph", char.class);
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
                replacementMethod = null;
            }
        }
        return replacementMethod;
    }
}
