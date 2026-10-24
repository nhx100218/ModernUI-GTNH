package blocamlimb.nhx100218.modernui.uilib.client.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

/**
 * Player-visible {@code /modernui} client command.
 *
 * <p>The only public subcommand is {@code modernconfig}, which opens the
 * single ModernUI configuration page.</p>
 */
public final class ModernUiClientCommand extends CommandBase {

    private static final String COMMAND_NAME = "modernui";
    private static final String SUBCOMMAND_MODERN_CONFIG = "modernconfig";
    private static final String COMMAND_USAGE = "/modernui modernconfig";

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
        if (args != null && args.length == 1 && SUBCOMMAND_MODERN_CONFIG.equalsIgnoreCase(args[0])) {
            ModernUiCommandActions.openModernConfig(sender);
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args != null && args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMAND_MODERN_CONFIG);
        }
        return Collections.emptyList();
    }

    /** Test-visible subcommand token. */
    static String modernConfigSubcommand() {
        return SUBCOMMAND_MODERN_CONFIG;
    }
}
