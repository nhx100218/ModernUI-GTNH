package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.util.ResourceLocation;

/**
 * Arrow sprites and hit boxes shared by the world and server lists.
 *
 * <p>The 1.7.10 resource-pack selector already ships the four arrows ModernUI
 * needs. Reusing that sheet keeps the mod free of new GUI art and matches the
 * texture players already see when moving resource packs.</p>
 */
public final class ModernMenuArrows {

    /** Vanilla resource-pack selector sheet. */
    public static final ResourceLocation SHEET = new ResourceLocation("textures/gui/resource_packs.png");
    /** Sheet width and height in pixels. */
    public static final float SHEET_SIZE = 256.0F;

    private static final float RIGHT_U = 0.0F;
    private static final float DOWN_U = 64.0F;
    private static final float UP_U = 96.0F;
    private static final float NORMAL_V = 0.0F;
    private static final float HOVER_V = 32.0F;

    private ModernMenuArrows() {
    }

    /** Whether the pointer is inside a square icon. */
    public static boolean overIcon(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    /** Right half of the icon, the join target used by 26.3. */
    public static boolean overRightHalf(double mouseX, double mouseY, int x, int y, int size) {
        return overIcon(mouseX, mouseY, x, y, size) && mouseX >= x + size / 2.0D;
    }

    /** Top-left quarter, the move-up target used by 26.3. */
    public static boolean overTopLeftQuarter(double mouseX, double mouseY, int x, int y, int size) {
        return overIcon(mouseX, mouseY, x, y, size) && mouseX < x + size / 2.0D && mouseY < y + size / 2.0D;
    }

    /** Bottom-left quarter, the move-down target used by 26.3. */
    public static boolean overBottomLeftQuarter(double mouseX, double mouseY, int x, int y, int size) {
        return overIcon(mouseX, mouseY, x, y, size) && mouseX < x + size / 2.0D && mouseY >= y + size / 2.0D;
    }

    /** Adds the 26.3 grey-white selected/hover emphasis over the icon. */
    public static void drawIconShade(int x, int y, int size) {
        ModernGuiRenderer.fill(x, y, x + size, y + size, 0x80D0D0D0);
    }

    /** Right arrow, the quick-join affordance used by the world list. */
    public static void drawJoin(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x, y, size, RIGHT_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }

    /**
     * Server join arrow. 26.3 places the server join glyph in the right half
     * (x=16..29), while the world-list join glyph is centred (x=10..23).
     */
    public static void drawServerJoin(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x + 6, y, size, RIGHT_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }

    /**
     * Up arrow. The vanilla resource-pack sheet has the glyph in the right half
     * of its 32x32 cell; shifting the whole sprite left puts the original glyph
     * into the 26.3 top-left hit box without redrawing it.
     */
    public static void drawMoveUp(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x - 15, y, size, UP_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }

    /** Down arrow, same original sprite shifted into the bottom-left quarter. */
    public static void drawMoveDown(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x - 15, y, size, DOWN_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }
}
