package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ScreenTransition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Starts the screen transition timer and delays the chat close animation. */
@Mixin(Minecraft.class)
public class MixinMinecraftScreenPoke {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void modernui$poke(GuiScreen screen, CallbackInfo ci) {
        if (ScreenTransition.interceptGuiDisplay(screen)) {
            ci.cancel();
            return;
        }
        ScreenTransition.poke();
    }
}
