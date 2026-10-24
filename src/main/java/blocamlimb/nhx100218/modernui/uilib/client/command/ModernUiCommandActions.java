package blocamlimb.nhx100218.modernui.uilib.client.command;

import blocamlimb.nhx100218.modernui.uilib.config.modern.ModernConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/**
 * Shared implementation for the player-visible {@code /modernui} command.
 */
public final class ModernUiCommandActions {

    private static final String CLIENT_UNAVAILABLE = "ModernUI: client is not available.";

    private ModernUiCommandActions() {}

    /**
     * Opens the single ModernUI configuration page.
     *
     * @param sender command sender, used for the unavailable-client notice
     */
    public static void openModernConfig(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText(CLIENT_UNAVAILABLE));
            return;
        }
        ModernConfigEntry.open();
    }
}
