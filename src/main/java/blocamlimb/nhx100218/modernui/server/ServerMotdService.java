package blocamlimb.nhx100218.modernui.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import blocamlimb.nhx100218.modernui.uilib.MyMod;

/**
 * Builds the server-list response from {@code config/MiniMOTD/main.conf} when
 * present, otherwise from the {@code server.motd} section of modernui.yaml.
 *
 * <p>1.7.10 clients receive the legacy (16-colour) MOTD plus ModernUI's marker.
 * 1.16+ clients receive a component with exact RGB colours, so MiniMOTD
 * gradients are displayed smoothly without a client mod.</p>
 */
public final class ServerMotdService {

    /** 1.16 introduced RGB component colours and {@code §x} legacy RGB. */
    private static final int HEX_COLOR_PROTOCOL = 735;

    private static final Map<NetworkManager, Integer> PROTOCOLS =
            Collections.synchronizedMap(new WeakHashMap<NetworkManager, Integer>());
    private static final Map<ServerStatusResponse, JsonObject> DESCRIPTIONS =
            Collections.synchronizedMap(new WeakHashMap<ServerStatusResponse, JsonObject>());

    private static Boolean enabledOverride;
    private static Selection cachedFallbackLegacy;
    private static Selection cachedFallbackHex;
    private static String cachedIcon;
    private static String cachedIconPath;
    private static long cachedIconStamp = -1L;
    private static long lastDiagnosticMs;

    private ServerMotdService() {}

    /** Remembers the protocol version from the status/login handshake. */
    public static void rememberProtocol(NetworkManager manager, int protocol) {
        if (manager != null) {
            PROTOCOLS.put(manager, Integer.valueOf(protocol));
        }
    }

    /** Returns the handshake protocol, or 5 (1.7.10) when it was not recorded. */
    public static int protocolFor(NetworkManager manager) {
        if (manager == null) {
            return 5;
        }
        Integer protocol = PROTOCOLS.get(manager);
        return protocol == null ? 5 : protocol.intValue();
    }

    /** RGB component produced for a modern status response, or null. */
    public static JsonObject directDescription(ServerStatusResponse response) {
        return response == null ? null : DESCRIPTIONS.get(response);
    }

    /** Writes the exact RGB description into an already serialized status JSON. */
    public static boolean applyDirectDescription(ServerStatusResponse response, JsonObject json) {
        JsonObject description = directDescription(response);
        if (description == null || json == null) {
            return false;
        }
        json.add("description", description);
        return true;
    }

    public static void setEnabledOverride(Boolean enabled) {
        enabledOverride = enabled;
    }

    public static void clearEnabledOverride() {
        enabledOverride = null;
    }

    public static void invalidate() {
        cachedFallbackLegacy = null;
        cachedFallbackHex = null;
        cachedIcon = null;
        cachedIconPath = null;
        cachedIconStamp = -1L;
        MiniMotdConfig.invalidate();
    }

    public static ServerStatusResponse apply(MinecraftServer server, ServerStatusResponse response) {
        return apply(server, response, 5);
    }

    public static ServerStatusResponse apply(MinecraftServer server, ServerStatusResponse response, int protocol) {
        if (!isEnabled() || response == null) {
            return response;
        }
        boolean hexColors = protocol >= HEX_COLOR_PROTOCOL;
        Selection selection = selection(hexColors);
        logDiagnostic(protocol, hexColors);
        // Copy the cached status so one ping's protocol/random MOTD cannot
        // leak into another client's response.
        ServerStatusResponse out = new ServerStatusResponse();
        out.func_151321_a(response.func_151322_c());
        if (server != null) {
            out.func_151319_a(
                    new ServerStatusResponse.PlayerCountData(
                            server.getMaxPlayers(),
                            server.getCurrentPlayerCount()));
        } else {
            out.func_151319_a(response.func_151318_b());
        }
        String icon = selection.icon != null ? selection.icon : response.func_151316_d();
        if (icon != null) {
            out.func_151320_a(icon);
        }
        // Keep the legacy fallback in the component: if the serializer mixin
        // is unavailable the client still sees the normal MOTD.
        out.func_151315_a(new ChatComponentText(selection.legacyText));
        if (selection.description != null) {
            DESCRIPTIONS.put(out, selection.description);
        }
        return out;
    }

    public static boolean isEnabled() {
        if (enabledOverride != null) {
            return enabledOverride.booleanValue();
        }
        MiniMotdConfig.Data mini = MiniMotdConfig.current();
        if (mini != null && mini.enabled) {
            return true;
        }
        // A generated/disabled MiniMOTD config must not suppress the YAML MOTD.
        return ServerFeatureConfig.motdEnabled;
    }

