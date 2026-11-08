package blocamlimb.nhx100218.modernui.uilib.font.layout;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.render.software.LatexSoftwareRenderKit;
import blocamlimb.nhx100218.modernui.uilib.ui.text.TextContentMode;

/**
 * Regression for GTNH private-use glyphs that should stay in the configured font.
 *
 * <p>GT5U uses {@code \uE020} for a subscript question mark and {@code \uE010..\uE019} for
 * superscript digits. They must expand to ordinary text with the existing sub/superscript styles
 * instead of falling back to the vanilla bitmap page.</p>
 */
public class VanillaCustomGlyphSemanticsTest {

    @AfterClass
    public static void release() {
        LatexSoftwareRenderKit.resetShared();
    }

    @Test
    public void subscriptQuestionMarkUsesConfiguredFont() {
        TextLayoutService service = LatexSoftwareRenderKit.currentService();
        List<TextSegment> segments = service.parseSegments("C\u2082\uE020", 0xFFFFFFFF,
                TextContentMode.MINECRAFT_FORMATTED);
        Assert.assertEquals("C\u2082?", join(segments));
        TextSegment question = find(segments, "?");
        Assert.assertNotNull(question);
        Assert.assertTrue(question.getStyle().isSubscript());
        Assert.assertFalse(question.getStyle().isSuperscript());
    }

    @Test
    public void superscriptDigitsUseConfiguredFont() {
        TextLayoutService service = LatexSoftwareRenderKit.currentService();
        List<TextSegment> segments = service.parseSegments("x\uE010\uE011", 0xFFFFFFFF,
                TextContentMode.MINECRAFT_FORMATTED);
        Assert.assertEquals("x01", join(segments));
        TextSegment zero = find(segments, "0");
        TextSegment one = find(segments, "1");
        Assert.assertNotNull(zero);
        Assert.assertNotNull(one);
        Assert.assertTrue(zero.getStyle().isSuperscript());
        Assert.assertTrue(one.getStyle().isSuperscript());
    }

    private static String join(List<TextSegment> segments) {
        StringBuilder builder = new StringBuilder();
        for (TextSegment segment : segments) {
            builder.append(segment.getText());
        }
        return builder.toString();
    }

    private static TextSegment find(List<TextSegment> segments, String text) {
        for (TextSegment segment : segments) {
            if (text.equals(segment.getText())) {
                return segment;
            }
        }
        return null;
    }
}
