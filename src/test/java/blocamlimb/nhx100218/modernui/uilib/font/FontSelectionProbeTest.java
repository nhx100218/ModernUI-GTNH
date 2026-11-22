package blocamlimb.nhx100218.modernui.uilib.font;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;
import blocamlimb.nhx100218.modernui.uilib.font.util.DerivedFontCache;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontCatalog;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontMatcher;

/**
 * Locks the family-stack face selection: real bold/italic faces must win, and
 * missing italic faces are synthesized only inside the selected family.
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
    public void selectsRealFacesInsideTheConfiguredFamily() throws Exception {
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

        assertFace(matcher, catalog, 'A', FontType.BOLD, "Bold");
        assertFace(matcher, catalog, 'A', FontType.ITALIC, "Italic");
        assertFace(matcher, catalog, 'A', FontType.BOLD_ITALIC, "Bold Italic");
        assertFace(matcher, catalog, 0x4E2D, FontType.BOLD, "Bold");
        // Source Han has no real italic; the bold-italic fallback must keep the
        // real bold face instead of degrading to faux bold on the regular face.
        assertFace(matcher, catalog, 0x4E2D, FontType.BOLD_ITALIC, "Bold");
    }

    private static void assertFace(FontMatcher matcher, FontCatalog catalog, int codepoint,
            FontType type, String expectedPart) {
        int index = matcher.matchFontIndex(0, codepoint, type);
        Assert.assertTrue("no face selected for " + type, index >= 0);
        String name = catalog.getFont(index).getName();
        Assert.assertTrue(type + " selected " + name + " (expected " + expectedPart + ")",
                name != null && name.contains(expectedPart));
    }
}
