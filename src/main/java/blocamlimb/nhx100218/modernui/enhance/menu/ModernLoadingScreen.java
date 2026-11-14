package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;

import org.lwjgl.opengl.GL11;

/**
 * 26.3-style world loading screen.
 *
 * <p>The centre of the screen is the vanilla chunk-status grid: one 2x2 cell
 * per chunk, coloured with {@code LevelLoadingScreen}'s status palette. The
 * title and the 200x2 progress bar follow the same layout constants. No EFR
 * chunk snapshot or ModernUI icon sheet is used here.</p>
 */
public final class ModernLoadingScreen {

    private static final int PROGRESS_BAR_WIDTH = 200;
    private static final int PROGRESS_BAR_HEIGHT = 2;

    private static float smoothedProgress;
    private static boolean rendering;

    private ModernLoadingScreen() {
    }

    /**
     * Renders the ModernUI loading screen.
     *
     * @param mc       client
     * @param width    scaled width
     * @param height   scaled height
     * @param snapshot optional EFR text/progress snapshot
     * @return true when this method drew and the host must cancel its own render
     */
    public static boolean render(Minecraft mc, int width, int height, EfrCompat.Snapshot snapshot) {
        return render(mc, width, height, snapshot, -1.0F, true);
    }

    /**
     * Renders the ModernUI loading screen with an explicit progress value.
     *
     * @param explicitProgress 0..1, or a negative value to read the host state
     */
    public static boolean render(Minecraft mc, int width, int height, EfrCompat.Snapshot snapshot,
            float explicitProgress) {
        return render(mc, width, height, snapshot, explicitProgress, true);
    }

    /**
     * Renders the ModernUI loading screen.
     *
     * @param explicitProgress 0..1, or a negative value to read the host state
     * @param showChunkMap     false while a save is being shut down, so exiting a
     *                         world does not display a chunk-status grid
     */
    public static boolean render(Minecraft mc, int width, int height, EfrCompat.Snapshot snapshot,
            float explicitProgress, boolean showChunkMap) {
        if (!MenuConfig.modernLoadingScreen || rendering) {
            return false;
        }
        rendering = true;
        try {
            int dimension = ModernBackdropRenderer.currentDimension(mc);
            ModernBackdropRenderer.renderLoadingBackground(mc, width, height, dimension);

            setupOrtho(width, height);
            try {
                LoadingText text = resolveText(mc, snapshot);
                float progress = explicitProgress >= 0.0F ? explicitProgress : resolveProgress(mc, snapshot);
                boolean hasProgress = progress >= 0.0F;
                drawForeground(mc, width, height, text, hasProgress,
                        Math.max(0.0F, Math.min(1.0F, progress)),
                        showChunkMap ? LoadingChunkMap.sample(mc) : null);
            } finally {
                restoreOrtho();
            }
            return true;
        } finally {
            rendering = false;
        }
    }

    private static LoadingText resolveText(Minecraft mc, EfrCompat.Snapshot snapshot) {
        String title = snapshot == null ? null : snapshot.title;
        if (title == null || title.isEmpty()) {
            IntegratedServer server = mc.getIntegratedServer();
            title = server != null ? I18n.format("menu.loadingLevel") : I18n.format("multiplayer.downloadingTerrain");
        }
        return new LoadingText(title);
    }

    private static float resolveProgress(Minecraft mc, EfrCompat.Snapshot snapshot) {
        float progress = snapshot == null ? -1.0F : snapshot.progress;
        float spawn = EfrCompat.spawnProgress();
        if (spawn >= 0.0F) {
            progress = Math.max(progress, spawn);
        }
        float chunks = EfrCompat.chunkProgress();
        if (chunks >= 0.0F) {
            progress = Math.max(progress, chunks);
        }
        if (progress < 0.0F) {
            IntegratedServer server = mc.getIntegratedServer();
            if (server != null) {
                progress = server.percentDone / 100.0F;
            }
        }
        return progress;
    }

    private static void drawForeground(Minecraft mc, int width, int height, LoadingText text,
            boolean hasProgress, float targetProgress, LoadingChunkMap.Snapshot chunkMap) {
        int centerX = width / 2;
        int centerY = height / 2;
        int titleY;

        if (chunkMap != null) {
            int mapSize = chunkMap.diameter * LoadingChunkMap.CELL_SIZE;
            int left = centerX - mapSize / 2;
            int top = centerY - mapSize / 2;
            drawChunkMap(chunkMap, left, top);
            titleY = centerY - chunkMap.radius * LoadingChunkMap.CELL_SIZE - 27;
        } else {
            titleY = centerY - 50;
        }

        ModernGuiRenderer.centeredString(mc.fontRenderer, text.title, centerX, titleY, 0xFFFFFFFF);

        if (hasProgress) {
            if (targetProgress < 0.05F && smoothedProgress > 0.5F) {
                smoothedProgress = 0.0F;
            }
            smoothedProgress += (targetProgress - smoothedProgress) * 0.2F;
            if (smoothedProgress < 0.001F && targetProgress < 0.001F) {
                smoothedProgress = 0.0F;
            }
            int barLeft = centerX - PROGRESS_BAR_WIDTH / 2;
            int barTop = titleY + 12;
            ModernGuiRenderer.fill(barLeft, barTop, barLeft + PROGRESS_BAR_WIDTH,
                    barTop + PROGRESS_BAR_HEIGHT, 0xFF000000);
            int filled = Math.round(smoothedProgress * PROGRESS_BAR_WIDTH);
            if (filled > 0) {
                ModernGuiRenderer.fill(barLeft, barTop, barLeft + filled,
                        barTop + PROGRESS_BAR_HEIGHT, 0xFF00FF00);
            }
        }
    }

    private static void drawChunkMap(LoadingChunkMap.Snapshot chunkMap, int left, int top) {
        int cell = LoadingChunkMap.CELL_SIZE;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        for (int z = 0; z < chunkMap.diameter; z++) {
            for (int x = 0; x < chunkMap.diameter; x++) {
                int color = chunkMap.color(x, z);
                float alpha = (color >>> 24 & 255) / 255.0F;
                float red = (color >> 16 & 255) / 255.0F;
                float green = (color >> 8 & 255) / 255.0F;
                float blue = (color & 255) / 255.0F;
                int cellLeft = left + x * cell;
                int cellTop = top + z * cell;
                int cellRight = cellLeft + cell;
                int cellBottom = cellTop + cell;
                GL11.glColor4f(red, green, blue, alpha);
                GL11.glVertex2f(cellLeft, cellBottom);
                GL11.glVertex2f(cellRight, cellBottom);
                GL11.glVertex2f(cellRight, cellTop);
                GL11.glVertex2f(cellLeft, cellTop);
            }
        }
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void setupOrtho(int width, int height) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        ModernGuiRenderer.beginUi();
    }

    private static void restoreOrtho() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private static final class LoadingText {

        private final String title;

        private LoadingText(String title) {
            this.title = title;
        }
    }
}
