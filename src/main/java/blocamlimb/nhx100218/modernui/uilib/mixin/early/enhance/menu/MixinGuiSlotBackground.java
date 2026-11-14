package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
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
            beginListClip();
            callbackInfo.cancel();
        }
    }

    @Inject(method = "overlayBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$skipListFade(int top, int bottom, int topAlpha, int bottomAlpha, CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            endListClip();
            callbackInfo.cancel();
        }
    }

    /**
     * 1.7.10 does not scissor {@link GuiSlot}; it relies on the opaque dirt
     * background and the two fade overlays to hide rows that overflow the list.
     * Once those are removed the row text leaks above and below the separators.
     * A vertical scissor restores the 26.3 list mask without clipping the
     * full-width Snooper columns.
     */
    private void beginListClip() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        GuiSlot slot = (GuiSlot) (Object) this;
        int height = slot.bottom - slot.top;
        if (height <= 0 || slot.width <= 0) {
            return;
        }
        int scale = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight)
                .getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, minecraft.displayHeight - (slot.top + height) * scale,
                slot.width * scale, height * scale);
        // 26.3: fully clear above/below the separators, a light 20% scrim
        // between them, and a one-pixel separator at each edge.
        ModernGuiRenderer.fill(0, slot.top, slot.width, slot.bottom, 0x33000000);
        ModernGuiRenderer.fill(0, slot.top, slot.width, slot.top + 1, 0x40FFFFFF);
        ModernGuiRenderer.fill(0, slot.bottom - 1, slot.width, slot.bottom, 0x40FFFFFF);
        ModernGuiRenderer.beginUi();
    }

    @Redirect(method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Tessellator;setColorRGBA_I(II)V"),
            require = 0)
    private void modernui$clearListFade(Tessellator tessellator, int color, int alpha) {
        // GuiSlot paints a four-pixel black fade above and below the list after
        // the cancelled overlayBackground. Keep those quads fully transparent
        // for panorama menus; the explicit one-pixel separators replace them.
        tessellator.setColorRGBA_I(color, usesSharedBackdrop() && color == 0 ? 0 : alpha);
    }

    private static void endListClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static boolean usesSharedBackdrop() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen);
    }
}
