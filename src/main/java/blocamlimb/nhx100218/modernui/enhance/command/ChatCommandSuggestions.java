package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.Item;

import org.lwjgl.input.Mouse;

/**
 * Drives the ModernUI command suggestion popup for vanilla 1.7.10
 * {@code GuiChat}. Local suggestions are recomputed while typing; Tab/server
 * completions replace them with the response list.
 */
public final class ChatCommandSuggestions {

    private static final CommandCompletionProvider PROVIDER = new ClientCommandCompletionProvider();
    private static final CommandCompletionModel MODEL = new CommandCompletionModel();
    /** The list renders 10 rows; keep enough candidates to scroll like 26.3. */
    private static final int MAX_CANDIDATES = 256;

    private static final int COLOR_CHAT = 0xE0E0E0;
    private static final int COLOR_LITERAL = 0xAAAAAA;
    private static final int COLOR_RED = 0xFF5555;
    /** 26.3 {@code CommandSuggestions.ARGUMENT_STYLES}, keyed by argument index. */
    private static final int[] ARGUMENT_COLORS = {0x55FFFF, 0xFFFF55, 0x55FF55, 0xFF55FF, 0xFFAA00};

    /** The field currently drawn by {@code GuiChat}; used by the text-field mixin. */
    private static GuiTextField chatField;
    private static int lastMouseX = Integer.MIN_VALUE;
    private static int lastMouseY = Integer.MIN_VALUE;
    /** True while Tab is cycling through the list captured before the first press. */
    private static boolean cycling;

    private ChatCommandSuggestions() {
    }

    public static CommandCompletionModel model() {
        return MODEL;
    }

    public static void bindField(GuiTextField field) {
        chatField = field;
    }

    public static boolean isChatField(GuiTextField field) {
        return field != null && field == chatField;
    }

    public static boolean isPopupOpen() {
        return MODEL.isOpen();
    }

    public static void reset() {
        MODEL.close();
        cycling = false;
        lastMouseX = Integer.MIN_VALUE;
        lastMouseY = Integer.MIN_VALUE;
    }

    public static void onInput(String text, int cursor) {
        cycling = false;
        if (text == null || !text.startsWith("/")) {
            MODEL.close();
            return;
        }
        int caret = Math.max(0, Math.min(cursor <= 0 ? text.length() : cursor, text.length()));
        List<CommandCompletionEntry> entries = PROVIDER.collect(text, caret, MAX_CANDIDATES);
        if (entries.isEmpty()) {
            MODEL.close();
        } else {
            MODEL.setEntries(entries, text);
        }
    }

