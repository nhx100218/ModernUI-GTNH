package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Gives Forge's {@code GuiScrollingList} the same 26.3 menu treatment as the
 * vanilla {@code GuiSlot}: a clear area above/below the separators, a light
 * 20% scrim between them, and untouched opaque selection highlights.
 *
 * <p>{@code GuiScrollingList} is the base of the 1.7.10 mod list. It draws its
 * old background through {@code drawGradientRect} and the dirt strips through
 * {@code overlayBackground}; both are replaced here instead of touching the
 * mod-list layout.</p>
 */
@Mixin(targets = "cpw.mods.fml.client.GuiScrollingList", remap = false)
public abstract class MixinGuiScrollingListBackground {

    private static final ResourceLocation MODERNUI_TRANSPARENT =
            new ResourceLocation("modernui", "textures/gui/transparent.png");

    @Shadow @Final protected int left;
    @Shadow @Final protected int top;
    @Shadow @Final protected int bottom;
    @Shadow @Final private int right;

    @Unique
    private boolean modernui$clipActive;

    @Inject(method = "drawGradientRect", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$drawListScrim(int left, int top, int right, int bottom, int startColor,
            int endColor, CallbackInfo callbackInfo) {
        if (!usesSharedBackdrop()) {
            return;
        }
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawScrim(left, top + 2, right, bottom - 2);
        callbackInfo.cancel();
    }

    @Inject(method = "overlayBackground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$clearOverlay(int top, int bottom, int topAlpha, int bottomAlpha,
            CallbackInfo callbackInfo) {
        if (usesSharedBackdrop()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Tessellator;func_78384_a(II)V"),
            remap = false, require = 0)
    private void modernui$clearFourPixelFade(Tessellator tessellator, int color, int alpha) {
        tessellator.setColorRGBA_I(color, usesSharedBackdrop() && color == 0 ? 0 : alpha);
    }

    /**
     * When no world exists {@code GuiScrollingList} paints the list with the
     * vanilla dirt texture instead of {@code drawGradientRect}. Replace that
     * first texture bind with a transparent pixel and paint the 26.3 scrim in
     * its place. Selection boxes are drawn later and stay fully opaque.
     */
    @Redirect(method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;func_110577_a(Lnet/minecraft/util/ResourceLocation;)V",
                    ordinal = 0),
            remap = false, require = 0)
    private void modernui$replaceDirtListBackground(TextureManager manager, ResourceLocation location) {
        if (!usesSharedBackdrop()) {
            manager.bindTexture(location);
            return;
        }
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawScrim(left, top + 2, right, bottom - 2);
        ModernGuiRenderer.beginUi();
        manager.bindTexture(MODERNUI_TRANSPARENT);
    }

    @Inject(method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lcpw/mods/fml/client/GuiScrollingList;drawBackground()V",
                    shift = At.Shift.AFTER),
            remap = false, require = 0)
    private void modernui$beginListClip(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        if (!usesSharedBackdrop()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        int clipTop = top + 2;
        int clipHeight = bottom - top - 4;
        int clipWidth = right - left;
        if (clipHeight <= 0 || clipWidth <= 0) {
            return;
        }
        int scale = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight)
                .getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left * scale, minecraft.displayHeight - (clipTop + clipHeight) * scale,
                clipWidth * scale, clipHeight * scale);
        modernui$clipActive = true;
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), remap = false, require = 0)
    private void modernui$drawListSeparators(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        if (modernui$clipActive) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            modernui$clipActive = false;
        }
        if (!usesSharedBackdrop()) {
            return;
        }
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawSeparators(left, top, right, bottom);
    }

    private static boolean usesSharedBackdrop() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen);
    }
}
