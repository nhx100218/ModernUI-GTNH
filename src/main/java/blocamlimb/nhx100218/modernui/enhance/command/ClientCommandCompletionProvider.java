package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;

/**
 * Client-side source for the modern command suggestion popup.
 *
 * <p>Command names and sub-commands come from Forge's
 * {@link ClientCommandHandler}; arguments include online player names. Server
 * completions received through the classic Tab path are merged into the same
 * model by {@code ChatInputBar}.</p>
 */
public final class ClientCommandCompletionProvider implements CommandCompletionProvider {

    @Override
    public List<CommandCompletionEntry> collect(String input, int cursor, int limit) {
        if (input == null || !input.startsWith("/") || limit <= 0) {
            return Collections.emptyList();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return Collections.emptyList();
        }
        int caret = Math.max(1, Math.min(cursor <= 0 ? input.length() : cursor, input.length()));
        String before = input.substring(1, caret);
        String word = currentWord(before);
        List<CommandCompletionEntry> entries = new ArrayList<CommandCompletionEntry>();

        boolean commandName = before.indexOf(' ') < 0;
        if (commandName) {
            collectCommands(mc, word, entries, limit);
        } else {
            collectArguments(mc, before, word, entries, limit);
        }
        return entries;
    }

    private static void collectCommands(Minecraft mc, String word, List<CommandCompletionEntry> out, int limit) {
        // Merge client-side commands with the server command list learned from
        // a C14 response, so typing "g" shows the same entries as Tab would.
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        try {
            List<String> options = ClientCommandHandler.instance.getPossibleCommands(mc.thePlayer, word);
            if (options != null) {
                for (String option : options) {
                    if (option == null) {
                        continue;
                    }
                    String clean = EnumChatFormatting.getTextWithoutFormattingCodes(option).trim();
                    if (clean.isEmpty()) {
                        continue;
                    }
                    names.add(clean.startsWith("/") ? clean.substring(1) : clean);
                }
            }
        } catch (Throwable ignored) {
            // A broken client command must not break chat input.
        }
        synchronized (SERVER_COMMANDS) {
            for (String name : SERVER_COMMANDS) {
                if (matches(name, word)) {
                    names.add(name);
                }
            }
        }
        for (String name : names) {
            if (out.size() >= limit) {
                return;
            }
            out.add(new CommandCompletionEntry("/" + name, "Command", CommandCompletionEntry.Kind.COMMAND,
                    1000 - out.size()));
        }
    }

    private static void collectArguments(Minecraft mc, String before, String word, List<CommandCompletionEntry> out,
            int limit) {
        int firstSpace = before.indexOf(' ');
        String command = firstSpace >= 0
                ? before.substring(0, firstSpace).trim().toLowerCase(Locale.ROOT) : "";
        if (("gamemode".equals(command) || "gm".equals(command))
                && firstSpace >= 0 && firstSpace == before.lastIndexOf(' ')) {
            addGamemodeSuggestions(word, out, limit);
            return;
        }
        // Sub-commands / argument hints registered by the client command, if any.
        try {
            List<String> options = ClientCommandHandler.instance.getPossibleCommands(mc.thePlayer, before);
            if (options != null) {
                for (String option : options) {
                    if (option == null || option.isEmpty() || out.size() >= limit) {
                        continue;
                    }
                    out.add(new CommandCompletionEntry(option, "Argument", CommandCompletionEntry.Kind.ARGUMENT,
                            500 - out.size()));
                }
            }
        } catch (Throwable ignored) {
            // Ignore malformed command trees.
        }
        // Player names are only offered where the command expects a player.
        int spaces = 0;
        for (int i = 0; i < before.length(); i++) {
            if (before.charAt(i) == ' ') {
                spaces++;
            }
        }
        int argIndex = Math.max(0, spaces - 1);
        if ("give".equals(command) && argIndex == 1) {
            addItemSuggestions(word, out, limit);
        }
        Set<String> players = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        if (isPlayerArgument(command, argIndex)) {
            if (mc.thePlayer.sendQueue != null) {
                try {
                    for (Object infoObject : mc.thePlayer.sendQueue.playerInfoList) {
                        if (!(infoObject instanceof GuiPlayerInfo)) {
                            continue;
                        }
                        GuiPlayerInfo info = (GuiPlayerInfo) infoObject;
                        if (info.name != null && matches(info.name, word)) {
                            players.add(info.name);
                        }
                    }
                } catch (Throwable ignored) {
                    // Player list may be rebuilding; ignore this frame.
                }
            }
            EntityPlayer player = mc.thePlayer;
            if (player != null && matches(player.getCommandSenderName(), word)) {
                players.add(player.getCommandSenderName());
            }
        }
        for (String name : players) {
            if (out.size() >= limit) {
                return;
            }
            out.add(new CommandCompletionEntry(name, "Player", CommandCompletionEntry.Kind.PLAYER,
                    100 - out.size()));
        }
    }

