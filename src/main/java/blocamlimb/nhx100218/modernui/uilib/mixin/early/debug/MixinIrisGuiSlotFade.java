package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import net.coderbot.iris.gui.element.IrisGuiSlot;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;
import blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu.MixinGuiSlotFadeAccessor;

/**
 * Hides the vanilla four-pixel fades on Iris' menu lists while keeping the
 * original method (and therefore its tessellator flush and GL-state restore).
 *
 * <p>Simply cancelling the call caused the shader screen to flicker because the
 * host's GL state tracker was left mid-batch. Calling the real method with alpha
 * zero is state-neutral and only removes the fade pixels; the modern 2px
 * separators are drawn later by {@code IrisListSeparators}.</p>
 */
@Mixin(targets = "net.coderbot.iris.gui.element.IrisGuiSlot", remap = false)
public abstract class MixinIrisGuiSlotFade {

    @Redirect(method = "func_148128_a",
            at = @At(value = "INVOKE",
                    target = "Lnet/coderbot/iris/gui/element/IrisGuiSlot;func_148136_c(IIII)V"),
            remap = false, require = 0)
    private void modernui$transparentFade(IrisGuiSlot slot, int top, int bottom, int topAlpha, int bottomAlpha) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen)) {
            return;
        }
        ((MixinGuiSlotFadeAccessor) slot).modernui$overlayBackground(top, bottom, 0, 0);
    }
}
