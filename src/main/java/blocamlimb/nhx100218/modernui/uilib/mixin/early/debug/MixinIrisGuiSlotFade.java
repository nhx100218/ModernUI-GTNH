package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import net.coderbot.iris.gui.element.IrisGuiSlot;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Suppresses the vanilla four-pixel fades on Iris' menu lists.
 *
 * <p>{@code IrisGuiSlot} overrides {@code drawScreen}, so the {@code GuiSlot}
 * fade cancellation never reaches it. ModernUI already draws the crisp 2px
 * separators after the screen (see {@code IrisListSeparators}); this mixin only
 * removes the old gradient bands. It deliberately performs no drawing inside the
 * host render pass, which previously caused GL-state flicker.</p>
 */
@Mixin(targets = "net.coderbot.iris.gui.element.IrisGuiSlot", remap = false)
public abstract class MixinIrisGuiSlotFade {

    @Redirect(method = "func_148128_a",
            at = @At(value = "INVOKE",
                    target = "Lnet/coderbot/iris/gui/element/IrisGuiSlot;func_148136_c(IIII)V"),
            remap = false, require = 0)
    private void modernui$skipVanillaFade(IrisGuiSlot slot, int top, int bottom, int topAlpha, int bottomAlpha) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen)) {
            return;
        }
        // All IrisGuiSlot users are ModernUI-managed menus; keep the fade skipped
        // rather than calling a private superclass method we cannot reach.
    }
}
