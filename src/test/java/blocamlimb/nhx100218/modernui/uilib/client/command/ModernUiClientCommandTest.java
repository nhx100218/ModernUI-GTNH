package blocamlimb.nhx100218.modernui.uilib.client.command;

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
 * Contract tests for the player-visible {@code /modernui} command.
 */
public class ModernUiClientCommandTest {

    @Test
    public void shouldExposeOnlyPlayerFacingSubcommands() {
        ModernUiClientCommand command = new ModernUiClientCommand();
        RecordingSender sender = new RecordingSender();

        Assert.assertEquals("modernui", command.getCommandName());
        Assert.assertEquals("/modernui modernconfig", command.getCommandUsage(sender));
        Assert.assertEquals(0, command.getRequiredPermissionLevel());

        Assert.assertTrue(command.addTabCompletionOptions(sender, new String[] { "mod" }).contains("modernconfig"));
        Assert.assertTrue("dev-only subcommands must not leak into the release command",
                command.addTabCompletionOptions(sender, new String[] { "te" }).isEmpty());
        Assert.assertTrue("dev-only subcommands must not leak into the release command",
                command.addTabCompletionOptions(sender, new String[] { "gl" }).isEmpty());
    }

    @Test
    public void shouldRejectUnknownArgumentsWithFixedUsage() {
        ModernUiClientCommand command = new ModernUiClientCommand();
        RecordingSender sender = new RecordingSender();

        assertUsageRejected(command, sender, new String[0]);
        assertUsageRejected(command, sender, new String[] { "inventory" });
        assertUsageRejected(command, sender, new String[] { "test" });
        assertUsageRejected(command, sender, new String[] { "chatmd", "toggle" });

        Assert.assertTrue(sender.messages.isEmpty());
    }

    private static void assertUsageRejected(ModernUiClientCommand command, RecordingSender sender, String[] args) {
        try {
            command.processCommand(sender, args);
            Assert.fail("Expected WrongUsageException");
        } catch (WrongUsageException expected) {
            Assert.assertEquals("/modernui modernconfig", expected.getMessage());
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
