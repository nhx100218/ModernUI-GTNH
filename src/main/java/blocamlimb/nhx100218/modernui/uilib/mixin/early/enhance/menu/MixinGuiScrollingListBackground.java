package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
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

    @Inject(method = "drawGradientRect", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$drawListScrim(int left, int top, int right, int bottom, int startColor,
            int endColor, CallbackInfo callbackInfo) {
        if (!usesSharedBackdrop()) {
            return;
        }
        ModernGuiRenderer.fill(left, top, right, bottom, 0x33000000);
        ModernGuiRenderer.fill(left, top, right, top + 1, 0x40FFFFFF);
        ModernGuiRenderer.fill(left, bottom - 1, right, bottom, 0x40FFFFFF);
        ModernGuiRenderer.beginUi();
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
        ModernGuiRenderer.fill(left, top, right, bottom, 0x33000000);
        ModernGuiRenderer.fill(left, top, right, top + 1, 0x40FFFFFF);
        ModernGuiRenderer.fill(left, bottom - 1, right, bottom, 0x40FFFFFF);
        ModernGuiRenderer.beginUi();
        manager.bindTexture(MODERNUI_TRANSPARENT);
    }

    private static boolean usesSharedBackdrop() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen);
    }
}
