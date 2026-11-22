package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.EfrCompat;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernLoadingScreen;

/**
 * Replaces the vanilla terrain-download screen with the ModernUI loading screen.
 *
 * <p>Priority sits above EFR's own HEAD injection. When both mods are present,
 * this cancel runs first, so EFR does not start its chunk-grid renderer for the
 * same frame.</p>
 */
@Mixin(value = GuiDownloadTerrain.class, priority = 1500)
public abstract class MixinGuiDownloadTerrain {

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$drawLoadingScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        GuiDownloadTerrain screen = (GuiDownloadTerrain) (Object) this;
        if (ModernLoadingScreen.render(minecraft, screen.width, screen.height, EfrCompat.snapshot())) {
            callbackInfo.cancel();
        }
    }
}
