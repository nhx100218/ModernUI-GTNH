package blocamlimb.nhx100218.modernui.client.motd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import blocamlimb.nhx100218.modernui.server.LegacyMotdText;
import blocamlimb.nhx100218.modernui.uilib.client.motd.MotdGradient;

public class MotdGradientTest {

    @Test
    public void markerIsRecognisedAndStripped() {
        String motd = LegacyMotdText.MARKER + "AB";
        assertTrue(MotdGradient.hasMarker(motd));
        List<MotdGradient.Glyph> glyphs = MotdGradient.parse(motd);
        assertEquals(2, glyphs.size());
        assertEquals('A', glyphs.get(0).codePoint);
        assertFalse(MotdGradient.hasMarker("AB"));
    }

    @Test
    public void interpolatesBetweenLegacyColorRuns() {
        String motd = LegacyMotdText.MARKER + "\u00a7cA\u00a7cB\u00a79C\u00a79D";
        List<MotdGradient.Glyph> glyphs = MotdGradient.parse(motd);
        assertEquals(4, glyphs.size());
        // The first run is blended towards blue instead of staying pure red.
        assertFalse(glyphs.get(0).rgb == 0xFF5555);
        assertFalse(glyphs.get(1).rgb == 0xFF5555);
    }

    @Test
    public void plainTextStaysUnchanged() {
        List<MotdGradient.Glyph> glyphs = MotdGradient.parse(LegacyMotdText.MARKER + "Hello");
        assertEquals(5, glyphs.size());
        assertEquals(0x808080, glyphs.get(0).rgb);
    }
}
