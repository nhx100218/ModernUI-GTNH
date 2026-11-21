package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.util.ArrayDeque;
import java.util.Deque;

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
 * {@code /give} instant). Server suggestions for the command list and for
 * individual arguments are requested automatically and cached, so lists such
 * as {@code /gamerule} appear without pressing Tab first.</p>
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChatCommandSuggestions {

    private static final String REQUEST_COMMANDS = "commands";
    private static final String REQUEST_ARGUMENT = "args:";
    private static final String REQUEST_TAB = "tab";

    @Shadow
    protected GuiTextField inputField;

    /** Vanilla "a tab request is pending" flag. */
    @Shadow
    private boolean field_146414_r;

    @Shadow
    private void func_146405_a(String leftOfCursor, String word) {
        throw new AssertionError();
    }

    /** Types of the requests sent to the server, in send order. */
    @Unique
    private final Deque<String> modernui$requests = new ArrayDeque<String>();

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernui$resetPopup(CallbackInfo callbackInfo) {
        ChatCommandSuggestions.bindField(inputField);
        ChatCommandSuggestions.reset();
        modernui$requests.clear();
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
        modernui$requests.clear();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernui$tabComplete(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (keyCode != 15) {
            return;
        }
        if (ChatCommandSuggestions.tabComplete(inputField, GuiScreen.isShiftKeyDown())) {
            // The popup owns this Tab; skip vanilla's C14 request and chat line.
            callbackInfo.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void modernui$updatePopup(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (!ChatCommandSuggestions.isChatField(inputField)) {
            return;
        }
        if (keyCode == 15) {
            // Vanilla handled this Tab (the popup had no local candidates) and
            // sent its own request; remember its response slot.
            if (field_146414_r) {
                modernui$requests.addLast(REQUEST_TAB);
            }
            return;
        }
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
        String request = modernui$requests.pollFirst();
        if (request == null) {
            return;
        }
        if (REQUEST_COMMANDS.equals(request)) {
            ClientCommandCompletionProvider.learnCommands(options);
            ClientCommandCompletionProvider.markCommandListFetched();
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
            modernui$maybeFetchArguments();
            return;
        }
        if (request.startsWith(REQUEST_ARGUMENT)) {
            ClientCommandCompletionProvider.learnArguments(request.substring(REQUEST_ARGUMENT.length()), options);
            ChatCommandSuggestions.onInput(inputField.getText(), inputField.getCursorPosition());
            modernui$maybeFetchArguments();
            return;
        }
        if (!REQUEST_TAB.equals(request)) {
            return;
        }
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
        if (modernui$hasPendingRequest(REQUEST_ARGUMENT)) {
            return;
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
        if (!ClientCommandCompletionProvider.needsArgumentRefresh(before)) {
            return;
        }
        modernui$requests.addLast(REQUEST_ARGUMENT + before);
        int wordStart = before.lastIndexOf(' ') + 1;
        try {
            func_146405_a(before, before.substring(Math.max(0, wordStart)));
        } catch (Throwable ignored) {
            modernui$requests.pollLast();
        }
    }

    /** Requests the full command tree once, using the vanilla C14 path. */
    @Unique
    private void modernui$fetchCommandList() {
        if (modernui$hasPendingRequest(REQUEST_COMMANDS)) {
            return;
        }
        if (!ClientCommandCompletionProvider.needsCommandRefresh()) {
            return;
        }
        if (inputField == null || !inputField.getText().startsWith("/")) {
            return;
        }
        modernui$requests.addLast(REQUEST_COMMANDS);
        try {
            func_146405_a("/", "");
        } catch (Throwable ignored) {
            modernui$requests.pollLast();
        }
    }

    @Unique
    private boolean modernui$hasPendingRequest(String prefix) {
        for (String request : modernui$requests) {
            if (request.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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
