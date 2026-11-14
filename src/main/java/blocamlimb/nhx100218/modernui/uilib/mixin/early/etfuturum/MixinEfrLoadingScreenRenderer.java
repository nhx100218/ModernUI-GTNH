package blocamlimb.nhx100218.modernui.uilib.mixin.early.etfuturum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.EfrCompat;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernLoadingScreen;

/**
 * Takes over EFR's loading renderer when its session is active.
 *
 * <p>The target class only exists with EFR installed; the early mixin list adds
 * this class only after a classpath probe. All EFR state is read through
 * {@link EfrCompat}, so this class has no EFR constant-pool entry.</p>
 */
@Mixin(targets = "ganymedes01.etfuturum.client.loading.LoadingScreenRenderer", remap = false, priority = 1500)
public abstract class MixinEfrLoadingScreenRenderer {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$renderLoadingScreen(CallbackInfo callbackInfo) {
        if (!EfrCompat.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        int width;
        int height;
        if (minecraft.currentScreen != null) {
            width = minecraft.currentScreen.width;
            height = minecraft.currentScreen.height;
        } else {
            ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth,
                    minecraft.displayHeight);
            width = resolution.getScaledWidth();
            height = resolution.getScaledHeight();
        }
        if (ModernLoadingScreen.render(minecraft, width, height, EfrCompat.snapshot())) {
            callbackInfo.cancel();
        }
    }
}
