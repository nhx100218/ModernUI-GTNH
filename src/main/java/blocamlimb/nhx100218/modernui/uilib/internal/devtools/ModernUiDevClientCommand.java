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
 * ModernUI 内部开发工具客户端命令（开发环境完整版）。
 *
 * <p>本类随 {@code internal.devtools} 整包排除出发布产物，只在开发环境由
 * {@code DevToolsClientBootstrap} 经 {@code ClientProxy} 探测装配；发布产物内同名命令为
 * {@code client.command.ModernUiClientCommand}，只含玩家可见通道。同一环境只注册其中一个。</p>
 *
 * <p>子命令：</p>
 * <ul>
 *   <li>{@code test} —— 打开 scene 测试场地（{@link TestPlaygroundEntry#open()}），
 *       在游戏内验证文本输入/浮层/响应式能力；</li>
 *   <li>{@code glass} —— 打开磨玻璃实验室（{@link GlassLabEntry#open()}），
 *       backdrop-filter 仿 iOS 磨玻璃观感与渲染路径验收；</li>
 *   <li>{@code modernconfig} —— 打开新架构配置页调试入口（{@link ModernConfigEntry#open()}）；</li>
 *   <li>{@code chatmd on|off|status} —— 聊天 3.0 接管开关与状态诊断（on 启用/off 逃生舱回退原版/status 查看接管状态）。
 *       只改本次运行态、<b>不写配置</b>：持久真源是配置项 {@code general.chatFrame}
 *       （见 {@code blocamlimb.nhx100218.modernui.uilib.config.modern.ChatFrameConfig}），重启后回到该配置值。</li>
 * </ul>
 */
final class ModernUiClientCommand extends CommandBase {

    private static final String COMMAND_NAME = "modernui";
    private static final String SUBCOMMAND_TEST = "test";
    private static final String SUBCOMMAND_GLASS = "glass";
    private static final String SUBCOMMAND_MODERN_CONFIG = "modernconfig";
    private static final String SUBCOMMAND_CHATMD = "chatmd";
    private static final String ARG_ON = "on";
    private static final String ARG_OFF = "off";
    private static final String ARG_STATUS = "status";
    private static final String COMMAND_USAGE = "/modernui <test|glass|modernconfig|chatmd on|off|status>";

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
            case CHATMD_ON:
                enableChatTakeover(sender);
                break;
            case CHATMD_OFF:
                disableChatTakeover(sender);
                break;
            case CHATMD_STATUS:
                reportChatStatus(sender);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    /**
     * 解析子命令（纯逻辑、无 MC/LWJGL 依赖）。
     *
     * <p>拆出独立静态方法有两个目的：一是让命令解析成为可 headless 单测的纯函数
     * （JVM 测试不触碰 LWJGL 类）；二是 processCommand 保持薄壳。</p>
     *
     * @param args 命令参数（可为 null/空）
     * @return 命中的子命令；参数非法返回 null
     */
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

    /**
     * 打开 scene 测试场地。
     *
     * @param sender 命令发送者，用于客户端不可用时提示
     */
    private void openTestPlayground(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText("ModernUI: 当前客户端不可用。"));
            return;
        }
        TestPlaygroundEntry.open();
    }

    /**
     * 打开磨玻璃实验室。
     *
     * @param sender 命令发送者，用于客户端不可用时提示
     */
    private void openGlassLab(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText("ModernUI: 当前客户端不可用。"));
            return;
        }
        GlassLabEntry.open();
    }

    /**
     * 打开新架构配置页（实验性）。与发布产物内同名子命令共用
     * {@link ModernUiCommandActions#openModernConfig}，避免两处实现漂移。
     *
     * @param sender 命令发送者，用于客户端不可用时提示
     */
    private void openModernConfig(ICommandSender sender) {
        ModernUiCommandActions.openModernConfig(sender);
    }

    /**
     * 启用聊天 3.0 接管。与发布产物内同名子命令共用
     * {@link ModernUiCommandActions#enableChatTakeover}。
     *
     * @param sender 命令发送者
     */
    private void enableChatTakeover(ICommandSender sender) {
        ModernUiCommandActions.enableChatTakeover(sender);
    }

    /**
     * 关闭聊天 3.0 接管（逃生舱）。与发布产物内同名子命令共用
     * {@link ModernUiCommandActions#disableChatTakeover}。
     *
     * @param sender 命令发送者
     */
    private void disableChatTakeover(ICommandSender sender) {
        ModernUiCommandActions.disableChatTakeover(sender);
    }

    /**
     * 输出接管状态诊断。与发布产物内同名子命令共用
     * {@link ModernUiCommandActions#reportChatStatus}。
     *
     * @param sender 命令发送者
     */
    private void reportChatStatus(ICommandSender sender) {
        ModernUiCommandActions.reportChatStatus(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMAND_TEST, SUBCOMMAND_GLASS,
                    SUBCOMMAND_MODERN_CONFIG, SUBCOMMAND_CHATMD);
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
        /** 打开 scene 测试场地。 */
        TEST,
        /** 打开磨玻璃实验室。 */
        GLASS,
        /** 打开新架构配置页调试入口。 */
        MODERN_CONFIG,
        /** 聊天 3.0 接管开。 */
        CHATMD_ON,
        /** 聊天 3.0 接管关(逃生舱)。 */
        CHATMD_OFF,
        /** 聊天 3.0 接管状态。 */
        CHATMD_STATUS
    }
}
