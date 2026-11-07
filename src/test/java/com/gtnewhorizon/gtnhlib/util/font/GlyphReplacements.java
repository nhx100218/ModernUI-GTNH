package com.gtnewhorizon.gtnhlib.util.font;

import java.util.HashMap;
import java.util.Map;

/**
 * Test-only stand-in for GTNHLib's custom glyph registry.
 *
 * <p>The real class pulls in fastutil, which is not part of the test runtime classpath. Production
 * code reaches this API reflectively and degrades to "no replacement" when it is absent, so the
 * tests only need the same public surface.</p>
 */
public final class GlyphReplacements {

    private static final Map<Character, Character> REPLACEMENTS = new HashMap<Character, Character>();

    private GlyphReplacements() {}

    public static String registerCustomGlyph(String replacement, char original) {
        if (replacement != null && !replacement.isEmpty()) {
            REPLACEMENTS.put(Character.valueOf(replacement.charAt(0)), Character.valueOf(original));
        }
        return replacement;
    }

    public static char getReplacementGlyph(char replacement) {
        Character original = REPLACEMENTS.get(Character.valueOf(replacement));
        return original == null ? 0 : original.charValue();
    }
}
