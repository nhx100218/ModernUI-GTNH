package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatScrollSmooth;

import net.minecraft.client.gui.GuiChat;

/** Chat-only slide-in/slide-out animation; scrolling is untouched. */
@Mixin(GuiChat.class)
public class MixinGuiChatAnimation {

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void modernui$pushChatAnimation(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ChatScrollSmooth.pushChat();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void modernui$popChatAnimation(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ChatScrollSmooth.popChat();
    }
}