    public static void onServerCandidates(List<String> candidates, String text, int selected) {
        cycling = false;
        List<CommandCompletionEntry> entries = new ArrayList<CommandCompletionEntry>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        if (candidates != null) {
            for (String candidate : candidates) {
                if (entries.size() >= MAX_CANDIDATES) {
                    break;
                }
                if (candidate == null || candidate.isEmpty()) {
                    continue;
                }
                // Forge colour-codes its client completions ("§7/gamemode§r");
                // the popup and the applied text must stay plain.
                String clean = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(candidate).trim();
                if (clean.isEmpty() || !seen.add(clean.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                boolean command = clean.startsWith("/") || clean.indexOf(' ') < 0;
                entries.add(new CommandCompletionEntry(clean, command ? "Command" : "Suggestion",
                        command ? CommandCompletionEntry.Kind.COMMAND : CommandCompletionEntry.Kind.ARGUMENT,
                        1000 - entries.size()));
            }
        }
        if (entries.isEmpty()) {
            MODEL.close();
            return;
        }
        MODEL.setEntries(entries, text);
        if (selected >= 0) {
            MODEL.selectIndex(selected);
        }
    }

    private static int popupWidth(Minecraft minecraft, GuiTextField inputField) {
        if (minecraft == null || inputField == null) {
            return 0;
        }
        FontRenderer font = minecraft.fontRenderer;
        int max = 0;
        for (CommandCompletionEntry entry : MODEL.entries()) {
            max = Math.max(max, font.getStringWidth(CommandCompletionRenderer.displayText(entry)));
        }
        int screenWidth = minecraft.currentScreen == null
                ? inputField.width + inputField.xPosition : minecraft.currentScreen.width;
        return Math.max(20, Math.min(screenWidth - inputField.xPosition, max + 1));
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
        // Root command suggestions align after the leading slash; the list
        // displays the command names without it.
        if (start == 0 && text.startsWith("/")) {
            start = 1;
        }
        int wordX = inputField.xPosition
                + minecraft.fontRenderer.getStringWidth(text.substring(0, Math.min(start, text.length())));
        int max = inputField.xPosition + inputField.width - width;
        if (max < inputField.xPosition) {
            max = inputField.xPosition;
        }
        // 26.3 places an unbordered suggestion list one pixel left of the
        // completed word and clamps it inside the input.
        return Math.max(inputField.xPosition, Math.min(wordX - 1, max));
    }

    private static int popupTop(GuiTextField inputField, int height) {
        // 26.3 (anchorToBottom, unbordered input): bottom = inputY - 3.
        int top = inputField.yPosition - 3 - height;
        return top < 0 ? 0 : top;
    }

    public static void render(Minecraft minecraft, GuiTextField inputField, int mouseX, int mouseY) {
        if (minecraft == null || inputField == null || minecraft.currentScreen == null || !MODEL.isOpen()) {
            if (mouseX != lastMouseX || mouseY != lastMouseY) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
            return;
        }
        int width = popupWidth(minecraft, inputField);
        int height = CommandCompletionRenderer.preferredHeight(MODEL, MODEL.visibleRows());
        int top = popupTop(inputField, height);
        int left = popupLeft(minecraft, inputField, width);

        // 26.3 moves the highlighted row when the mouse moves over the list.
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            if (inside(left, top, width, height, mouseX, mouseY)) {
                int row = (mouseY - top) / CommandCompletionRenderer.ROW_HEIGHT;
                int index = MODEL.scrollOffset() + row;
                if (row >= 0 && row < MODEL.visibleRows() && index < MODEL.entries().size()) {
                    MODEL.selectIndex(index);
                }
            }
        }
        CommandCompletionRenderer.render(minecraft, MODEL, left, top, width, MODEL.visibleRows());
    }

    /** True when the cursor is over the suggestion list (used for the hand). */
    public static boolean isPopupHovered(int mouseX, int mouseY) {
        GuiTextField field = chatField;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (field == null || minecraft == null || !MODEL.isOpen()) {
            return false;
        }
        int width = popupWidth(minecraft, field);
        int height = CommandCompletionRenderer.preferredHeight(MODEL, MODEL.visibleRows());
        return inside(popupLeft(minecraft, field, width), popupTop(field, height), width, height, mouseX, mouseY);
    }

    private static boolean inside(int left, int top, int width, int height, int mouseX, int mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** Mouse wheel over the popup scrolls the visible rows. */
    public static boolean mouseScrolled(int wheel, GuiTextField inputField) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.currentScreen == null || inputField == null || !MODEL.isOpen()) {
            return false;
        }
        int width = popupWidth(minecraft, inputField);
        int height = CommandCompletionRenderer.preferredHeight(MODEL, MODEL.visibleRows());
        int left = popupLeft(minecraft, inputField, width);
        int top = popupTop(inputField, height);
        int mouseX = Mouse.getEventX() * minecraft.currentScreen.width / minecraft.displayWidth;
        int mouseY = minecraft.currentScreen.height
                - Mouse.getEventY() * minecraft.currentScreen.height / minecraft.displayHeight - 1;
        if (!inside(left, top, width, height, mouseX, mouseY)) {
            return false;
        }
        MODEL.scrollBy(-Integer.signum(wheel));
        return true;
    }

