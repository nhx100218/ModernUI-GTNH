package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;

/**
 * Drives the ModernUI command suggestion popup for vanilla 1.7.10
 * {@code GuiChat}. Local suggestions are recomputed while typing; Tab/server
 * completions replace them with the response list.
 */
public final class ChatCommandSuggestions {

    private static final CommandCompletionProvider PROVIDER = new ClientCommandCompletionProvider();
    private static final CommandCompletionModel MODEL = new CommandCompletionModel();

    private ChatCommandSuggestions() {
    }

    public static CommandCompletionModel model() {
        return MODEL;
    }

    public static void reset() {
        MODEL.close();
    }

    public static void onInput(String text) {
        if (text == null || !text.startsWith("/")) {
            MODEL.close();
            return;
        }
        List<CommandCompletionEntry> entries = PROVIDER.collect(text, text.length(), 8);
        if (entries.isEmpty()) {
            MODEL.close();
        } else {
            MODEL.setEntries(entries, text);
        }
    }

    public static void onServerCandidates(List<String> candidates, String text, int selected) {
        List<CommandCompletionEntry> entries = new ArrayList<CommandCompletionEntry>();
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate == null || candidate.isEmpty()) {
                    continue;
                }
                boolean command = candidate.startsWith("/") || candidate.indexOf(' ') < 0;
                entries.add(new CommandCompletionEntry(candidate, command ? "Command" : "Suggestion",
                        command ? CommandCompletionEntry.Kind.COMMAND : CommandCompletionEntry.Kind.ARGUMENT,
                        1000 - entries.size()));
            }
        }
        if (entries.isEmpty()) {
            MODEL.close();
            return;
        }
        MODEL.setEntries(entries, text);
        MODEL.selectIndex(selected);
    }

    private static int popupWidth(Minecraft minecraft, GuiTextField inputField) {
        if (minecraft == null || inputField == null) {
            return 0;
        }
        FontRenderer font = minecraft.fontRenderer;
        int max = 0;
        for (CommandCompletionEntry entry : MODEL.entries()) {
            max = Math.max(max, font.getStringWidth(entry.text));
        }
        int screenWidth = minecraft.currentScreen == null
                ? inputField.width + inputField.xPosition : minecraft.currentScreen.width;
        return Math.max(20, Math.min(screenWidth - inputField.xPosition, max + 2));
    }

    private static int popupLeft(Minecraft minecraft, GuiTextField inputField, int width) {
        if (minecraft == null || inputField == null) {
            return inputField == null ? 0 : inputField.xPosition;
        }
        String text = inputField.getText();
        if (text == null) {
            text = "";
        }
        int caret = Math.max(0, Math.min(inputField.getCursorPosition(), text.length()));
        int start = text.lastIndexOf(' ', Math.max(0, caret - 1)) + 1;
        if (start < 0) {
            start = 0;
        }
        int wordX = inputField.xPosition
                + minecraft.fontRenderer.getStringWidth(text.substring(0, Math.min(start, text.length())));
        int min = inputField.xPosition;
        int max = inputField.xPosition + inputField.width - width;
        if (max < min) {
            max = min;
        }
        return Math.max(min, Math.min(wordX, max));
    }

    private static int popupTop(GuiTextField inputField, int height) {
        return inputField.yPosition - height - 1;
    }

    public static void render(Minecraft minecraft, GuiTextField inputField) {
        if (minecraft == null || inputField == null || minecraft.currentScreen == null || !MODEL.isOpen()) {
            return;
        }
        int width = popupWidth(minecraft, inputField);
        int height = CommandCompletionRenderer.preferredHeight(MODEL, MODEL.visibleRows());
        int top = popupTop(inputField, height);
        if (top < 0) {
            top = 0;
        }
        int left = popupLeft(minecraft, inputField, width);
        ModernGuiRenderer.beginUi();
        CommandCompletionRenderer.render(minecraft, MODEL, left, top, width,
                MODEL.visibleRows());
        ModernGuiRenderer.beginUi();
    }

    /** Mouse click on a suggestion row; returns true when consumed. */
    public static boolean mouseClicked(int mouseX, int mouseY, GuiTextField inputField) {
        if (!MODEL.isOpen() || inputField == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        int width = popupWidth(minecraft, inputField);
        int height = CommandCompletionRenderer.preferredHeight(MODEL, MODEL.visibleRows());
        int top = popupTop(inputField, height);
        if (top < 0) {
            top = 0;
        }
        int left = popupLeft(minecraft, inputField, width);
        if (mouseX < left || mouseX >= left + width || mouseY < top || mouseY >= top + height) {
            return false;
        }
        int row = (mouseY - top - 1) / CommandCompletionRenderer.ROW_HEIGHT;
        int index = MODEL.scrollOffset() + row;
        if (index < 0 || index >= MODEL.entries().size()) {
            return false;
        }
        MODEL.selectIndex(index);
        apply(inputField, MODEL.entries().get(index));
        return true;
    }

    private static void apply(GuiTextField inputField, CommandCompletionEntry entry) {
        String text = inputField.getText();
        if (text == null) {
            text = "";
        }
        int caret = Math.max(0, Math.min(inputField.getCursorPosition(), text.length()));
        int start = text.lastIndexOf(' ', Math.max(0, caret - 1)) + 1;
        if (start < 0) {
            start = 0;
        }
        String candidate = entry.text;
        // The input already starts with '/', so do not duplicate it for the
        // first word (client command candidates carry their own '/').
        if (start <= 1 && candidate.startsWith("/")) {
            candidate = candidate.substring(1);
        }
        String next = text.substring(0, start) + candidate;
        inputField.setText(next);
        inputField.setCursorPosition(Math.min(next.length(), start + candidate.length()));
        onInput(next);
    }
}
