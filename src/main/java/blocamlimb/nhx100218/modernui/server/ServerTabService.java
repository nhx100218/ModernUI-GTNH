package blocamlimb.nhx100218.modernui.server;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;

import com.google.gson.JsonObject;

import blocamlimb.nhx100218.modernui.uilib.MyMod;
import cpw.mods.fml.common.Loader;

/**
 * Appends one MSPT/TPS line to ServerUtilities' custom tab.
 *
 * <p>ServerUtilities keeps its own header/footer configuration and sends it on
 * {@code SU|TabHF}. We read those values and re-send them with one extra footer
 * line, so the server's custom content is preserved. If ServerUtilities is not
 * installed the feature stays disabled.</p>
 */
public final class ServerTabService {

    public static final String CHANNEL = "SU|TabHF";
    public static final String SERVER_UTILITIES_MODID = "serverutilities";

    private static final long UPDATE_MS = 1000L;
    private static final String TPS_LINE = "&7MSPT: &a{mspt}  &7TPS: &a{tps}";

    private static String lastPayload;
    private static long lastSendMs;
    private static boolean wasEnabled;
    private static boolean warnedMissing;

    private ServerTabService() {}

    public static boolean available() {
        return Loader.isModLoaded(SERVER_UTILITIES_MODID);
    }

    public static void tick() {
        if (!available()) {
            if (ServerFeatureConfig.tabEnabled && !warnedMissing) {
                warnedMissing = true;
                MyMod.LOG.warn("服务端 tab 显示已启用，但未安装 ServerUtilities；该功能不会生效");
            }
            return;
        }
        if (!ServerFeatureConfig.tabEnabled) {
            if (wasEnabled) {
                // Restore ServerUtilities' own header/footer without our line.
                sendAll(readTabField("headerText"), readTabField("footerText"));
                wasEnabled = false;
                lastPayload = null;
            }
            return;
        }

        wasEnabled = true;
        long now = System.currentTimeMillis();
        if (now - lastSendMs < UPDATE_MS) {
            return;
        }
        String header = readTabField("headerText");
        String footer = appendLine(readTabField("footerText"), ServerMetrics.expand(TPS_LINE));
        String payload = payload(header, footer);
        if (!payload.equals(lastPayload)) {
            sendAllJson(payload);
            lastPayload = payload;
        }
        lastSendMs = now;
    }

    public static void onPlayerLogin(EntityPlayerMP player) {
        if (!available() || !ServerFeatureConfig.tabEnabled || player == null) {
            return;
        }
        String header = readTabField("headerText");
        String footer = appendLine(readTabField("footerText"), ServerMetrics.expand(TPS_LINE));
        sendJson(player, payload(header, footer));
    }

    public static void forceUpdate() {
        lastPayload = null;
        lastSendMs = 0L;
    }

    private static String appendLine(String footer, String line) {
        String base = footer == null ? "" : footer;
        if (base.isEmpty()) {
            return line;
        }
        return base + "\n" + line;
    }

    /**
     * Reads a {@code ServerUtilitiesConfig.Tab} field without a compile-time
     * dependency on ServerUtilities.
     */
    private static String readTabField(String name) {
        try {
            Class<?> config = Class.forName("serverutils.ServerUtilitiesConfig", true,
                    ServerTabService.class.getClassLoader());
            Object tab = config.getField("tab").get(null);
            if (tab == null) {
                return "";
            }
            Object value = tab.getClass().getField(name).get(tab);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String payload(String header, String footer) {
        JsonObject json = new JsonObject();
        json.addProperty("header", header == null ? "" : header);
        json.addProperty("footer", footer == null ? "" : footer);
        return json.toString();
    }

    private static void sendAll(String header, String footer) {
        sendAllJson(payload(header, footer));
    }

    private static void sendAllJson(String json) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            sendJson(player, json);
        }
    }

    private static void sendJson(EntityPlayerMP player, String json) {
        if (player == null || player.playerNetServerHandler == null) {
            return;
        }
        player.playerNetServerHandler.sendPacket(
                new S3FPacketCustomPayload(CHANNEL, json.getBytes(StandardCharsets.UTF_8)));
    }
}
