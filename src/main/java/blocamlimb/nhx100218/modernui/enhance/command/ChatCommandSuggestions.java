package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
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

    public static void render(Minecraft minecraft, GuiTextField inputField) {
        if (minecraft == null || inputField == null || minecraft.currentScreen == null || !MODEL.isOpen()) {
            return;
        }
        int left = inputField.xPosition;
        int width = Math.max(140, Math.min(300, inputField.width));
        int height = CommandCompletionRenderer.preferredHeight(MODEL, 8);
        int top = inputField.yPosition - height - 2;
        ModernGuiRenderer.beginUi();
        CommandCompletionRenderer.render(minecraft, MODEL, left, top, width, 8);
        ModernGuiRenderer.beginUi();
    }
}
