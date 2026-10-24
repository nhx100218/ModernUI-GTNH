package blocamlimb.nhx100218.modernui.uilib.client.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

/**
 * 发布产物内的 {@code /modernui} 客户端命令。
 *
 * <p>只承载玩家可见通道：新架构配置页入口与聊天 3.0 接管开关（逃生舱）。开发设施
 * （scene 测试场地、磨玻璃实验室、网络自检端点、headless 出图）位于
 * {@code internal.devtools}，整包不进发布产物（门禁 {@code verifyDevToolsNotPackaged}），
 * 其完整命令由开发环境的装配入口注册——两者同名，同一环境只注册一个，见
 * {@code ClientProxy#registerModernUICommand()}。</p>
 */
public final class ModernUiClientCommand extends CommandBase {

    private static final String COMMAND_NAME = "modernui";
    private static final String SUBCOMMAND_MODERN_CONFIG = "modernconfig";
    private static final String SUBCOMMAND_CHATMD = "chatmd";
    private static final String ARG_ON = "on";
    private static final String ARG_OFF = "off";
    private static final String ARG_STATUS = "status";
    private static final String COMMAND_USAGE = "/modernui <modernconfig|chatmd on|off|status>";

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
            case MODERN_CONFIG:
                ModernUiCommandActions.openModernConfig(sender);
                break;
            case CHATMD_ON:
                ModernUiCommandActions.enableChatTakeover(sender);
                break;
            case CHATMD_OFF:
                ModernUiCommandActions.disableChatTakeover(sender);
                break;
            case CHATMD_STATUS:
                ModernUiCommandActions.reportChatStatus(sender);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    /**
     * 解析子命令（纯逻辑、无 MC/LWJGL 依赖）。
     *
     * @param args 命令参数（可为 null/空）
     * @return 命中的子命令；参数非法返回 null
     */
    static Subcommand resolveSubcommand(String[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        if (args.length == 1) {
            if (SUBCOMMAND_MODERN_CONFIG.equalsIgnoreCase(args[0])) {
                return Subcommand.MODERN_CONFIG;
            }
            return null;
        }
        if (args.length == 2 && SUBCOMMAND_CHATMD.equalsIgnoreCase(args[0]) && args[1] != null) {
            if (ARG_ON.equalsIgnoreCase(args[1])) {
                return Subcommand.CHATMD_ON;
            }
            if (ARG_OFF.equalsIgnoreCase(args[1])) {
                return Subcommand.CHATMD_OFF;
            }
            if (ARG_STATUS.equalsIgnoreCase(args[1])) {
                return Subcommand.CHATMD_STATUS;
            }
        }
        return null;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMAND_MODERN_CONFIG, SUBCOMMAND_CHATMD);
        }
        if (args.length == 2 && SUBCOMMAND_CHATMD.equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, ARG_ON, ARG_OFF, ARG_STATUS);
        }
        return Collections.emptyList();
    }

    /**
     * 已注册子命令枚举（供解析与测试引用）。
     */
    enum Subcommand {
        /** 打开新架构配置页。 */
        MODERN_CONFIG,
        /** 启用聊天 3.0 接管。 */
        CHATMD_ON,
        /** 关闭聊天 3.0 接管。 */
        CHATMD_OFF,
        /** 输出聊天 3.0 接管状态。 */
        CHATMD_STATUS
    }
}
