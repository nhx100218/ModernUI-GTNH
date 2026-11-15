package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import net.coderbot.iris.gui.element.IrisGuiSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Gives Iris' custom lists (shader packs, atlas packs, dynamic-light entities)
 * the 26.3 header/footer separators instead of the vanilla four-pixel fades.
 *
 * <p>{@code IrisGuiSlot} overrides {@code drawScreen} and therefore never
 * reaches the {@code GuiSlot} mixin. The vanilla fade is suppressed and the two
 * 2px separators are drawn after the rows.</p>
 */
@Mixin(targets = "net.coderbot.iris.gui.element.IrisGuiSlot", remap = false)
public abstract class MixinIrisGuiSlotSeparators {

    @Redirect(method = "func_148128_a",
            at = @At(value = "INVOKE",
                    target = "Lnet/coderbot/iris/gui/element/IrisGuiSlot;func_148136_c(IIII)V"),
            remap = false, require = 0)
    private void modernui$skipVanillaFade(IrisGuiSlot slot, int top, int bottom, int topAlpha, int bottomAlpha) {
        // ModernUI replaces both fades with the crisp 2px separators below.
    }

    @Inject(method = "func_148128_a", at = @At("RETURN"), remap = false, require = 0)
    private void modernui$drawSeparators(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !ModernMenuScreens.usesSharedListBackdrop(minecraft.currentScreen)) {
            return;
        }
        GuiSlot slot = (GuiSlot) (Object) this;
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawSeparators(slot.left, slot.top, slot.left + slot.width, slot.bottom);
    }
}
