package blocamlimb.nhx100218.modernui.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LegacyMotdTextTest {

    @Test
    public void keepsLegacyCodes() {
        String rendered = LegacyMotdText.render("&aHello &lWorld");
        assertTrue(rendered.contains("\u00a7a"));
        assertTrue(rendered.contains("\u00a7l"));
    }

    @Test
    public void gradientUsesOnlyLegacyColors() {
        String rendered = LegacyMotdText.render("<gradient:#ff0000:#0000ff>abcdef</gradient>");
        assertFalse(rendered.contains("<"));
        assertFalse(rendered.contains(">"));
        assertTrue(rendered.contains("\u00a7"));
        for (int i = 0; i < rendered.length(); i++) {
            if (rendered.charAt(i) == '\u00a7') {
                char code = Character.toLowerCase(rendered.charAt(i + 1));
                assertTrue("unexpected legacy code " + code, "0123456789abcdef".indexOf(code) >= 0);
                i++;
            }
        }
    }

    @Test
    public void rainbowIsRenderedWithLegacyColors() {
        String rendered = LegacyMotdText.render("<rainbow>abcdef</rainbow>");
        assertTrue(rendered.contains("\u00a7"));
        assertFalse(rendered.contains("<rainbow>"));
    }

    @Test
    public void namedColorAndHexAreDownsampled() {
        assertTrue(LegacyMotdText.render("<red>x</red>").contains("\u00a7"));
        assertTrue(LegacyMotdText.render("<#00ff00>x</#00ff00>").contains("\u00a7"));
    }

    @Test
    public void emptyInputIsEmpty() {
        assertTrue(LegacyMotdText.render(null).isEmpty());
        assertTrue(LegacyMotdText.render("").isEmpty());
    }
}
