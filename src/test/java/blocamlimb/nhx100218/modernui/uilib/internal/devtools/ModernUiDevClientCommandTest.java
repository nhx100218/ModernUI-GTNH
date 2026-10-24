package blocamlimb.nhx100218.modernui.uilib.internal.devtools;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * Contract tests for the development-only {@code /modernui} command.
 */
public class ModernUiDevClientCommandTest {

    @Test
    public void shouldExposeCommandMetadataAndTabCompletion() {
        ModernUiClientCommand command = new ModernUiClientCommand();
        RecordingSender sender = new RecordingSender();

        Assert.assertEquals("modernui", command.getCommandName());
        Assert.assertEquals("/modernui <test|glass|modernconfig>", command.getCommandUsage(sender));
        Assert.assertEquals(0, command.getRequiredPermissionLevel());
        Assert.assertTrue(command.addTabCompletionOptions(sender, new String[] { "te" }).contains("test"));
        Assert.assertTrue(command.addTabCompletionOptions(sender, new String[] { "gl" }).contains("glass"));
        Assert.assertTrue(command.addTabCompletionOptions(sender, new String[] { "mod" }).contains("modernconfig"));
        Assert.assertTrue(command.addTabCompletionOptions(sender, new String[] { "te", "extra" }).isEmpty());
    }

    @Test
    public void resolveSubcommandParsesKnownAndRejectsUnknown() {
        Assert.assertEquals(ModernUiClientCommand.Subcommand.TEST,
                ModernUiClientCommand.resolveSubcommand(new String[] { "test" }));
        Assert.assertEquals(ModernUiClientCommand.Subcommand.TEST,
                ModernUiClientCommand.resolveSubcommand(new String[] { "TEST" }));
        Assert.assertEquals(ModernUiClientCommand.Subcommand.GLASS,
                ModernUiClientCommand.resolveSubcommand(new String[] { "glass" }));
        Assert.assertEquals(ModernUiClientCommand.Subcommand.GLASS,
                ModernUiClientCommand.resolveSubcommand(new String[] { "GLASS" }));
        Assert.assertEquals(ModernUiClientCommand.Subcommand.MODERN_CONFIG,
                ModernUiClientCommand.resolveSubcommand(new String[] { "modernconfig" }));
        Assert.assertEquals(ModernUiClientCommand.Subcommand.MODERN_CONFIG,
                ModernUiClientCommand.resolveSubcommand(new String[] { "ModernConfig" }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[0]));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { "inventory" }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { "chatmd" }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { "chatmd", "toggle" }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { "test", "modernconfig" }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(null));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { null }));
        Assert.assertNull(ModernUiClientCommand.resolveSubcommand(new String[] { "chatmd", null }));
    }

    @Test
    public void shouldRejectArgumentsOtherThanKnownSubcommands() {
        ModernUiClientCommand command = new ModernUiClientCommand();
        RecordingSender sender = new RecordingSender();

        assertUsageRejected(command, sender, new String[0]);
        assertUsageRejected(command, sender, new String[] { "inventory" });
        assertUsageRejected(command, sender, new String[] { "chatmd", "toggle" });

        Assert.assertTrue(sender.messages.isEmpty());
    }

    private static void assertUsageRejected(ModernUiClientCommand command, RecordingSender sender, String[] args) {
        try {
            command.processCommand(sender, args);
            Assert.fail("Expected WrongUsageException");
        } catch (WrongUsageException expected) {
            Assert.assertEquals("/modernui <test|glass|modernconfig>", expected.getMessage());
        }
    }

    private static final class RecordingSender implements ICommandSender {

        private final List<IChatComponent> messages = new ArrayList<IChatComponent>();

        @Override
        public String getCommandSenderName() {
            return "tester";
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messages.add(message);
        }

        @Override
        public boolean canCommandSenderUseCommand(int permissionLevel, String command) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }
    }
}
