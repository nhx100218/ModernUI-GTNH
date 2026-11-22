package blocamlimb.nhx100218.modernui.uilib.font.util;

import java.awt.Font;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves an AWT {@link Font} to the family name the player configures.
 *
 * <p>ModernUI's bundled fonts carry different metadata depending on the file:
 * {@code inter-bold.ttf} has the family {@code Inter}, while
 * {@code inter-frozen-medium.otf} reports {@code Inter Frozen Medium}; the
 * Source Han files report either the localized {@code 思源黑体 CN Medium} or the
 * localized family {@code 思源黑体}. Treating those as separate families breaks
 * automatic bold/italic selection and makes an English default miss the
 * localized family name.</p>
 *
 * <p>This class therefore exposes one canonical family per family group and
 * accepts the common localized/English spellings as aliases. It is intentionally
 * data-only: no font discovery, no I/O and no global state.</p>
 */
public final class FontFamilyNames {

    /** Canonical name of the bundled Latin family. */
    public static final String INTER = "Inter";
    /** Canonical name of the bundled CJK family. */
    public static final String SOURCE_HAN_SANS = "思源黑体";

    private static final Map<String, String> ALIASES = new LinkedHashMap<String, String>();

    static {
        alias(INTER, "Inter", "Inter Frozen", "Inter Frozen Medium", "Inter Bold", "Inter Bold Italic",
                "Inter Frozen Medium Italic", "Inter-Bold", "Inter-BoldItalic", "InterFrozen-Medium",
                "InterFrozen-MediumItalic");

        alias(SOURCE_HAN_SANS,
                "思源黑体", "思源黑体 CN", "思源黑体 SC", "思源黑体 CN Medium", "思源黑体 Bold",
                "Source Han Sans", "Source Han Sans CN", "Source Han Sans SC",
                "Source Han Sans CN Medium", "Source Han Sans SC Bold",
                "SourceHanSansCN-Medium", "SourceHanSansSC-Bold");
    }

    private FontFamilyNames() {
    }

    /**
     * Returns the canonical family name for a font file/face.
     *
     * <p>Aliases win first so bundled fonts are grouped exactly like the
     * ModernUI defaults. Otherwise the AWT display name is used, with a trailing
     * style token removed so {@code Arial Bold} stays in the {@code Arial}
     * family and the engine can pick real bold/italic faces automatically.</p>
     *
     * @param font font face, may be null
     * @return canonical family name, never null
     */
    public static String familyName(Font font) {
        if (font == null) {
            return "";
        }
        for (String candidate : descriptors(font)) {
            String canonical = canonicalName(candidate);
            if (!canonical.isEmpty()) {
                return canonical;
            }
        }
        return "";
    }

    /**
     * Resolves a configured family name/path to its canonical family name.
     *
     * @param value configured name or path
     * @return canonical name, or the trimmed input when no alias applies
     */
    public static String canonicalName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        String alias = ALIASES.get(trimmed.toLowerCase(Locale.ENGLISH));
        if (alias != null) {
            return alias;
        }
        String stripped = stripTrailingStyle(trimmed);
        alias = ALIASES.get(stripped.toLowerCase(Locale.ENGLISH));
        return alias != null ? alias : stripped;
    }

    /**
     * Normalizes a name for map/set comparisons (case and whitespace folded).
     *
     * @param value name
     * @return lookup key
     */
    public static String key(String value) {
        return canonicalName(value).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Tests a font against a configured family name, including aliases.
     *
     * @param font           font face
     * @param configuredName configured family name
     * @return true when the face belongs to that family
     */
    public static boolean matches(Font font, String configuredName) {
        String configuredKey = key(configuredName);
        if (configuredKey.isEmpty()) {
            return false;
        }
        return key(familyName(font)).equals(configuredKey);
    }

    /** Candidate descriptors ordered from most to least specific. */
    private static Set<String> descriptors(Font font) {
        Set<String> values = new LinkedHashSet<String>();
        add(values, font.getName());
        add(values, font.getFamily(Locale.ENGLISH));
        add(values, font.getFamily());
        add(values, font.getFontName(Locale.ENGLISH));
        add(values, font.getFontName());
        add(values, font.getPSName());
        return values;
    }

    private static void add(Set<String> values, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (!normalized.isEmpty()) {
            values.add(normalized);
        }
    }

    private static void alias(String canonical, String... names) {
        for (String name : Arrays.asList(names)) {
            ALIASES.put(normalize(name), canonical);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
    }

    /**
     * Removes a trailing weight/style token from an AWT display name.
     *
     * <p>Deliberately does not strip {@code Black}: {@code Arial Black} is a
     * distinct family, not Arial bold. The other tokens are weight/style
     * suffixes that AWT appends to a family display name.</p>
     */
    private static String stripTrailingStyle(String value) {
        String result = value;
        boolean changed = true;
        while (changed) {
            changed = false;
            String lower = result.toLowerCase(Locale.ENGLISH);
            for (String suffix : new String[] { " semi bold", " semibold", " bold italic", " bold oblique",
                    " extra bold", " extrabold", " extra light", " extralight", " ultra light", " ultralight",
                    " bold", " italic", " oblique", " regular", " medium", " light", " heavy", " thin" }) {
                if (lower.endsWith(suffix) && result.length() > suffix.length()) {
                    result = result.substring(0, result.length() - suffix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }
        return result;
    }
}
