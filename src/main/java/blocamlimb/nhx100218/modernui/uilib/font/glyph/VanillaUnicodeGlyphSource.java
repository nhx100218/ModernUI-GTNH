package blocamlimb.nhx100218.modernui.uilib.font.glyph;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;

/**
 * Bitmap glyph source backed by Minecraft's own Unicode font pages.
 *
 * <p>GTNH mods use private-use characters (for example {@code \uE020} for a subscript question
 * mark) and override {@code assets/minecraft/textures/font/unicode_page_e0.png} plus
 * {@code assets/minecraft/font/glyph_sizes.bin}. The vanilla renderer draws those glyphs from the
 * bitmap page. ModernUI's AWT font pipeline cannot see them, so private-use characters would fall
 * back to an unrelated placeholder from a system font.</p>
 *
 * <p>This source reads the same resource-manager assets the vanilla renderer uses, converts the
 * 16x16 cell into a ModernUI atlas glyph, and reports the same advance. Only private-use
 * characters are handled; ordinary text keeps using the AWT pipeline.</p>
 */
public final class VanillaUnicodeGlyphSource {

    private static final int PAGE_SIZE = 256;
    private static final int CELL_SIZE = 16;
    private static final int GLYPH_SIZE_BYTES = 65536;
    private static final int VANILLA_FONT_SIZE = 9;
    /** The vanilla unicode cell is 16x16 but is drawn as an 8x8 quad at the default font size. */
    private static final int VANILLA_CELL_DISPLAY_SIZE = 8;
    /** Baseline within the 16x16 unicode cell (the vanilla 8px line baseline is 7px). */
    private static final int VANILLA_CELL_BASELINE = 14;

    private static final Map<Integer, BufferedImage> PAGES = new ConcurrentHashMap<Integer, BufferedImage>();

    private static volatile byte[] glyphSizes;
    private static volatile byte[] testGlyphSizes;
    private static volatile BufferedImage testPage;

    private VanillaUnicodeGlyphSource() {}

    /** Test-only resource injection; avoids initialising the Minecraft resource manager. */
    static void setTestResources(byte[] sizes, BufferedImage page) {
        testGlyphSizes = sizes;
        testPage = page;
    }

    static void clearTestResources() {
        testGlyphSizes = null;
        testPage = null;
    }

    /** Drops resource-manager caches after a pack or language reload. */
    public static void clearCache() {
        glyphSizes = null;
        PAGES.clear();
    }

    /**
     * Whether the character is a private-use glyph supplied by the active resource pack.
     *
     * @param codepoint character to test
     * @return true when the unicode page and size table contain a glyph
     */
    public static boolean hasGlyph(int codepoint) {
        if (codepoint < 0xE000 || codepoint > 0xF8FF) {
            return false;
        }
        byte[] sizes = glyphSizes();
        if (sizes == null || (sizes[codepoint] & 0xFF) == 0) {
            return false;
        }
        return loadPage(codepoint >> 8) != null;
    }

    /**
     * Returns the vanilla advance for a private-use glyph at the requested display size.
     *
     * @param codepoint character
     * @param fontSizePx display font size in UI pixels
     * @return advance in UI pixels, or 0 when the glyph is unavailable
     */
    public static double advance(int codepoint, double fontSizePx) {
        byte[] sizes = glyphSizes();
        if (sizes == null) {
            return 0.0D;
        }
        int value = sizes[codepoint] & 0xFF;
        if (value == 0) {
            return 0.0D;
        }
        int left = value >>> 4;
        int right = value & 15;
        double vanillaAdvance = (right + 1 - left) / 2.0D + 1.0D;
        return vanillaAdvance * fontSizePx / VANILLA_FONT_SIZE;
    }

