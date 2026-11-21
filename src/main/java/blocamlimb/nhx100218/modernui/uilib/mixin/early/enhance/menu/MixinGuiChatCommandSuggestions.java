package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Mouse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.command.ChatCommandSuggestions;
import blocamlimb.nhx100218.modernui.enhance.command.ClientCommandCompletionProvider;

/**
 * Adds the ModernUI command suggestion popup to vanilla {@code GuiChat}.
 *
 * <p>Tab is handled by the popup while it is open, so no C14 request is sent
 * for completions we can build locally (this keeps large lists such as
 * {@code /give} instant). Server responses are processed through the same model,
 * without letting vanilla rewrite the input line.</p>
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChatCommandSuggestions {

    @Shadow
    protected GuiTextField inputField;

    /** Vanilla "a tab request is pending" flag. */
    @Shadow
    private boolean field_146414_r;

    @Shadow
    private void func_146405_a(String leftOfCursor, String word) {
        throw new AssertionError();
    }

    @Unique
    private boolean modernui$awaitingCommands;

    @Unique
    private boolean modernui$tabRequested;

    @Unique
    private String modernui$argRequest;

    @Unique
    private long modernui$argRequestAt;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernui$resetPopup(CallbackInfo callbackInfo) {
        ChatCommandSuggestions.bindField(inputField);
        ChatCommandSuggestions.reset();
        modernui$awaitingCommands = false;
        modernui$tabRequested = false;
        modernui$argRequest = null;
        if (inputField != null) {
            // Opening chat with "/" must show the popup immediately.
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
            if (inputField.getText().startsWith("/")) {
                modernui$fetchCommandList();
                modernui$maybeFetchArguments();
            }
        }
    }

    @Inject(method = "onGuiClosed", at = @At("RETURN"))
    private void modernui$closePopup(CallbackInfo callbackInfo) {
        ChatCommandSuggestions.bindField(null);
        ChatCommandSuggestions.reset();
        modernui$awaitingCommands = false;
        modernui$tabRequested = false;
        modernui$argRequest = null;
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernui$tabComplete(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (keyCode != 15) {
            return;
        }
        if (ChatCommandSuggestions.tabComplete(inputField, GuiScreen.isShiftKeyDown())) {
            // The popup owns this Tab; skip vanilla's C14 request and chat line.
            callbackInfo.cancel();
        } else {
            modernui$tabRequested = true;
        }
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void modernui$updatePopup(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (!ChatCommandSuggestions.isChatField(inputField)) {
            return;
        }
        if (keyCode == 15) {
            // Vanilla handled this Tab (the popup had no local candidates).
            if (!field_146414_r) {
                modernui$tabRequested = false;
            }
            return;
        }
        modernui$tabRequested = false;
        ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
        if (inputField.getText().startsWith("/")) {
            modernui$fetchCommandList();
            modernui$maybeFetchArguments();
        }
    }

    @Inject(method = "func_146406_a", at = @At("HEAD"), cancellable = true)
    private void modernui$serverResponse(String[] options, CallbackInfo callbackInfo) {
        if (!ChatCommandSuggestions.isChatField(inputField)) {
            return;
        }
        // Always keep vanilla's prefix insertion / cycling out of the text:
        // the popup applies the response itself.
        callbackInfo.cancel();
        if (modernui$awaitingCommands) {
            modernui$awaitingCommands = false;
            ClientCommandCompletionProvider.learnCommands(options);
            ClientCommandCompletionProvider.markCommandListFetched();
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
            modernui$maybeFetchArguments();
            return;
        }
        if (modernui$argRequest != null) {
            String requested = modernui$argRequest;
            modernui$argRequest = null;
            ClientCommandCompletionProvider.learnArguments(requested, options);
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
            modernui$maybeFetchArguments();
            return;
        }
        if (!modernui$tabRequested) {
            return;
        }
        modernui$tabRequested = false;
        ClientCommandCompletionProvider.learnCommands(options);
        java.util.List<String> merged = new java.util.ArrayList<String>();
        String[] client = net.minecraftforge.client.ClientCommandHandler.instance.latestAutoComplete;
        if (client != null) {
            java.util.Collections.addAll(merged, client);
        }
        java.util.Collections.addAll(merged, options);
        ChatCommandSuggestions.onServerTabResponse(merged, inputField);
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

    /** Requests server suggestions for the argument currently being typed. */
    @Unique
    private void modernui$maybeFetchArguments() {
        if (inputField == null) {
            return;
        }
        long now = net.minecraft.client.Minecraft.getSystemTime();
        if (modernui$argRequest != null) {
            if (now - modernui$argRequestAt < 3000L) {
                return;
            }
            modernui$argRequest = null;
        }
        String text = inputField.getText();
        if (text == null || text.indexOf(' ') < 0) {
            return;
        }
        int caret = Math.max(0, Math.min(inputField.getCursorPosition(), text.length()));
        String before = text.substring(0, caret);
        if (before.indexOf(' ') < 0) {
            return;
        }
        // Local suggestions are already complete for this argument.
        if (ChatCommandSuggestions.isPopupOpen()) {
            return;
        }
        if (modernui$awaitingCommands) {
            return;
        }
        if (!ClientCommandCompletionProvider.needsArgumentRefresh(before)) {
            return;
        }
        modernui$argRequest = before;
        modernui$argRequestAt = now;
        int wordStart = before.lastIndexOf(' ') + 1;
        func_146405_a(before, before.substring(Math.max(0, wordStart)));
    }

    /** Requests the full command tree once, using the vanilla C14 path. */
    @Unique
    private void modernui$fetchCommandList() {
        if (modernui$awaitingCommands) {
            return;
        }
        if (!ClientCommandCompletionProvider.needsCommandRefresh()) {
            return;
        }
        if (inputField == null || !inputField.getText().startsWith("/")) {
            return;
        }
        modernui$awaitingCommands = true;
        func_146405_a("/", "");
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
