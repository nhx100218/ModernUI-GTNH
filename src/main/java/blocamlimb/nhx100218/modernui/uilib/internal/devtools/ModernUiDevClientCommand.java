package blocamlimb.nhx100218.modernui.uilib.internal.devtools;

import java.util.Collections;
import java.util.List;

import blocamlimb.nhx100218.modernui.uilib.client.command.ModernUiCommandActions;
import blocamlimb.nhx100218.modernui.uilib.internal.devtools.glass.GlassLabEntry;
import blocamlimb.nhx100218.modernui.uilib.internal.devtools.playground.TestPlaygroundEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

/**
 * Development-only {@code /modernui} command. This class lives in
 * {@code internal.devtools} and is excluded from the release jar.
 */
final class ModernUiClientCommand extends CommandBase {

    private static final String COMMAND_NAME = "modernui";
    private static final String SUBCOMMAND_TEST = "test";
    private static final String SUBCOMMAND_GLASS = "glass";
    private static final String SUBCOMMAND_MODERN_CONFIG = "modernconfig";
    private static final String COMMAND_USAGE = "/modernui <test|glass|modernconfig>";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return COMMAND_USAGE;
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Subcommand subcommand = resolveSubcommand(args);
        if (subcommand == null) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        switch (subcommand) {
            case TEST:
                openTestPlayground(sender);
                break;
            case GLASS:
                openGlassLab(sender);
                break;
            case MODERN_CONFIG:
                openModernConfig(sender);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    static Subcommand resolveSubcommand(String[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        if (args.length == 1) {
            if (SUBCOMMAND_TEST.equalsIgnoreCase(args[0])) {
                return Subcommand.TEST;
            }
            if (SUBCOMMAND_GLASS.equalsIgnoreCase(args[0])) {
                return Subcommand.GLASS;
            }
            if (SUBCOMMAND_MODERN_CONFIG.equalsIgnoreCase(args[0])) {
                return Subcommand.MODERN_CONFIG;
            }
        }
        return null;
    }

    private void openTestPlayground(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText("ModernUI: client is not available."));
            return;
        }
        TestPlaygroundEntry.open();
    }

    private void openGlassLab(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText("ModernUI: client is not available."));
            return;
        }
        GlassLabEntry.open();
    }

    private void openModernConfig(ICommandSender sender) {
        ModernUiCommandActions.openModernConfig(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args != null && args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMAND_TEST, SUBCOMMAND_GLASS,
                    SUBCOMMAND_MODERN_CONFIG);
        }
        return Collections.emptyList();
    }

    enum Subcommand {
        TEST,
        GLASS,
        MODERN_CONFIG
    }
}