    /**
     * Draws chat input text with the 26.3 colour grammar: command literals and
     * spaces grey, arguments cycling through aqua / yellow / green / light
     * purple / gold, and values that cannot exist in red.
     *
     * @param caret index of the cursor inside {@code text}, or -1 when the
     *              cursor is outside the visible part of the field
     */
    public static void drawTokenText(FontRenderer font, String text, int x, int y, int caret, boolean popupOpen) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        int cursorX = x;
        if (!text.startsWith("/")) {
            font.drawStringWithShadow(text, cursorX, y, COLOR_CHAT);
            return;
        }
        String commandName = "";
        int tokenIndex = 0;
        int i = 0;
        while (i < text.length()) {
            int start = i;
            while (i < text.length() && text.charAt(i) != ' ') {
                i++;
            }
            int end = i;
            String token = text.substring(start, end);
            boolean current = caret >= 0 && caret >= start && caret <= end;
            int color;
            if (tokenIndex == 0) {
                commandName = token.startsWith("/")
                        ? token.substring(1).toLowerCase(Locale.ROOT) : token.toLowerCase(Locale.ROOT);
                // The candidate Tab selected is shown in the bar too, so the
                // word currently being completed is yellow like the list row.
                if (current && popupOpen && !commandName.isEmpty()) {
                    color = ARGUMENT_COLORS[1];
                } else {
                    // 1.7.10 has no client-side command tree, so an unknown
                    // server command must not be shown as invalid.
                    color = COLOR_LITERAL;
                }
            } else {
                int argument = tokenIndex - 1;
                // While the popup offers candidates for the word being typed,
                // it is a valid prefix; only finished values can be missing.
                if (current && popupOpen) {
                    color = ARGUMENT_COLORS[1];
                } else if (!validArgument(commandName, argument, token)) {
                    color = COLOR_RED;
                } else {
                    color = ARGUMENT_COLORS[argument % ARGUMENT_COLORS.length];
                }
            }
            font.drawStringWithShadow(token, cursorX, y, color);
            cursorX += font.getStringWidth(token);
            if (i < text.length() && text.charAt(i) == ' ') {
                cursorX += font.getCharWidth(' ');
                i++;
            }
            tokenIndex++;
        }
    }

    private static boolean validArgument(String command, int index, String token) {
        if (token == null || token.isEmpty()) {
            return true;
        }
        if (command == null) {
            return true;
        }
        switch (command) {
            case "gamemode":
            case "gm":
                if (index == 0) {
                    String value = token.toLowerCase(Locale.ROOT);
                    if (value.equals("spectator") || value.equals("3")) {
                        return ClientCommandCompletionProvider.supportsSpectator();
                    }
                    return value.equals("survival") || value.equals("creative") || value.equals("adventure")
                            || value.equals("0") || value.equals("1") || value.equals("2");
                }
                if (index == 1) {
                    return isOnlinePlayer(token);
                }
                return true;
            case "give":
                if (index == 0) {
                    return isOnlinePlayer(token);
                }
                if (index == 1) {
                    return isItem(token);
                }
                if (index == 2) {
                    return isNumber(token);
                }
                return true;
            case "tp":
            case "tpx":
            case "tphere":
            case "teleport":
            case "kill":
            case "msg":
            case "tell":
            case "w":
            case "op":
            case "deop":
            case "kick":
                return index != 0 || isOnlinePlayer(token);
            default:
                return true;
        }
    }

    private static boolean isItem(String token) {
        String id = token;
        int first = id.indexOf(':');
        int second = first >= 0 ? id.indexOf(':', first + 1) : -1;
        if (second > 0) {
            id = id.substring(0, second);
        }
        try {
            return Item.itemRegistry.getObject(id) != null || Block.blockRegistry.getObject(id) != null;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean isOnlinePlayer(String name) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || minecraft.thePlayer.sendQueue == null) {
            return true;
        }
        if (name.equalsIgnoreCase(minecraft.thePlayer.getCommandSenderName())) {
            return true;
        }
        try {
            for (Object infoObject : minecraft.thePlayer.sendQueue.playerInfoList) {
                if (infoObject instanceof GuiPlayerInfo
                        && name.equalsIgnoreCase(((GuiPlayerInfo) infoObject).name)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return true;
        }
        return false;
    }

    private static boolean isNumber(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Handles Tab/Shift+Tab locally while the popup is open: fills the common
     * prefix once, then cycles the list captured for the typed prefix. Returns
     * false when there is nothing local to complete, so vanilla can ask the
     * server.
     */
    public static boolean tabComplete(GuiTextField inputField, boolean backwards) {
        if (inputField == null || !MODEL.isOpen()) {
            return false;
        }
        String text = inputField.getText();
        if (text == null) {
            return false;
        }
        int caret = Math.max(0, Math.min(inputField.getCursorPosition(), text.length()));
        int start = wordStart(text, caret);
        if (!cycling) {
            String word = text.substring(start, caret);
            if (!hasPrefixMatch(word)) {
                return false;
            }
            String common = commonPrefix(word);
            if (common.length() > word.length()) {
                write(inputField, text, start, caret, common);
                return true;
            }
            cycling = true;
        } else if (backwards) {
            MODEL.move(-1);
        } else {
            MODEL.move(1);
        }
        CommandCompletionEntry entry = MODEL.selected();
        if (entry == null) {
            cycling = false;
            return false;
        }
        write(inputField, text, start, caret, candidateText(entry.text, start));
        return true;
    }

    private static int wordStart(String text, int caret) {
        int start = text.lastIndexOf(' ', Math.max(0, caret - 1)) + 1;
        if (start < 0) {
            start = 0;
        }
        if (start == 0 && text.startsWith("/")) {
            start = 1;
        }
        return Math.min(start, caret);
    }

    private static String candidateText(String candidate, int start) {
        if (start == 1 && candidate.startsWith("/")) {
            return candidate.substring(1);
        }
        return candidate;
    }

    private static boolean hasPrefixMatch(String word) {
        String folded = word.toLowerCase(Locale.ROOT);
        for (CommandCompletionEntry entry : MODEL.entries()) {
            if (CommandCompletionRenderer.displayText(entry).toLowerCase(Locale.ROOT).startsWith(folded)) {
                return true;
            }
        }
        return false;
    }

    private static String commonPrefix(String word) {
        String folded = word.toLowerCase(Locale.ROOT);
        String prefix = null;
        for (CommandCompletionEntry entry : MODEL.entries()) {
            String display = CommandCompletionRenderer.displayText(entry);
            if (!display.toLowerCase(Locale.ROOT).startsWith(folded)) {
                continue;
            }
            if (prefix == null) {
                prefix = display;
            } else {
                prefix = commonOf(prefix, display);
            }
            if (prefix.length() <= word.length()) {
                break;
            }
        }
        return prefix == null ? word : prefix;
    }

    private static String commonOf(String left, String right) {
        int limit = Math.min(left.length(), right.length());
        int i = 0;
        while (i < limit && left.charAt(i) == right.charAt(i)) {
            i++;
        }
        return left.substring(0, i);
    }

    private static void write(GuiTextField inputField, String text, int start, int caret, String replacement) {
        String next = text.substring(0, start) + replacement + text.substring(caret);
        inputField.setText(next);
        inputField.setCursorPosition(Math.min(next.length(), start + replacement.length()));
    }

    /** Applies a server response: rebuilds the list, then completes like Tab. */
    public static boolean onServerTabResponse(List<String> options, GuiTextField inputField) {
        if (inputField == null) {
            return false;
        }
        onServerCandidates(options, inputField.getText(), -1);
        return MODEL.isOpen() && tabComplete(inputField, false);
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
        int left = popupLeft(minecraft, inputField, width);
        if (!inside(left, top, width, height, mouseX, mouseY)) {
            return false;
        }
        int row = (mouseY - top) / CommandCompletionRenderer.ROW_HEIGHT;
        int index = MODEL.scrollOffset() + row;
        if (row < 0 || row >= MODEL.visibleRows() || index < 0 || index >= MODEL.entries().size()) {
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
        // The leading '/' of a command is kept: 26.3 suggestions carry the
        // command name only, so replace the word after the slash.
        if (start == 0 && text.startsWith("/")) {
            start = 1;
        }
        String candidate = entry.text;
        if (start == 1 && candidate.startsWith("/")) {
            candidate = candidate.substring(1);
        }
        String next = text.substring(0, start) + candidate;
        inputField.setText(next);
        inputField.setCursorPosition(Math.min(next.length(), start + candidate.length()));
        onInput(next, inputField.getCursorPosition());
    }
}
