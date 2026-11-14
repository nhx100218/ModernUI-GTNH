package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.integrated.IntegratedServer;

import org.lwjgl.opengl.GL11;

/**
 * Modern loading screen: dimension backdrop, centred mark, title and progress bar.
 *
 * <p>The layout follows the 26.3 loading overlay: one large centred mark, a thin
 * progress bar, and a quiet title underneath. EFR's chunk grid and mixed state are
 * intentionally not reproduced. EFR progress and text are read through
 * {@link EfrCompat} when present.</p>
 */
public final class ModernLoadingScreen {

    private static final ResourceLocation ICON =
            new ResourceLocation("modernui", "textures/gui/new_icon_white.png");

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
     * @param snapshot EFR snapshot, may be null
     * @return true when this method drew and the host must cancel its own render
     */
    public static boolean render(Minecraft mc, int width, int height, EfrCompat.Snapshot snapshot) {
        return render(mc, width, height, snapshot, -1.0F);
    }

    /**
     * Renders the ModernUI loading screen with an explicit progress value.
     *
     * @param explicitProgress 0..1, or a negative value to read the host state
     */
    public static boolean render(Minecraft mc, int width, int height, EfrCompat.Snapshot snapshot,
            float explicitProgress) {
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
                drawForeground(mc, width, height, text, hasProgress, Math.max(0.0F, Math.min(1.0F, progress)));
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
        String subtitle = snapshot == null ? null : snapshot.subtitle;
        IntegratedServer server = mc.getIntegratedServer();
        if (title == null || title.isEmpty()) {
            title = server != null ? I18n.format("menu.loadingLevel") : I18n.format("multiplayer.downloadingTerrain");
        }
        if (subtitle == null) {
            subtitle = server != null ? I18n.format("menu.generatingTerrain") : "";
        }
        return new LoadingText(title, subtitle);
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
            boolean hasProgress, float targetProgress) {
        int centerX = width / 2;
        int logoSize = (int) Math.max(64.0F, Math.min(176.0F, Math.min(width * 0.72F, height) * 0.24F));
        int logoX = centerX - logoSize / 2;
        int logoY = height / 2 - logoSize - 18;
        float pulse = 0.985F + 0.015F * (float) Math.sin(Minecraft.getSystemTime() / 650.0D);

        GL11.glPushMatrix();
        GL11.glTranslatef(logoX + logoSize / 2.0F, logoY + logoSize / 2.0F, 0.0F);
        GL11.glScalef(pulse, pulse, 1.0F);
        ModernGuiRenderer.textureRect(ICON, -logoSize / 2.0F, -logoSize / 2.0F, logoSize / 2.0F,
                logoSize / 2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0xF2FFFFFF);
        GL11.glPopMatrix();

        int titleY = height / 2 + 4;
        ModernGuiRenderer.centeredString(mc.fontRenderer, text.title, centerX, titleY, 0xFFFFFFFF);
        if (text.subtitle != null && !text.subtitle.isEmpty()) {
            ModernGuiRenderer.centeredString(mc.fontRenderer, text.subtitle, centerX, titleY + 18, 0xFFB7B7C4);
        }

        int barWidth = (int) Math.max(200.0F, Math.min(width * 0.46F, logoSize * 2.4F));
        int barHeight = 4;
        int barLeft = centerX - barWidth / 2;
        int barTop = titleY + 42;
        ModernGuiRenderer.fill(barLeft - 1, barTop - 1, barLeft + barWidth + 1, barTop + barHeight + 1,
                0x50000000);
        ModernGuiRenderer.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, 0x30FFFFFF);
        if (hasProgress) {
            smoothedProgress += (targetProgress - smoothedProgress) * 0.2F;
            if (smoothedProgress < 0.001F && targetProgress < 0.001F) {
                smoothedProgress = 0.0F;
            }
            int filled = Math.round(smoothedProgress * barWidth);
            ModernGuiRenderer.fill(barLeft, barTop, barLeft + filled, barTop + barHeight, 0xFFFFFFFF);
            ModernGuiRenderer.fill(barLeft, barTop, barLeft + barWidth, barTop + 1, 0x40FFFFFF);
        } else {
            float phase = (Minecraft.getSystemTime() % 1600L) / 1600.0F;
            int segment = Math.max(32, barWidth / 4);
            int start = barLeft - segment + (int) (phase * (barWidth + segment));
            int from = Math.max(barLeft, start);
            int to = Math.min(barLeft + barWidth, start + segment);
            if (to > from) {
                ModernGuiRenderer.fill(from, barTop, to, barTop + barHeight, 0xCCFFFFFF);
            }
        }
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
        private final String subtitle;

        private LoadingText(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}
