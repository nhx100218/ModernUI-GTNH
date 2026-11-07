package blocamlimb.nhx100218.modernui.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import blocamlimb.nhx100218.modernui.uilib.MyMod;

/**
 * Builds the server-list response from {@code config/MiniMOTD/main.conf} when
 * present, otherwise from the {@code server.motd} section of modernui.yaml.
 */
public final class ServerMotdService {

    private static Boolean enabledOverride;
    private static String cachedText;
    private static String cachedIcon;
    private static String cachedIconPath;
    private static long cachedIconStamp = -1L;

    private ServerMotdService() {}

    public static void setEnabledOverride(Boolean enabled) {
        enabledOverride = enabled;
    }

    public static void clearEnabledOverride() {
        enabledOverride = null;
    }

    public static void invalidate() {
        cachedText = null;
        cachedIcon = null;
        cachedIconPath = null;
        cachedIconStamp = -1L;
        MiniMotdConfig.invalidate();
    }

    public static ServerStatusResponse apply(MinecraftServer server, ServerStatusResponse response) {
        if (!isEnabled() || response == null) {
            return response;
        }
        Selection selection = selection();
        response.func_151315_a(new ChatComponentText(selection.text));
        if (server != null) {
            response.func_151319_a(
                    new ServerStatusResponse.PlayerCountData(
                            server.getMaxPlayers(),
                            server.getCurrentPlayerCount()));
        }
        if (selection.icon != null) {
            response.func_151320_a(selection.icon);
        }
        return response;
    }

    public static boolean isEnabled() {
        if (enabledOverride != null) {
            return enabledOverride.booleanValue();
        }
        MiniMotdConfig.Data mini = MiniMotdConfig.current();
        if (mini != null) {
            return mini.enabled;
        }
        return ServerFeatureConfig.motdEnabled;
    }

    private static Selection selection() {
        MiniMotdConfig.Data mini = MiniMotdConfig.current();
        if (mini != null && !mini.motds.isEmpty()) {
            int index = mini.motds.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(mini.motds.size());
            MiniMotdConfig.Entry entry = mini.motds.get(index);
            String line1 = LegacyMotdText.render(ServerMetrics.expand(entry.line1));
            String line2 = LegacyMotdText.render(ServerMetrics.expand(entry.line2));
            if (!LegacyMotdText.hasVisibleText(line1) && !LegacyMotdText.hasVisibleText(line2)) {
                MyMod.LOG.warn("MiniMOTD 配置渲染结果为空，回退到 modernui.yaml MOTD: line1={}, line2={}",
                        entry.line1, entry.line2);
                line1 = LegacyMotdText.render(ServerMetrics.expand(ServerFeatureConfig.motdLine1));
                line2 = LegacyMotdText.render(ServerMetrics.expand(ServerFeatureConfig.motdLine2));
            }
            if (!LegacyMotdText.hasVisibleText(line1) && !LegacyMotdText.hasVisibleText(line2)) {
                line1 = "\u00a7bModernUI-GTNH";
                line2 = "";
            }
            String text = LegacyMotdText.MARKER + (line2.isEmpty() ? line1 : line1 + "\n" + line2);
            String icon = mini.iconsEnabled ? miniIcon(entry.icon) : null;
            return new Selection(text, icon);
        }
        return new Selection(fallbackText(), fallbackIcon());
    }

    private static String fallbackText() {
        if (cachedText == null) {
            String line1 = LegacyMotdText.render(ServerMetrics.expand(ServerFeatureConfig.motdLine1));
            String line2 = LegacyMotdText.render(ServerMetrics.expand(ServerFeatureConfig.motdLine2));
            cachedText = LegacyMotdText.MARKER + (line2.isEmpty() ? line1 : line1 + "\n" + line2);
        }
        return cachedText;
    }

    private static String fallbackIcon() {
        String configured = ServerFeatureConfig.motdIcon;
        if (configured == null || configured.trim().isEmpty()) {
            return null;
        }
        File configDir = ServerFeatureConfig.configFile() == null
                ? new File("config")
                : ServerFeatureConfig.configFile().getParentFile();
        File file = resolveIconFile(configDir, configured.trim());
        return file == null ? null : dataUri(file);
    }

    private static String miniIcon(String configured) {
        File icons = MiniMotdConfig.iconsDirectory();
        if (icons == null || !icons.isDirectory()) {
            return null;
        }
        File[] pngs = icons.listFiles(
                (dir, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (pngs == null || pngs.length == 0) {
            return null;
        }
        if (configured == null || configured.isEmpty() || "random".equalsIgnoreCase(configured)) {
            return dataUri(pngs[ThreadLocalRandom.current().nextInt(pngs.length)]);
        }
        String name = configured.toLowerCase(Locale.ROOT).endsWith(".png")
                ? configured
                : configured + ".png";
        return dataUri(new File(icons, name));
    }

    private static File resolveIconFile(File configDir, String configured) {
        if (configured.indexOf('/') >= 0 || configured.indexOf('\\') >= 0) {
            return new File(configured);
        }
        String name = configured.toLowerCase(Locale.ROOT).endsWith(".png")
                ? configured
                : configured + ".png";
        File direct = new File(configDir, name);
        if (direct.isFile()) {
            return direct;
        }
        return new File(new File(configDir, "motd"), name);
    }

    private static String dataUri(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        long stamp = file.lastModified();
        if (cachedIcon != null && file.getAbsolutePath().equals(cachedIconPath) && stamp == cachedIconStamp) {
            return cachedIcon;
        }
        try {
            java.awt.image.BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() != 64 || image.getHeight() != 64) {
                MyMod.LOG.warn("MOTD 图标必须是 64x64 PNG: {}", file);
                return null;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            cachedIcon = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            cachedIconPath = file.getAbsolutePath();
            cachedIconStamp = stamp;
            return cachedIcon;
        } catch (Throwable t) {
            MyMod.LOG.warn("读取 MOTD 图标失败: {}", file, t);
            return null;
        }
    }

    private static final class Selection {

        private final String text;
        private final String icon;

        Selection(String text, String icon) {
            this.text = text;
            this.icon = icon;
        }
    }
}
