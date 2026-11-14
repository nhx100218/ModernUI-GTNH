package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.achievement.GuiAchievements;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuConfig;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernBackdropRenderer;

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
        if (!usesSharedBackdrop(screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        ModernBackdropRenderer.renderMenuBackground(minecraft, screen.width, screen.height);
        callbackInfo.cancel();
    }

    private static boolean usesSharedBackdrop(GuiScreen screen) {
        return screen instanceof GuiSelectWorld
                || screen instanceof GuiMultiplayer
                || screen instanceof GuiCreateWorld
                || screen instanceof GuiScreenResourcePacks
                || screen instanceof GuiOptions
                || screen instanceof GuiVideoSettings
                || screen instanceof GuiControls
                || screen instanceof GuiLanguage
                || screen instanceof GuiSnooper
                || screen instanceof GuiAchievements;
    }
}
