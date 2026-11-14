package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

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
     * Up arrow, drawn in the top-left quarter of the server icon.
     *
     * <p>26.3 uses dedicated move sprites on the left half; the vanilla 1.7.10
     * resource-pack sheet has its up/down glyphs on the right half, which made
     * the old hit box and the visible arrow disagree. Drawing the triangle at
     * the hit box keeps them aligned.</p>
     */
    public static void drawMoveUp(int x, int y, int size, boolean hovered) {
        int centerX = x + size / 4;
        int top = y + 5;
        drawTriangle(centerX, top, 6, 7, hovered ? 0xFFFFFFFF : 0xFFE8E8E8);
    }

    /** Down arrow, drawn in the bottom-left quarter of the server icon. */
    public static void drawMoveDown(int x, int y, int size, boolean hovered) {
        int centerX = x + size / 4;
        int bottom = y + size - 5;
        drawTriangle(centerX, bottom, 6, -7, hovered ? 0xFFFFFFFF : 0xFFE8E8E8);
    }

    private static void drawTriangle(int centerX, int tipY, int halfWidth, int height, int argb) {
        int baseY = tipY + height;
        // Dark outline first, then the white face, so the glyph stays readable
        // over both bright and dark server icons.
        drawTriangleFace(centerX, tipY + 1, halfWidth + 1, baseY + 1, 0xA0000000);
        drawTriangleFace(centerX, tipY, halfWidth, baseY, argb);
    }

    private static void drawTriangleFace(int centerX, int tipY, int halfWidth, int baseY, int argb) {
        float alpha = (argb >>> 24 & 255) / 255.0F;
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(centerX, tipY);
        GL11.glVertex2f(centerX + halfWidth, baseY);
        GL11.glVertex2f(centerX - halfWidth, baseY);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
