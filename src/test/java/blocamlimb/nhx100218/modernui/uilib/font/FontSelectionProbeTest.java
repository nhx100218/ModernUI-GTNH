package blocamlimb.nhx100218.modernui.uilib.font;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;
import blocamlimb.nhx100218.modernui.uilib.font.util.DerivedFontCache;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontCatalog;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontMatcher;

/**
 * Locks the family-stack face selection after bold faces were removed: bold is
 * synthesized from the regular face, while a real italic face is still used.
 */
public class FontSelectionProbeTest {

    private String[] savedFontSort;
    private boolean savedConfigured;
    private double savedGlyphSize;

    @Before
    public void saveState() {
        savedFontSort = FontConfig.fontSort;
        savedConfigured = FontConfig.fontSortConfigured;
        savedGlyphSize = FontConfig.glyphGenerationSize;
    }

    @After
    public void restoreState() {
        FontConfig.fontSort = savedFontSort;
        FontConfig.fontSortConfigured = savedConfigured;
        FontConfig.glyphGenerationSize = savedGlyphSize;
        FontConfig.onConfigReload();
    }

    @Test
    public void selectsRegularOrItalicFacesForSyntheticBold() throws Exception {
        File root = Files.createTempDirectory("modernui-font-probe").toFile();
        FontConfig.fontSort = new String[] {"Inter", "思源黑体"};
        FontConfig.fontSortConfigured = true;
        FontConfig.glyphGenerationSize = 64.0D;

        FontGenerationBuildRequest request = FontGenerationBuildRequest.capture(0L, null, root);
        FontCatalog catalog = new FontCatalog();
        FontGenerationRegistry registry = new FontGenerationRegistry(catalog);
        FontGenerationRegistry.PreparedCatalog prepared =
                registry.prepare(request.getSettings(), FontResourceSnapshot.capture(request));
        catalog.replaceAll(prepared.getCatalogSnapshot().getFonts());
        FontMatcher matcher = new FontMatcher(catalog, new DerivedFontCache(catalog));

        assertFacePart(matcher, catalog, 'A', FontType.BOLD, "Inter Frozen Medium");
        assertFacePart(matcher, catalog, 'A', FontType.ITALIC, "Italic");
        assertFacePart(matcher, catalog, 'A', FontType.BOLD_ITALIC, "Italic");
        assertFacePart(matcher, catalog, 0x4E2D, FontType.BOLD, "Source Han Sans", "思源黑体");
        assertFacePart(matcher, catalog, 0x4E2D, FontType.BOLD_ITALIC, "Source Han Sans", "思源黑体");
    }

    private static void assertFacePart(FontMatcher matcher, FontCatalog catalog, int codepoint,
            FontType type, String... expectedParts) {
        int index = matcher.matchFontIndex(0, codepoint, type);
        Assert.assertTrue("no face selected for " + type, index >= 0);
        String name = catalog.getFont(index).getName();
        boolean matched = name != null;
        if (matched) {
            matched = false;
            for (String part : expectedParts) {
                if (name.contains(part)) {
                    matched = true;
                    break;
                }
            }
        }
        Assert.assertTrue(type + " selected " + name + " (expected one of "
                        + Arrays.toString(expectedParts) + ")",
                matched);
    }
}
