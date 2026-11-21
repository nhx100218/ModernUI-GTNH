package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
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

    @Shadow
    protected boolean field_148163_i;

    @Inject(method = "getListWidth", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$matchWorldSelectionWidth(CallbackInfoReturnable<Integer> callbackInfo) {
        // Singleplayer is deliberately narrower than the server selection.
        // The server list base width is 305; 271 removes another 20px while
        // keeping the row and icon left-aligned inside the frame.
        if (getClass().getName().equals("net.minecraft.client.gui.GuiSelectWorld$List")) {
            callbackInfo.setReturnValue(Integer.valueOf(271));
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), require = 0)
    private void modernui$topAlignList(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            // GuiSlot centres content when it is shorter than the viewport. 26.3
            // keeps the list anchored to the top.
            field_148163_i = false;
        }
    }

    @Inject(method = "drawContainerBackground", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private void modernui$skipOpaqueListBackground(Tessellator tessellator, CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            if (usesListScrim()) {
                beginListClip();
            }
            callbackInfo.cancel();
        }
    }

    @Inject(method = "overlayBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$skipListFade(int top, int bottom, int topAlpha, int bottomAlpha, CallbackInfo callbackInfo) {
        if (!usesSharedBackdrop()) {
            return;
        }
        // Iris lists keep the call so its tessellator flush and GL-state restore
        // still run; the redirect below makes the fade colours transparent.
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && ModernMenuScreens.isIrisListScreen(minecraft.currentScreen)) {
            endListClip();
            return;
        }
        endListClip();
        callbackInfo.cancel();
    }

    @Redirect(method = "overlayBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Tessellator;setColorRGBA_I(II)V"),
            require = 0)
    private void modernui$transparentIrisFade(Tessellator tessellator, int color, int alpha) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && ModernMenuScreens.isIrisListScreen(minecraft.currentScreen)) {
            tessellator.setColorRGBA_I(color, 0);
            return;
        }
        tessellator.setColorRGBA_I(color, alpha);
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
        int listLeft = slot.left;
        int listRight = slot.left + slot.width;
        int clipTop = slot.top + 2;
        int clipBottom = slot.bottom - 2;
        int clipHeight = clipBottom - clipTop;
        if (clipHeight <= 0) {
            return;
        }
        ModernGuiRenderer.beginUi();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(listLeft * scale, minecraft.displayHeight - (clipTop + clipHeight) * scale,
                slot.width * scale, clipHeight * scale);
        ModernMenuFrame.drawScrim(listLeft, clipTop, listRight, clipBottom);
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), require = 0)
    private void modernui$drawListSeparators(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        if (!usesListScrim()) {
            return;
        }
        GuiSlot slot = (GuiSlot) (Object) this;
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawSeparators(slot.left, slot.top, slot.left + slot.width, slot.bottom);
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

    @Redirect(method = "drawSelectionBox",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_I(I)V"),
            require = 0)
    private void modernui$whiteSelectionOutline(Tessellator tessellator, int color) {
        // 8421504 is the grey outline around the selected world/server. 26.3
        // uses a pure white outer edge and keeps the inner shadow opaque.
        tessellator.setColorOpaque_I(usesSharedBackdrop() && color == 8421504 ? 0xFFFFFF : color);
    }

    @Redirect(method = "drawSelectionBox",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiSlot;getListWidth()I",
                    ordinal = 1),
            require = 0)
    private int modernui$widenServerSelectionRight(GuiSlot slot) {
        int width = slot.getListWidth();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.currentScreen instanceof GuiMultiplayer) {
            // Only the right edge moves: the left edge stays aligned with the
            // server icon, while Forge's compatibility marker gets 6px of room.
            return width + 12;
        }
        return width;
    }

    private static void endListClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static boolean usesSharedBackdrop() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen);
    }

    private static boolean usesListScrim() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesListScrim(minecraft.currentScreen);
    }
}