    private static boolean isPlayerArgument(String command, int argIndex) {
        if (command == null) {
            return false;
        }
        switch (command) {
            case "give":
            case "kill":
            case "msg":
            case "tell":
            case "w":
            case "op":
            case "deop":
            case "kick":
            case "ban":
            case "pardon":
                return argIndex == 0;
            case "gamemode":
            case "gm":
                return argIndex == 1;
            case "tp":
            case "tpx":
            case "tphere":
            case "teleport":
                return argIndex <= 1;
            default:
                return false;
        }
    }

    private static void addItemSuggestions(String word, List<CommandCompletionEntry> out, int limit) {
        // Keep only the alphabetically first candidates instead of sorting the
        // whole registry on every keystroke (GTNH registers tens of thousands
        // of item ids).
        TreeSet<String> ids = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        int keep = Math.max(limit, 1) + 1;
        try {
            collectRegistryKeys(Item.itemRegistry.getKeys(), word, ids, keep);
            collectRegistryKeys(Block.blockRegistry.getKeys(), word, ids, keep);
        } catch (Throwable ignored) {
            return;
        }
        for (String id : ids) {
            if (out.size() >= limit) {
                return;
            }
            out.add(new CommandCompletionEntry(id, "Item", CommandCompletionEntry.Kind.ARGUMENT,
                    500 - out.size()));
        }
    }

    private static void collectRegistryKeys(Set<?> keys, String word, TreeSet<String> ids, int keep) {
        for (Object keyObject : keys) {
            if (!(keyObject instanceof String)) {
                continue;
            }
            String id = (String) keyObject;
            if (!matches(id, word)) {
                continue;
            }
            ids.add(id);
            if (ids.size() > keep) {
                ids.pollLast();
            }
        }
    }

    private static void addGamemodeSuggestions(String word, List<CommandCompletionEntry> out, int limit) {
        String[] base = {"survival", "creative", "adventure"};
        for (String mode : base) {
            if (out.size() >= limit) {
                return;
            }
            if (matches(mode, word)) {
                out.add(new CommandCompletionEntry(mode, "Game mode", CommandCompletionEntry.Kind.ARGUMENT,
                        1000 - out.size()));
            }
        }
        if (supportsSpectator() && out.size() < limit && matches("spectator", word)) {
            out.add(new CommandCompletionEntry("spectator", "Game mode (EFR)",
                    CommandCompletionEntry.Kind.ARGUMENT, 1000 - out.size()));
        }
    }

    private static Boolean efrSpectator;

    /** Server command names learned from C14 tab-complete responses. */
    private static final Set<String> SERVER_COMMANDS = new HashSet<String>();
    private static final long COMMAND_CACHE_MS = 5L * 60L * 1000L;
    private static long serverCommandsAt;

    /** True when the full server command list should be requested again. */
    public static boolean needsCommandRefresh() {
        synchronized (SERVER_COMMANDS) {
            return SERVER_COMMANDS.isEmpty()
                    || System.currentTimeMillis() - serverCommandsAt > COMMAND_CACHE_MS;
        }
    }

    /**
     * Stores server command names from a C14 response. Only entries carrying the
     * leading slash are commands; argument suggestions never update the cache.
     */
    public static void learnCommands(String[] options) {
        if (options == null) {
            return;
        }
        boolean learned = false;
        synchronized (SERVER_COMMANDS) {
            for (String option : options) {
                if (option == null) {
                    continue;
                }
                String clean = EnumChatFormatting.getTextWithoutFormattingCodes(option).trim();
                if (!clean.startsWith("/")) {
                    continue;
                }
                clean = clean.substring(1).trim();
                int space = clean.indexOf(' ');
                if (space >= 0) {
                    clean = clean.substring(0, space);
                }
                if (!clean.isEmpty() && SERVER_COMMANDS.add(clean)) {
                    learned = true;
                }
            }
            if (learned) {
                serverCommandsAt = System.currentTimeMillis();
            }
        }
    }

    /** Marks the full command list as requested, even when the server returned none. */
    public static void markCommandListFetched() {
        synchronized (SERVER_COMMANDS) {
            serverCommandsAt = System.currentTimeMillis();
        }
    }

    public static void clearServerCommands() {
        synchronized (SERVER_COMMANDS) {
            SERVER_COMMANDS.clear();
            serverCommandsAt = 0L;
        }
    }

    /** True when Et Futurum Requiem is installed and can run spectator mode. */
    public static boolean supportsSpectator() {
        if (efrSpectator == null) {
            try {
                Class.forName("ganymedes01.etfuturum.spectator.SpectatorMode");
                efrSpectator = Boolean.TRUE;
            } catch (Throwable ignored) {
                efrSpectator = Boolean.FALSE;
            }
        }
        return efrSpectator.booleanValue();
    }

    private static String commandUsage(Minecraft mc, String name) {
        try {
            ICommand command = ClientCommandHandler.instance.getCommands().get(name);
            if (command != null) {
                String usage = command.getCommandUsage(mc.thePlayer);
                return usage == null ? "Command" : usage;
            }
        } catch (Throwable ignored) {
            // Ignore broken command metadata.
        }
        return "Command";
    }

    private static String currentWord(String before) {
        int space = before.lastIndexOf(' ');
        return space < 0 ? before : before.substring(space + 1);
    }

    private static boolean matches(String value, String word) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith(word.toLowerCase(Locale.ROOT));
    }
}
