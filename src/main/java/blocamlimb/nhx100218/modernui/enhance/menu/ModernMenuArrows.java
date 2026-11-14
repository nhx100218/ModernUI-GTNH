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

    /** Darkens the icon so the white arrows stay readable. */
    public static void drawIconShade(int x, int y, int size) {
        ModernGuiRenderer.fill(x, y, x + size, y + size, 0xA0101014);
    }

    /** Right arrow, the quick-join affordance. */
    public static void drawJoin(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x, y, size, RIGHT_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }

    /** Up arrow, the server move-up affordance. */
    public static void drawMoveUp(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x, y, size, UP_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }

    /** Down arrow, the server move-down affordance. */
    public static void drawMoveDown(int x, int y, int size, boolean hovered) {
        ModernGuiRenderer.sprite(SHEET, x, y, size, DOWN_U, hovered ? HOVER_V : NORMAL_V, SHEET_SIZE,
                0xFFFFFFFF);
    }
}
