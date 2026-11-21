package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.util.List;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Mouse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.command.ChatCommandSuggestions;

/**
 * Adds the ModernUI command suggestion popup to vanilla {@code GuiChat}.
 *
 * <p>The vanilla completion machinery (client command handler + C14 server
 * response + Tab cycling) is kept; its candidate list drives the popup instead
 * of printing a comma-separated line into chat.</p>
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChatCommandSuggestions {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private List field_146412_t;

    @Shadow
    private int field_146413_s;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernui$resetPopup(CallbackInfo callbackInfo) {
        ChatCommandSuggestions.bindField(inputField);
        ChatCommandSuggestions.reset();
    }

    @Inject(method = "onGuiClosed", at = @At("RETURN"))
    private void modernui$closePopup(CallbackInfo callbackInfo) {
        ChatCommandSuggestions.bindField(null);
        ChatCommandSuggestions.reset();
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void modernui$updatePopup(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (!ChatCommandSuggestions.isChatField(inputField)) {
            return;
        }
        if (keyCode == 15) {
            ChatCommandSuggestions.onServerCandidates(field_146412_t, inputField.getText(),
                    field_146413_s - 1);
        } else {
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
        }
    }

    @Inject(method = "func_146406_a", at = @At("RETURN"))
    private void modernui$serverResponse(String[] options, CallbackInfo callbackInfo) {
        if (!ChatCommandSuggestions.isChatField(inputField)) {
            return;
        }
        ChatCommandSuggestions.onServerCandidates(field_146412_t, inputField.getText(),
                field_146413_s);
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void modernui$drawPopup(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        ChatCommandSuggestions.render(net.minecraft.client.Minecraft.getMinecraft(), inputField, mouseX, mouseY);
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void modernui$scrollPopup(CallbackInfo callbackInfo) {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && ChatCommandSuggestions.mouseScrolled(wheel, inputField)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void modernui$clickPopup(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (mouseButton == 0 && ChatCommandSuggestions.mouseClicked(mouseX, mouseY, inputField)) {
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "func_146404_p_",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiNewChat;"
                            + "printChatMessageWithOptionalDeletion(Lnet/minecraft/util/IChatComponent;I)V"),
            require = 0)
    private void modernui$suppressCandidateChatLine(GuiNewChat chat, IChatComponent message, int id) {
        // Candidates are shown in the popup instead of a chat line.
    }
}
