package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Keeps the shared menu panorama visible through vanilla selection lists.
 *
 * <p>{@link GuiSlot} normally paints an opaque {@code optionsBackground} texture
 * over the whole list and fades the top/bottom with the same dirt texture. Both
 * cover the panorama that {@code MixinGuiScreenBackground} just drew. Only the
 * whitelisted menu screens are affected; in-game and mod lists keep their
 * vanilla background.</p>
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotBackground {

    @Inject(method = "drawContainerBackground", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void modernui$skipOpaqueListBackground(Tessellator tessellator, CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "overlayBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$skipListFade(int top, int bottom, int topAlpha, int bottomAlpha, CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            callbackInfo.cancel();
        }
    }

    private static boolean usesSharedBackdrop() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen);
    }
}
