package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.command.ChatCommandSuggestions;

/**
 * Draws the vanilla chat input with 26.3 token colours.
 *
 * <p>Only {@code GuiChat}'s field is taken over; every other
 * {@link GuiTextField} keeps vanilla rendering (including its search border,
 * smooth scrolling and selection hooks). The replacement mirrors vanilla
 * {@code drawTextBox} so scrolling, the blinking caret and the selection
 * overlay behave exactly as before.</p>
 */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldChatColors {

    @Shadow
    @Final
    private FontRenderer field_146211_a;

    @Shadow
    private String text;

    @Shadow
    private int lineScrollOffset;

    @Shadow
    private int cursorPosition;

    @Shadow
    private int selectionEnd;

    @Shadow
    private int enabledColor;

    @Shadow
    private int disabledColor;

    @Shadow
    private int cursorCounter;

    @Shadow
    private boolean isFocused;

    @Shadow
    private boolean isEnabled;

    @Shadow
    private boolean enableBackgroundDrawing;

    @Shadow
    private boolean visible;

    @Shadow
    public int xPosition;

    @Shadow
    public int yPosition;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    private void drawCursorVertical(int x1, int y1, int x2, int y2) {
        throw new AssertionError();
    }

    @Inject(method = "drawTextBox", at = @At("HEAD"), cancellable = true)
    private void modernui$drawChatInput(CallbackInfo callbackInfo) {
        GuiTextField field = (GuiTextField) (Object) this;
        if (!ChatCommandSuggestions.isChatField(field)) {
            return;
        }
        // Horizontal scrolling (and its smooth-scroll matrix shift) is handled
        // by the vanilla path; colouring a scrolled view would misalign. Chat
        // input only scrolls for extremely long text.
        if (text != null && field_146211_a.getStringWidth(text) > field.getWidth()) {
            return;
        }
        callbackInfo.cancel();
        if (!visible) {
            return;
        }
        if (enableBackgroundDrawing) {
            Gui.drawRect(xPosition - 1, yPosition - 1, xPosition + width + 1, yPosition + height + 1, -6250336);
            Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, -16777216);
        }

        int color = isEnabled ? enabledColor : disabledColor;
        int caretInLine = cursorPosition - lineScrollOffset;
        int selectionInLine = selectionEnd - lineScrollOffset;
        String visibleText = field_146211_a.trimStringToWidth(text.substring(lineScrollOffset), field.getWidth());
        boolean caretInRange = caretInLine >= 0 && caretInLine <= visibleText.length();
        boolean caretBlink = isFocused && cursorCounter / 6 % 2 == 0 && caretInRange;
        int textX = enableBackgroundDrawing ? xPosition + 4 : xPosition;
        int textY = enableBackgroundDrawing ? yPosition + (height - 8) / 2 : yPosition;

        if (selectionInLine > visibleText.length()) {
            selectionInLine = visibleText.length();
        }

        if (!visibleText.isEmpty()) {
            ChatCommandSuggestions.drawTokenText(field_146211_a, visibleText, textX, textY,
                    caretInRange ? caretInLine : -1, ChatCommandSuggestions.isPopupOpen());
        }

        boolean caretAtEnd = cursorPosition >= text.length();
        int caretX;
        if (!caretInRange) {
            caretX = caretInLine > 0 ? textX + width : textX;
        } else if (caretAtEnd) {
            caretX = textX + field_146211_a.getStringWidth(visibleText);
        } else {
            caretX = textX + field_146211_a.getStringWidth(visibleText.substring(0, caretInLine)) - 1;
        }

        if (caretBlink) {
            if (!caretAtEnd) {
                Gui.drawRect(caretX, textY - 1, caretX + 1, textY + 1 + field_146211_a.FONT_HEIGHT, -3092272);
            } else {
                field_146211_a.drawStringWithShadow("_", caretX, textY, color);
            }
        }

        if (selectionInLine != caretInLine) {
            int selectionX = textX + field_146211_a.getStringWidth(visibleText.substring(0, selectionInLine));
            drawCursorVertical(caretX, textY - 1, selectionX - 1, textY + 1 + field_146211_a.FONT_HEIGHT);
        }
    }
}
