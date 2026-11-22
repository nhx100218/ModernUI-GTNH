package blocamlimb.nhx100218.modernui.uilib.mixin.early.etfuturum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.EfrCompat;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernLoadingScreen;

/**
 * Suppresses EFR's bridge screen before it reaches EFR's renderer.
 *
 * <p>Both this and EFR's renderer are optional mixins. Whichever entry point the
 * host calls first, {@link ModernLoadingScreen}'s re-entrancy guard keeps the
 * frame from being drawn twice.</p>
 */
@Mixin(targets = "ganymedes01.etfuturum.client.GuiLoadingBridge", remap = false, priority = 1500)
public abstract class MixinEfrGuiLoadingBridge {

    // EFR ships a production (SRG-named) class: the method is literally
    // func_73863_a, not drawScreen. remap=false keeps Mixin from looking for a
    // name that does not exist in the EFR jar.
    @Inject(method = "func_73863_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$drawLoadingBridge(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        GuiScreen screen = (GuiScreen) (Object) this;
        boolean leavingWorld = minecraft.currentScreen instanceof GuiIngameMenu;
        if (ModernLoadingScreen.render(minecraft, screen.width, screen.height, EfrCompat.snapshot(),
                -1.0F, !leavingWorld)) {
            callbackInfo.cancel();
        }
    }
}
