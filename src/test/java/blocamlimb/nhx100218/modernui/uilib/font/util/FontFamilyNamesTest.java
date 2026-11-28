package blocamlimb.nhx100218.modernui.uilib.font.util;

import java.awt.Font;
import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * Family grouping is what makes automatic bold/italic and the localized
 * Source Han family work.
 */
public class FontFamilyNamesTest {

    @Test
    public void aliasesResolveBundledEnglishAndLocalizedNamesToOneFamily() {
        Assert.assertEquals(FontFamilyNames.INTER, FontFamilyNames.canonicalName("Inter Frozen Medium"));
        Assert.assertEquals(FontFamilyNames.INTER, FontFamilyNames.canonicalName("Inter Bold"));
        Assert.assertEquals(FontFamilyNames.SOURCE_HAN_SANS,
                FontFamilyNames.canonicalName("Source Han Sans CN Medium"));
        Assert.assertEquals(FontFamilyNames.SOURCE_HAN_SANS,
                FontFamilyNames.canonicalName("思源黑体 CN Medium"));
        Assert.assertEquals(FontFamilyNames.SOURCE_HAN_SANS,
                FontFamilyNames.canonicalName("思源黑体"));
    }

    @Test
    public void familyNameKeepsFamilyCaseAndStripsOnlyStyleSuffixes() {
        Assert.assertEquals("Arial", FontFamilyNames.familyName(new Font("Arial", Font.BOLD, 14)));
        Assert.assertEquals("Arial Black", FontFamilyNames.familyName(new Font("Arial Black", Font.PLAIN, 14)));
        Assert.assertEquals("Microsoft YaHei",
                FontFamilyNames.familyName(new Font("Microsoft YaHei", Font.PLAIN, 14)));
    }

    @Test
    public void bundledFilesReportCanonicalFamilies() throws Exception {
        File inter = new File("src/main/resources/assets/modernui/fonts/bundled/inter-frozen-medium.otf");
        File sourceHan = new File("src/main/resources/assets/modernui/fonts/bundled/source-han-sans-cn-medium.otf");
        Assert.assertTrue(inter.isFile());
        Assert.assertTrue(sourceHan.isFile());
        Assert.assertEquals(FontFamilyNames.INTER,
                FontFamilyNames.familyName(Font.createFont(Font.TRUETYPE_FONT, inter)));
        Assert.assertEquals(FontFamilyNames.SOURCE_HAN_SANS,
                FontFamilyNames.familyName(Font.createFont(Font.TRUETYPE_FONT, sourceHan)));
    }

}
