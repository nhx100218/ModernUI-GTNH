package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics;

/**
 * Panorama and dimension backdrops for menus and loading screens.
 *
 * <p>The panorama is drawn as a real perspective cubemap. Loading screens swap in
 * a nether or end portal when the destination dimension is known. Everything is
 * immediate-mode geometry so Angelica's SDL-GPU backend can replay it.</p>
 */
public final class ModernBackdropRenderer {

    private static final ResourceLocation[] PANORAMA = new ResourceLocation[] {
            new ResourceLocation("textures/gui/title/background/panorama_0.png"),
            new ResourceLocation("textures/gui/title/background/panorama_1.png"),
            new ResourceLocation("textures/gui/title/background/panorama_2.png"),
            new ResourceLocation("textures/gui/title/background/panorama_3.png"),
            new ResourceLocation("textures/gui/title/background/panorama_4.png"),
            new ResourceLocation("textures/gui/title/background/panorama_5.png")
    };
    private static final ResourceLocation NETHER_PORTAL = new ResourceLocation("textures/blocks/portal.png");
    private static final ResourceLocation END_SKY = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL = new ResourceLocation("textures/entity/end_portal.png");

    private ModernBackdropRenderer() {
    }

    /** Draws the shared panorama behind a menu screen. */
    public static void renderMenuBackground(Minecraft mc, int width, int height) {
        setupOrtho(width, height);
        try {
            if (MenuConfig.globalPanorama) {
                drawPanorama(mc, width, height, 0.92F);
            } else {
                ModernGuiRenderer.fill(0, 0, width, height, 0xFF101014);
            }
            drawTint(width, height, 0xC0101014);
            // The host list draws text and icons after this callback. Leave the
            // fixed-pipeline state in the shape vanilla expects.
            ModernGuiRenderer.beginUi();
        } finally {
            restoreOrtho();
        }
    }

    /** Draws a loading backdrop for the current destination dimension. */
    public static void renderLoadingBackground(Minecraft mc, int width, int height, int dimension) {
        setupOrtho(width, height);
        try {
            if (MenuConfig.dimensionPortalBackground && dimension == -1) {
                drawNetherPortal(mc, width, height);
            } else if (MenuConfig.dimensionPortalBackground && dimension == 1) {
                drawEndPortal(mc, width, height);
            } else if (MenuConfig.globalPanorama) {
                drawPanorama(mc, width, height, 0.88F);
            } else {
                ModernGuiRenderer.fill(0, 0, width, height, 0xFF101014);
            }
            drawTint(width, height, 0xB8101014);
            ModernGuiRenderer.beginUi();
        } finally {
            restoreOrtho();
        }
    }

    /** Destination dimension from the live client world, defaulting to the overworld. */
    public static int currentDimension(Minecraft mc) {
        if (mc.theWorld != null && mc.theWorld.provider != null) {
            return mc.theWorld.provider.dimensionId;
        }
        if (mc.thePlayer != null) {
            return mc.thePlayer.dimension;
        }
        return 0;
    }

    private static void drawNetherPortal(Minecraft mc, int width, int height) {
        ModernGuiRenderer.fill(0, 0, width, height, 0xFF1A071E);
        float drift = (Minecraft.getSystemTime() % 20000L) / 20000.0F;
        int tiles = 3;
        float tileWidth = width / (float) tiles;
        float tileHeight = height / (float) tiles;
        for (int row = 0; row < tiles; row++) {
            for (int column = 0; column < tiles; column++) {
                float u0 = drift + column * 0.31F;
                float v0 = drift * 1.7F + row * 0.29F;
                ModernGuiRenderer.textureRect(NETHER_PORTAL, column * tileWidth, row * tileHeight,
                        (column + 1) * tileWidth, (row + 1) * tileHeight,
                        u0, v0, u0 + 1.0F, v0 + 1.0F, 0xFFB45CFF);
            }
        }
        ModernGuiRenderer.fill(0, 0, width, height, 0x4A2A0A3D);
    }

    private static void drawEndPortal(Minecraft mc, int width, int height) {
        ModernGuiRenderer.fill(0, 0, width, height, 0xFF03030A);
        ModernGuiRenderer.textureRect(END_SKY, 0.0F, 0.0F, width, height, 0.0F, 0.0F, 2.0F, 2.0F,
                0xFF6D6D8A);
        float drift = (Minecraft.getSystemTime() % 30000L) / 30000.0F;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        ModernGuiRenderer.textureRect(END_PORTAL, 0.0F, 0.0F, width, height,
                drift, drift * 0.7F, drift + 2.0F, drift * 0.7F + 2.0F, 0xCCB5E7FF);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        ModernGuiRenderer.fill(0, 0, width, height, 0x22000000);
    }

    private static void drawPanorama(Minecraft mc, int width, int height, float alpha) {
        GlStateDiagnostics.pushAttrib(GL11.GL_ALL_ATTRIB_BITS, "ModernBackdropRenderer#panorama");
        try {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            Project.gluPerspective(120.0F, (float) width / Math.max(1, height), 0.05F, 10.0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            float seconds = (Minecraft.getSystemTime() % 1_000_000L) / 1000.0F;
            // Match the vanilla panorama camera (20 ticks/s) and its final 90-degree
            // screen-space roll. Vanilla renders into a 256x256 target and rotates the
            // blit; this renderer draws directly, so the roll has to be applied here.
            GL11.glRotatef((float) Math.sin(seconds / 20.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-seconds * 2.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
            for (int face = 0; face < 6; face++) {
                GL11.glPushMatrix();
                if (face == 1) {
                    GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                } else if (face == 2) {
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                } else if (face == 3) {
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                } else if (face == 4) {
                    GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                } else if (face == 5) {
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                }
                mc.getTextureManager().bindTexture(PANORAMA[face]);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0.0F, 0.0F);
                GL11.glVertex3f(-1.0F, -1.0F, 1.0F);
                GL11.glTexCoord2f(1.0F, 0.0F);
                GL11.glVertex3f(1.0F, -1.0F, 1.0F);
                GL11.glTexCoord2f(1.0F, 1.0F);
                GL11.glVertex3f(1.0F, 1.0F, 1.0F);
                GL11.glTexCoord2f(0.0F, 1.0F);
                GL11.glVertex3f(-1.0F, 1.0F, 1.0F);
                GL11.glEnd();
                GL11.glPopMatrix();
            }

            GL11.glDepthMask(true);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
        } finally {
            GlStateDiagnostics.popAttrib("ModernBackdropRenderer#panorama");
        }
    }

    private static void drawTint(int width, int height, int argb) {
        int alpha = argb >>> 24 & 255;
        int rgb = argb & 0xFFFFFF;
        int top = (Math.min(255, alpha + 24) & 255) << 24 | rgb;
        int bottom = (Math.min(255, alpha + 48) & 255) << 24 | rgb;
        ModernGuiRenderer.fillVerticalGradient(0, 0, width, height, top, bottom);
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
}
