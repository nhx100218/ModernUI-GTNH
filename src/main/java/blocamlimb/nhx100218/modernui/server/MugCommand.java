package blocamlimb.nhx100218.modernui.server;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * {@code /mug} administration command. Grouped form:
 * {@code /mug status|reload|tab ...|motd ...}.
 */
public final class MugCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "mug";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mug <status|reload|tab|motd> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            status(sender);
            return;
        }
        String group = args[0].toLowerCase(Locale.ROOT);
        switch (group) {
            case "status":
                status(sender);
                break;
            case "reload":
                reload(sender);
                break;
            case "tab":
                tab(sender, args);
                break;
            case "motd":
                motd(sender, args);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private void status(ICommandSender sender) {
        info(sender, "ModernUI-GTNH 服务端状态");
        info(sender, "TPS: " + ServerMetrics.formatTps() + "   MSPT: " + ServerMetrics.formatMspt());
        info(sender, "Tab: " + onOff(ServerFeatureConfig.tabEnabled)
                + "   ServerUtilities: " + (ServerTabService.available() ? "已安装" : "未安装"));
        info(sender, "MOTD: " + onOff(ServerMotdService.isEnabled())
                + "   MiniMOTD 配置: " + (MiniMotdConfig.isPresent() ? "已加载" : "未使用"));
        if (ServerFeatureConfig.configFile() != null) {
            info(sender, "配置: " + ServerFeatureConfig.configFile().getAbsolutePath());
        }
    }

    private void reload(ICommandSender sender) {
        if (ServerFeatures.reload()) {
            success(sender, "服务端配置已重新加载");
        } else {
            error(sender, "重新加载失败，请查看日志");
        }
    }

    private void tab(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mug tab <on|off>");
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!"on".equals(action) && !"off".equals(action)) {
            throw new WrongUsageException("/mug tab <on|off>");
        }
        boolean enable = "on".equals(action);
        if (enable && !ServerTabService.available()) {
            error(sender, "未安装 ServerUtilities，不能启用 tab 显示");
            return;
        }
        if (persist(sender, ServerFeatureConfig.TAB_ENABLED, Boolean.valueOf(enable), "tab.enabled")) {
            ServerTabService.forceUpdate();
        }
    }

    private void motd(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mug motd <on|off|line1|line2|icon> [值]");
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on":
            case "off":
                boolean enable = "on".equals(action);
                if (MiniMotdConfig.isPresent()) {
                    ServerMotdService.setEnabledOverride(Boolean.valueOf(enable));
                    success(sender, "已设置 MOTD 运行时" + onOff(enable)
                            + "；重启后以 config/MiniMOTD/main.conf 为准");
                } else if (persist(sender, ServerFeatureConfig.MOTD_ENABLED,
                        Boolean.valueOf(enable), "motd.enabled")) {
                    ServerMotdService.invalidate();
                }
                break;
            case "line1":
                if (persist(sender, ServerFeatureConfig.MOTD_LINE1, join(args, 2), "motd.line1")) {
                    ServerMotdService.invalidate();
                }
                break;
            case "line2":
                if (persist(sender, ServerFeatureConfig.MOTD_LINE2, join(args, 2), "motd.line2")) {
                    ServerMotdService.invalidate();
                }
                break;
            case "icon":
                if (persist(sender, ServerFeatureConfig.MOTD_ICON, join(args, 2), "motd.icon")) {
                    ServerMotdService.invalidate();
                }
                break;
            default:
                throw new WrongUsageException("/mug motd <on|off|line1|line2|icon> [值]");
        }
    }

    private boolean persist(ICommandSender sender, String path, Object value, String label) {
        if (ServerFeatureConfig.persist(path, value)) {
            success(sender, "已更新 " + label + " = " + value);
            return true;
        }
        error(sender, "保存 " + label + " 失败，请查看日志");
        return false;
    }

    private static String join(String[] args, int from) {
        if (from >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static String onOff(boolean value) {
        return value ? "开启" : "关闭";
    }

    private static void info(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + text));
    }

    private static void success(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + text));
    }

    private static void error(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + text));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "reload", "tab", "motd");
        }
        if (args.length == 2 && "tab".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        if (args.length == 2 && "motd".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "line1", "line2", "icon");
        }
        return null;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("modernuiadmin");
    }
}
