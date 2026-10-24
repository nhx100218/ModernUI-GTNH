package blocamlimb.nhx100218.modernui.uilib.client.command;

import blocamlimb.nhx100218.modernui.uilib.config.modern.ModernConfigEntry;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.ChatMarkdownSettings;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.wiring.ChatMarkdownInstaller;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/**
 * 玩家可见的 {@code /modernui} 子命令动作。
 *
 * <p>发布产物内的通道命令（{@link ModernUiClientCommand}）与开发环境的完整命令
 * （{@code internal.devtools.ModernUiDevClientCommand}，随该包排除出发布产物）共用本类：
 * 两者的子命令集不同，但 modernconfig 与 chatmd 的行为必须逐字一致，故实现只留这一处。</p>
 */
public final class ModernUiCommandActions {

    private static final String CLIENT_UNAVAILABLE = "ModernUI: 当前客户端不可用。";

    private ModernUiCommandActions() {}

    /**
     * 打开新架构配置页（实验性）。
     *
     * <p>uilib 作为新架构配置页的第一个真实使用方，经 {@link ModernConfigEntry} 接入。
     * 接入代码位于 {@code uilib.config.modern} 专门包，依据决策 {@code ee1e181d}
     * 可直接 import {@code config.ui.*}（含 ConfigUI.open），不再需要反射。</p>
     *
     * @param sender 命令发送者，用于客户端不可用时提示
     */
    public static void openModernConfig(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            sender.addChatMessage(new ChatComponentText(CLIENT_UNAVAILABLE));
            return;
        }
        ModernConfigEntry.open();
    }

    /**
     * 启用聊天 3.0 接管(开关置开并立即装配一次;后续渲染帧幂等维持)。
     *
     * @param sender 命令发送者
     */
    public static void enableChatTakeover(ICommandSender sender) {
        ChatMarkdownSettings.setEnabled(true);
        ChatMarkdownInstaller.installIfNeeded();
        sender.addChatMessage(new ChatComponentText("ModernUI: 聊天 3.0 接管已启用。"));
    }

    /**
     * 关闭聊天 3.0 接管(开关置关并立即回退原版实例;逃生舱)。
     *
     * @param sender 命令发送者
     */
    public static void disableChatTakeover(ICommandSender sender) {
        ChatMarkdownSettings.setEnabled(false);
        ChatMarkdownInstaller.installIfNeeded();
        sender.addChatMessage(new ChatComponentText("ModernUI: 聊天 3.0 接管已关闭,回退原版对话框。"));
    }

    /**
     * 输出接管状态诊断(开关/接管状态)。
     *
     * @param sender 命令发送者
     */
    public static void reportChatStatus(ICommandSender sender) {
        StringBuilder report = new StringBuilder("ModernUI 聊天 3.0:开关=")
                .append(ChatMarkdownSettings.isEnabled() ? "开" : "关")
                .append(" | 接管状态=")
                .append(ChatMarkdownInstaller.isInstalled() ? "已接管" : "未接管");
        if (ChatMarkdownSettings.isEnabled() && !ChatMarkdownInstaller.isInstalled()) {
            report.append("(等待渲染帧装配)");
        }
        sender.addChatMessage(new ChatComponentText(report.toString()));
    }
}
