package blocamlimb.nhx100218.modernui.enhance.menu;

/**
 * Shared 26.3 menu frame: a 70%-transparent list scrim and the two-pixel
 * header/footer separators.
 *
 * <p>The separator half-tones follow the vanilla 26.3 textures: the header is
 * a translucent white line over a black line, the footer is black over
 * translucent white. Keeping this in one place makes lists and option
 * sub-screens agree.</p>
 */
public final class ModernMenuFrame {

    /** 30% opaque black = 70% transparent. */
    public static final int SCRIM = 0x4D000000;
    /** Black half of a separator. */
    public static final int SEPARATOR_BLACK = 0xFF000000;
    /** Translucent white/grey half of a separator. */
    public static final int SEPARATOR_GREY = 0x33FFFFFF;

    private ModernMenuFrame() {
    }

    /**
     * Restores the dark world mask used behind in-game option screens. This is
     * separate from the list scrim: the whole world view must be dimmed even
     * when the screen has no list.
     */
    public static void drawWorldMask(int width, int height) {
        ModernGuiRenderer.beginUi();
        ModernGuiRenderer.fillVerticalGradient(0, 0, width, height, 0x8C101014, 0xA0101014);
        ModernGuiRenderer.beginUi();
    }

    /** Draws only the inner list tint, before rows. */
    public static void drawScrim(int left, int top, int right, int bottom) {
        // fill() relies on blending. In-world screens arrive with blend disabled,
        // which used to turn the 70%-transparent scrim into an opaque black box.
        ModernGuiRenderer.beginUi();
        ModernGuiRenderer.fill(left, top, right, bottom, SCRIM);
        ModernGuiRenderer.beginUi();
    }

    /** Draws the header and footer separators, after rows. */
    public static void drawSeparators(int left, int top, int right, int bottom) {
        if (right <= left || bottom - top < 4) {
            return;
        }
        ModernGuiRenderer.beginUi();
        // Header: grey over black.
        ModernGuiRenderer.fill(left, top, right, top + 1, SEPARATOR_GREY);
        ModernGuiRenderer.fill(left, top + 1, right, top + 2, SEPARATOR_BLACK);
        // Footer: black over grey.
        ModernGuiRenderer.fill(left, bottom - 2, right, bottom - 1, SEPARATOR_BLACK);
        ModernGuiRenderer.fill(left, bottom - 1, right, bottom, SEPARATOR_GREY);
        ModernGuiRenderer.beginUi();
    }

    /** Draws a full-width frame for sub-screens that have no selection list. */
    public static void drawFrame(int width, int height) {
        int top = 33;
        int bottom = height - 33;
        if (bottom - top < 4) {
            return;
        }
        drawSeparators(0, top, width, bottom);
    }
}
