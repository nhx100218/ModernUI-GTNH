package blocamlimb.nhx100218.modernui.uilib.font.layout;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.render.software.LatexSoftwareRenderKit;
import blocamlimb.nhx100218.modernui.uilib.ui.text.TextContentMode;

/**
 * Regression for GTNHLib custom glyphs used by GregTech chemical formulas.
 *
 * <p>{@code GTUtility.toSubscript} writes {@code \uE01A} instead of {@code \u2080}. GTNHLib
 * registers that private-use character as a replacement for the real subscript zero; ModernUI
 * replaces the vanilla renderer, so it must expand the mapping before layout and measurement.</p>
 */
public class GlyphReplacementCompatTest {

    private static final char CUSTOM_SUBSCRIPT_ZERO = '\uE01A';
    private static final char CUSTOM_UNRENDERABLE = '\uE0FF';
    private static final char UNRENDERABLE_ORIGINAL = '\u0378';

    @BeforeClass
    public static void registerMappings() throws Exception {
        Class<?> glyphs = Class.forName("com.gtnewhorizon.gtnhlib.util.font.GlyphReplacements");
        Method register = glyphs.getMethod("registerCustomGlyph", String.class, char.class);
        register.invoke(null, String.valueOf(CUSTOM_SUBSCRIPT_ZERO), Character.valueOf('0'));
        register.invoke(null, String.valueOf(CUSTOM_UNRENDERABLE), Character.valueOf(UNRENDERABLE_ORIGINAL));
    }

    @AfterClass
    public static void release() {
        LatexSoftwareRenderKit.resetShared();
    }

    @Test
    public void registeredCustomGlyphIsExpandedBeforeLayoutAndMeasurement() {
        TextLayoutService service = LatexSoftwareRenderKit.currentService();
        String custom = "Fe" + CUSTOM_SUBSCRIPT_ZERO + "C";
        Assert.assertEquals("Fe0C", join(service.parseSegments(custom, 0xFFFFFFFF,
                TextContentMode.MINECRAFT_FORMATTED)));
        Assert.assertEquals(service.getStringWidth("Fe0C"), service.getStringWidth(custom));
    }

    @Test
    public void unrenderableReplacementKeepsTheCustomGlyph() {
        TextLayoutService service = LatexSoftwareRenderKit.currentService();
        String custom = "A" + CUSTOM_UNRENDERABLE + "B";
        Assert.assertEquals(custom, join(service.parseSegments(custom, 0xFFFFFFFF,
                TextContentMode.MINECRAFT_FORMATTED)));
    }

    private static String join(List<TextSegment> segments) {
        StringBuilder builder = new StringBuilder();
        for (TextSegment segment : segments) {
            builder.append(segment.getText());
        }
        return builder.toString();
    }
}
