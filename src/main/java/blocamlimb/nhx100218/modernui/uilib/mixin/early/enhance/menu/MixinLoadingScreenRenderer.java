package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.EfrCompat;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernLoadingScreen;

/**
 * Covers the vanilla loading renderer used while a local server starts.
 *
 * <p>EFR has its own wrapper around this method. When EFR owns the session its
 * mixins draw the screen, so this one steps aside. Without EFR this is what keeps
 * the dirt progress screen from appearing between the menu and the world.</p>
 */
@Mixin(value = LoadingScreenRenderer.class, priority = 1500)
public abstract class MixinLoadingScreenRenderer {

    @Inject(method = "setLoadingProgress", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$drawLoadingScreen(int progress, CallbackInfo callbackInfo) {
        if (EfrCompat.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        GuiScreen screen = minecraft.currentScreen;
        boolean leavingWorld = screen instanceof GuiIngameMenu;
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth,
                minecraft.displayHeight);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();

        boolean loadingWorld = ModernLoadingScreen.isWorldLoadRequested()
                || minecraft.getIntegratedServer() != null
                || minecraft.theWorld != null
                || screen instanceof GuiDownloadTerrain
                || screen instanceof GuiSelectWorld
                || screen instanceof GuiMultiplayer
                || leavingWorld;
        if (!loadingWorld) {
            return;
        }
        float fraction = progress < 0 ? -1.0F : progress / 100.0F;
        // The status titles (resource preparation / saving world) are resolved
        // inside ModernLoadingScreen so they share the same panorama layout as
        // "Loading world" + chunk grid.
        boolean showChunkMap = progress >= 0 && !leavingWorld;
        if (ModernLoadingScreen.render(minecraft, width, height,
                EfrCompat.snapshot(), fraction, showChunkMap)) {
            callbackInfo.cancel();
        }
    }
}
