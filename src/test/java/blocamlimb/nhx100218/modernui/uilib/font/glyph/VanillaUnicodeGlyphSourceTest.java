package blocamlimb.nhx100218.modernui.uilib.font.glyph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.FontType;

/**
 * Regression for GTNH private-use glyphs that are supplied by the vanilla Unicode font pages.
 *
 * <p>GT5U writes {@code \uE020} for a subscript question mark and overrides
 * {@code unicode_page_e0.png}. The source must extract that cell and keep the vanilla advance.</p>
 */
public class VanillaUnicodeGlyphSourceTest {

    private static final int SUBSCRIPT_QUESTION = 0xE020;
    private static final int GLYPH_SIZE = 64;

    @BeforeClass
    public static void installTestPage() {
        BufferedImage page = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = page.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            // E020 cell starts at x=0, y=32; its real ink bbox is (2,9)-(6,15).
            graphics.fillRect(2, 32 + 9, 5, 7);
        } finally {
            graphics.dispose();
        }
        byte[] sizes = new byte[65536];
        sizes[SUBSCRIPT_QUESTION] = 0x26; // left=2, right=6
        VanillaUnicodeGlyphSource.setTestResources(sizes, page);
    }

    @AfterClass
    public static void clear() {
        VanillaUnicodeGlyphSource.clearTestResources();
    }

    @Test
    public void privateUseGlyphComesFromUnicodePage() {
        Assert.assertTrue(VanillaUnicodeGlyphSource.hasGlyph(SUBSCRIPT_QUESTION));
        Assert.assertEquals(3.5D, VanillaUnicodeGlyphSource.advance(SUBSCRIPT_QUESTION, 9.0D), 0.001D);

        GlyphRequestToken token = new GlyphRequestToken(1, 1L, SUBSCRIPT_QUESTION, FontType.NORMAL);
        GlyphGenerationResult result = VanillaUnicodeGlyphSource.generate(
                new GlyphGenerationTask(token, GLYPH_SIZE, GlyphGenerationPriority.HIGH));
        Assert.assertNotNull(result);
        GlyphInfo info = result.getGlyphInfo();
        Assert.assertTrue(info.hasBitmap());
        Assert.assertEquals(3.5D * GLYPH_SIZE / 9.0D, info.getAdvance(), 0.01D);
        Assert.assertEquals(50, info.getLineBaselineY());
        Assert.assertTrue(info.getSlotWidth() > 0);
        Assert.assertTrue(info.getSlotHeight() > 0);

        BufferedImage image = result.getImage();
        Assert.assertNotNull(image);
        boolean hasAlpha = false;
        for (int y = 0; y < image.getHeight() && !hasAlpha; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    hasAlpha = true;
                    break;
                }
            }
        }
        Assert.assertTrue(hasAlpha);
    }

    @Test
    public void ordinaryCharactersAreNotClaimed() {
        Assert.assertFalse(VanillaUnicodeGlyphSource.hasGlyph('A'));
        Assert.assertFalse(VanillaUnicodeGlyphSource.hasGlyph(0x2080));
    }
}
