package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
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
        try {
            List<String> options = ClientCommandHandler.instance.getPossibleCommands(mc.thePlayer, word);
            if (options == null) {
                return;
            }
            for (String option : options) {
                if (option == null || option.isEmpty() || out.size() >= limit) {
                    continue;
                }
                String text = "/" + option;
                out.add(new CommandCompletionEntry(text, commandUsage(mc, option),
                        CommandCompletionEntry.Kind.COMMAND, 1000 - out.size()));
            }
        } catch (Throwable ignored) {
            // A broken client command must not break chat input.
        }
    }

    private static void collectArguments(Minecraft mc, String before, String word, List<CommandCompletionEntry> out,
            int limit) {
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
        // Player names are valid for many server commands (msg/tp/kill/...).
        if (mc.thePlayer.sendQueue != null) {
            try {
                for (Object infoObject : mc.thePlayer.sendQueue.playerInfoList) {
                    if (!(infoObject instanceof GuiPlayerInfo) || out.size() >= limit) {
                        continue;
                    }
                    GuiPlayerInfo info = (GuiPlayerInfo) infoObject;
                    if (info.name == null || !matches(info.name, word)) {
                        continue;
                    }
                    out.add(new CommandCompletionEntry(info.name, "Player", CommandCompletionEntry.Kind.PLAYER,
                            100 - out.size()));
                }
            } catch (Throwable ignored) {
                // Player list may be rebuilding; ignore this frame.
            }
        }
        EntityPlayer player = mc.thePlayer;
        if (player != null && out.size() < limit && matches(player.getCommandSenderName(), word)) {
            out.add(new CommandCompletionEntry(player.getCommandSenderName(), "Player",
                    CommandCompletionEntry.Kind.PLAYER, 50));
        }
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
