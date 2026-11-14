package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.EfrCompat;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernLoadingBox;
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

        // 26.3 order: resource preparation box -> Loading world + chunk grid ->
        // save box -> title screen.
        if (leavingWorld) {
            ModernLoadingBox.render(minecraft, width, height, I18n.format("menu.savingLevel"));
            minecraft.func_147120_f();
            callbackInfo.cancel();
            return;
        }
        if (ModernLoadingScreen.isWorldLoadRequested() && !(screen instanceof GuiDownloadTerrain)) {
            ModernLoadingBox.render(minecraft, width, height, I18n.format("selectWorld.resource_load"));
            minecraft.func_147120_f();
            callbackInfo.cancel();
            return;
        }

        boolean loadingWorld = minecraft.getIntegratedServer() != null
                || minecraft.theWorld != null
                || screen instanceof GuiDownloadTerrain
                || screen instanceof GuiMultiplayer;
        if (!loadingWorld) {
            return;
        }
        float fraction = progress < 0 ? -1.0F : progress / 100.0F;
        // The pre-world "Loading world" stage has progress -1; keep the vanilla
        // title + bar there and only show the chunk grid once real progress is
        // available (or when GuiDownloadTerrain renders it).
        boolean showChunkMap = progress >= 0;
        if (ModernLoadingScreen.render(minecraft, width, height,
                EfrCompat.snapshot(), fraction, showChunkMap)) {
            callbackInfo.cancel();
        }
    }
}