    /**
     * Generates a ModernUI atlas glyph from the vanilla unicode page.
     *
     * @param task codepoint glyph task
     * @return glyph result, or null when the page is unavailable
     */
    public static GlyphGenerationResult generate(GlyphGenerationTask task) {
        int codepoint = task.getCodepoint();
        byte[] sizes = glyphSizes();
        if (sizes == null) {
            return null;
        }
        int value = sizes[codepoint] & 0xFF;
        if (value == 0) {
            return null;
        }
        BufferedImage page = loadPage(codepoint >> 8);
        if (page == null) {
            return null;
        }

        int glyphSize = task.getGlyphSize();
        int cellX = (codepoint & 15) * CELL_SIZE;
        int cellY = ((codepoint & 255) / 16) * CELL_SIZE;
        int minX = CELL_SIZE;
        int minY = CELL_SIZE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < CELL_SIZE; y++) {
            for (int x = 0; x < CELL_SIZE; x++) {
                if ((page.getRGB(cellX + x, cellY + y) >>> 24) > 8) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        int left = value >>> 4;
        int right = value & 15;
        float vanillaAdvance = (right + 1 - left) / 2.0F + 1.0F;
        float atlasAdvance = vanillaAdvance * glyphSize / (float) VANILLA_FONT_SIZE;
        float cellScale = (float) glyphSize * VANILLA_CELL_DISPLAY_SIZE
                / ((float) VANILLA_FONT_SIZE * CELL_SIZE);
        float lineBaseline = VANILLA_CELL_BASELINE * cellScale;

        if (minX > maxX) {
            GlyphInfo empty = new GlyphInfo(codepoint, glyphSize, glyphSize, atlasAdvance, lineBaseline,
                    Math.max(0.0F, glyphSize - lineBaseline), 0.0F, 0.0F, 0.0F, 1, 1, 0, 0,
                    Math.round(lineBaseline), 0, 0, false, false);
            return new GlyphGenerationResult(task.getToken(),
                    new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), empty);
        }

        int inkWidth = Math.max(1, Math.round((maxX - minX + 1) * cellScale));
        int inkHeight = Math.max(1, Math.round((maxY - minY + 1) * cellScale));
        int bearingX = Math.round(minX * cellScale);
        int bearingY = Math.round(minY * cellScale - lineBaseline);
        int padding = Math.max(0, Math.min(32, FontConfig.glyphInkPadding));
        int atlasBaselineX = Math.max(0, padding - bearingX);
        int atlasBaselineY = Math.max(0, padding - bearingY);
        int inkLeftInSlot = atlasBaselineX + bearingX;
        int inkTopInSlot = atlasBaselineY + bearingY;
        int slotWidth = Math.max(1, inkLeftInSlot + inkWidth + padding);
        int slotHeight = Math.max(1, inkTopInSlot + inkHeight + padding);

        BufferedImage slot = new BufferedImage(slotWidth, slotHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = slot.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int dx = inkLeftInSlot;
            int dy = inkTopInSlot;
            graphics.drawImage(page, dx, dy, dx + inkWidth, dy + inkHeight, cellX + minX, cellY + minY,
                    cellX + maxX + 1, cellY + maxY + 1, null);
        } finally {
            graphics.dispose();
        }

        GlyphInfo info = new GlyphInfo(codepoint, glyphSize, glyphSize, atlasAdvance, lineBaseline,
                Math.max(0.0F, glyphSize - lineBaseline), 0.0F, inkWidth, inkHeight, slotWidth, slotHeight,
                atlasBaselineX, atlasBaselineY, Math.round(lineBaseline), bearingX, bearingY, true, false);
        return new GlyphGenerationResult(task.getToken(), slot, info);
    }

    private static byte[] glyphSizes() {
        byte[] test = testGlyphSizes;
        if (test != null) {
            return test;
        }
        byte[] current = glyphSizes;
        if (current != null) {
            return current;
        }
        synchronized (VanillaUnicodeGlyphSource.class) {
            if (glyphSizes != null) {
                return glyphSizes;
            }
            try {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft == null || minecraft.getResourceManager() == null) {
                    return null;
                }
                IResource resource = minecraft.getResourceManager()
                        .getResource(new ResourceLocation("font/glyph_sizes.bin"));
                try (InputStream input = resource.getInputStream()) {
                    byte[] data = new byte[GLYPH_SIZE_BYTES];
                    int offset = 0;
                    while (offset < data.length) {
                        int read = input.read(data, offset, data.length - offset);
                        if (read < 0) {
                            break;
                        }
                        offset += read;
                    }
                    if (offset == data.length) {
                        glyphSizes = data;
                    }
                }
            } catch (IOException | RuntimeException | LinkageError ignored) {
                glyphSizes = null;
            }
            return glyphSizes;
        }
    }

    private static BufferedImage loadPage(int page) {
        BufferedImage test = testPage;
        if (test != null) {
            return test;
        }
        BufferedImage cached = PAGES.get(Integer.valueOf(page));
        if (cached != null) {
            return cached;
        }
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                return null;
            }
            ResourceLocation location = new ResourceLocation(
                    String.format("textures/font/unicode_page_%02x.png", Integer.valueOf(page)));
            IResource resource = minecraft.getResourceManager().getResource(location);
            try (InputStream input = resource.getInputStream()) {
                BufferedImage image = ImageIO.read(input);
                if (image != null && image.getWidth() == PAGE_SIZE && image.getHeight() == PAGE_SIZE) {
                    PAGES.put(Integer.valueOf(page), image);
                    return image;
                }
            }
        } catch (IOException | RuntimeException | LinkageError ignored) {
            return null;
        }
        return null;
    }
}
