package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuConfig;
import blocamlimb.nhx100218.modernui.enhance.menu.MenuCursor;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernBackdropRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Gives the menu screens one shared translucent panorama instead of the dirt
 * background each screen draws for itself.
 *
 * <p>The whitelist is deliberate. In-game screens keep the world backdrop, and
 * the main menu keeps its own panorama. Loading screens use their dimension
 * backdrop instead.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenBackground {

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$drawMenuPanorama(int tint, CallbackInfo callbackInfo) {
        if (!MenuConfig.globalPanorama) {
            return;
        }
        GuiScreen screen = (GuiScreen) (Object) this;
        if (!ModernMenuScreens.usesSharedBackdrop(screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        if (minecraft.theWorld != null) {
            // In a save these screens must stay transparent over the world, not
            // swap in the title-screen panorama.
            callbackInfo.cancel();
            return;
        }
        ModernBackdropRenderer.renderMenuBackground(minecraft, screen.width, screen.height);
        if (ModernMenuScreens.usesMenuFrame(screen)) {
            ModernMenuFrame.drawScrim(0, 35, screen.width, screen.height - 35);
        }
        callbackInfo.cancel();
    }

    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$transparentWorldBackground(int tint, CallbackInfo callbackInfo) {
        if (!MenuConfig.globalPanorama) {
            return;
        }
        GuiScreen screen = (GuiScreen) (Object) this;
        if (!ModernMenuScreens.usesSharedBackdrop(screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.theWorld != null) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), require = 0)
    private void modernui$drawSubScreenFrame(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        GuiScreen screen = (GuiScreen) (Object) this;
        MenuCursor.update(screen, mouseX, mouseY);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!MenuConfig.globalPanorama || minecraft == null || minecraft.theWorld != null
                || !ModernMenuScreens.usesMenuFrame(screen)) {
            return;
        }
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawFrame(screen.width, screen.height);
    }
}