    /** Legacy (0xFE) ping text, including the same MiniMOTD/YAML content. */
    public static String legacyMotd(MinecraftServer server) {
        if (!isEnabled()) {
            return server == null ? "" : server.getMOTD();
        }
        logLegacyDiagnostic();
        String text = selection(false).legacyText;
        int payload = text.indexOf(LegacyMotdText.EXACT_MARKER);
        return payload < 0 ? text : text.substring(0, payload);
    }

    private static void logDiagnostic(int protocol, boolean hexColors) {
        long now = System.currentTimeMillis();
        if (now - lastDiagnosticMs < 5000L) {
            return;
        }
        lastDiagnosticMs = now;
        MiniMotdConfig.Data mini = MiniMotdConfig.current();
        MyMod.LOG.info("MOTD ping: protocol={} rgb={} mini={} yaml={}",
                Integer.valueOf(protocol), Boolean.valueOf(hexColors),
                Boolean.valueOf(mini != null && mini.enabled),
                Boolean.valueOf(ServerFeatureConfig.motdEnabled));
        if (mini != null && mini.enabled && !mini.motds.isEmpty()) {
            MiniMotdConfig.Entry entry = mini.motds.get(0);
            MyMod.LOG.info("MOTD source[0]: line1='{}' line2='{}'", entry.line1, entry.line2);
        }
    }

    private static void logLegacyDiagnostic() {
        long now = System.currentTimeMillis();
        if (now - lastDiagnosticMs < 5000L) {
            return;
        }
        lastDiagnosticMs = now;
        MyMod.LOG.info("MOTD ping: legacy 0xFE (no protocol; legacy colours only)");
    }

    private static Selection selection(boolean hexColors) {
        MiniMotdConfig.Data mini = MiniMotdConfig.current();
        if (mini != null && mini.enabled && !mini.motds.isEmpty()) {
            int index = mini.motds.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(mini.motds.size());
            MiniMotdConfig.Entry entry = mini.motds.get(index);
            String raw1 = ServerMetrics.expand(entry.line1);
            String raw2 = ServerMetrics.expand(entry.line2);
            String legacy1 = LegacyMotdText.render(raw1, false);
            String legacy2 = LegacyMotdText.render(raw2, false);
            if (!LegacyMotdText.hasVisibleText(legacy1) && !LegacyMotdText.hasVisibleText(legacy2)) {
                MyMod.LOG.warn("MiniMOTD 配置渲染结果为空，回退到 modernui.yaml MOTD: line1={}, line2={}",
                        entry.line1, entry.line2);
                raw1 = ServerMetrics.expand(ServerFeatureConfig.motdLine1);
                raw2 = ServerMetrics.expand(ServerFeatureConfig.motdLine2);
                legacy1 = LegacyMotdText.render(raw1, false);
                legacy2 = LegacyMotdText.render(raw2, false);
            }
            if (!LegacyMotdText.hasVisibleText(legacy1) && !LegacyMotdText.hasVisibleText(legacy2)) {
                raw1 = "ModernUI-GTNH";
                raw2 = "";
                legacy1 = "\u00a7bModernUI-GTNH";
                legacy2 = "";
            }
            String icon = mini.iconsEnabled ? miniIcon(entry.icon) : null;
            return build(raw1, raw2, legacy1, legacy2, icon, hexColors);
        }
        return fallback(hexColors);
    }

    private static Selection fallback(boolean hexColors) {
        Selection cached = hexColors ? cachedFallbackHex : cachedFallbackLegacy;
        if (cached != null) {
            return cached;
        }
        String raw1 = ServerMetrics.expand(ServerFeatureConfig.motdLine1);
        String raw2 = ServerMetrics.expand(ServerFeatureConfig.motdLine2);
        String legacy1 = LegacyMotdText.render(raw1, false);
        String legacy2 = LegacyMotdText.render(raw2, false);
        Selection selection = build(raw1, raw2, legacy1, legacy2, fallbackIcon(), hexColors);
        if (hexColors) {
            cachedFallbackHex = selection;
        } else {
            cachedFallbackLegacy = selection;
        }
        return selection;
    }

    private static Selection build(String raw1, String raw2, String legacy1, String legacy2, String icon,
            boolean hexColors) {
        String text = legacy2.isEmpty() ? legacy1 : legacy1 + "\n" + legacy2;
        String legacy = LegacyMotdText.MARKER + text;
        if (!hexColors) {
            // 1.7.10 only understands 16 colours. Append the exact per-glyph
            // colours invisibly; ModernUI reads them, vanilla skips the codes.
            legacy += LegacyMotdText.exactColorPayload(raw1, raw2);
        }
        JsonObject description = hexColors ? LegacyMotdText.renderJson(raw1, raw2) : null;
        return new Selection(legacy, description, icon);
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

        private final String legacyText;
        private final JsonObject description;
        private final String icon;

        Selection(String legacyText, JsonObject description, String icon) {
            this.legacyText = legacyText;
            this.description = description;
            this.icon = icon;
        }
    }
}
