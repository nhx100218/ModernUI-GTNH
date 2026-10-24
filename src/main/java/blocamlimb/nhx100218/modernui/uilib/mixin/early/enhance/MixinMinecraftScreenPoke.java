package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ScreenTransition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Starts the screen transition timer on every displayGuiScreen call. */
@Mixin(Minecraft.class)
public class MixinMinecraftScreenPoke {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void modernui$poke(GuiScreen screen, CallbackInfo ci) {
        ScreenTransition.poke();
    }
}
