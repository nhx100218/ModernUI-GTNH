package blocamlimb.nhx100218.modernui.server;

import java.util.Locale;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Samples server TPS and MSPT without any platform dependency.
 *
 * <p>TPS uses the same 1-minute, time-weighted rolling window as TabTPS:
 * a sample is taken every 20 ticks from the delta between tick starts. MSPT
 * reuses vanilla's 100-tick {@code tickTimeArray}.</p>
 */
public final class ServerMetrics {

    private static final int TPS_SAMPLE_TICKS = 20;
    private static final int TPS_WINDOW = 60;

    public static final ServerMetrics INSTANCE = new ServerMetrics();

    private final double[] samples = new double[TPS_WINDOW];
    private final long[] weights = new long[TPS_WINDOW];
    private int index;
    private long totalWeight;
    private double total;
    private long lastSampleNanos;
    private boolean haveLastSample;
    private int tickCounter;

    private ServerMetrics() {}

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long now = System.nanoTime();
        if (tickCounter % TPS_SAMPLE_TICKS == 0) {
            if (haveLastSample) {
                long delta = now - lastSampleNanos;
                if (delta > 0L) {
                    addSample(Math.min(20.0D, 20_000_000_000.0D / (double) delta), delta);
                }
            }
            lastSampleNanos = now;
            haveLastSample = true;
        }
        tickCounter++;
    }

    private void addSample(double tps, long weight) {
        total -= samples[index] * (double) weights[index];
        totalWeight -= weights[index];
        samples[index] = tps;
        weights[index] = weight;
        total += tps * (double) weight;
        totalWeight += weight;
        if (++index >= TPS_WINDOW) {
            index = 0;
        }
    }

    public static double tps() {
        ServerMetrics metrics = INSTANCE;
        if (metrics.totalWeight <= 0L) {
            return 20.0D;
        }
        return metrics.total / (double) metrics.totalWeight;
    }

    public static double mspt() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.tickTimeArray == null || server.tickTimeArray.length == 0) {
            return 0.0D;
        }
        long total = 0L;
        for (long value : server.tickTimeArray) {
            total += value;
        }
        return (double) total / (double) server.tickTimeArray.length * 1.0E-6D;
    }

    public static String formatTps() {
        return String.format(Locale.ROOT, "%.2f", Double.valueOf(tps()));
    }

    public static String formatMspt() {
        return String.format(Locale.ROOT, "%.2f", Double.valueOf(mspt()));
    }

    /** Expands the placeholders supported in tab/MOTD templates. */
    public static String expand(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        MinecraftServer server = MinecraftServer.getServer();
        int online = server == null ? 0 : server.getCurrentPlayerCount();
        int max = server == null ? 0 : server.getMaxPlayers();
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        return template.replace("{tps}", formatTps())
                .replace("{mspt}", formatMspt())
                .replace("{players}", Integer.toString(online))
                .replace("{maxplayers}", Integer.toString(max))
                .replace("{memory}", usedMb + "/" + maxMb)
                .replace("{cpu}", cpu())
                .replace("{uptime}", Long.toString(server == null ? 0L : server.getSystemTimeMillis() / 1000L));
    }

    private static String cpu() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            Object load;
            try {
                load = bean.getClass().getMethod("getCpuLoad").invoke(bean);
            } catch (NoSuchMethodException older) {
                load = bean.getClass().getMethod("getSystemCpuLoad").invoke(bean);
            }
            if (load instanceof Number) {
                double value = ((Number) load).doubleValue();
                if (value >= 0.0D) {
                    return String.format(Locale.ROOT, "%.0f%%", Double.valueOf(value * 100.0D));
                }
            }
            double average = bean.getSystemLoadAverage();
            if (average >= 0.0D) {
                return String.format(Locale.ROOT, "%.1f", Double.valueOf(average));
            }
        } catch (Throwable ignored) {
            // Fall through to the unavailable marker.
        }
        return "n/a";
    }
}
