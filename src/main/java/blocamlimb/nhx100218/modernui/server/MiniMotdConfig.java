package blocamlimb.nhx100218.modernui.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import blocamlimb.nhx100218.modernui.uilib.MyMod;

/**
 * Reads a real MiniMOTD {@code config/MiniMOTD/main.conf} so existing MiniMOTD
 * configs and {@code icons/} can be dropped in unchanged.
 *
 * <p>Typesafe Config (bundled with Forge) is used reflectively; if parsing
 * fails we fall back to the {@code server.motd} section of modernui.yaml.</p>
 */
public final class MiniMotdConfig {

    public static final String DIRECTORY = "MiniMOTD";
    public static final String MAIN_FILE = "main.conf";

    private static File directory;
    private static File mainFile;
    private static long lastModified = -1L;
    private static Data cached;
    private static boolean warnedParseFailure;

    private MiniMotdConfig() {}

    public static synchronized void initialize(File configDir) {
        if (configDir == null) {
            return;
        }
        directory = new File(configDir, DIRECTORY);
        mainFile = new File(directory, MAIN_FILE);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            MyMod.LOG.warn("无法创建 MiniMOTD 配置目录: {}", directory);
        }
        File icons = new File(directory, "icons");
        if (!icons.isDirectory()) {
            icons.mkdirs();
        }
        if (!mainFile.isFile()) {
            writeDefault(mainFile);
        }
        invalidate();
    }

    public static boolean isPresent() {
        return mainFile != null && mainFile.isFile();
    }

    public static File directory() {
        return directory;
    }

    public static File iconsDirectory() {
        return directory == null ? null : new File(directory, "icons");
    }

    public static synchronized void invalidate() {
        cached = null;
        lastModified = -1L;
    }

    public static synchronized Data current() {
        if (!isPresent()) {
            return null;
        }
        long stamp = mainFile.lastModified();
        if (cached != null && stamp == lastModified) {
            return cached;
        }
        cached = parse(mainFile);
        lastModified = stamp;
        return cached;
    }

    private static void writeDefault(File file) {
        String text = "# MiniMOTD-compatible config. Drop an existing MiniMOTD main.conf here.\n"
                + "motd-enabled = false\n"                + "icon-enabled = true\n"
                + "motds = [\n"
                + "  {\n"
                + "    line1 = \"&bModernUI-GTNH\"\n"
                + "    line2 = \"&7TPS: &a{tps}  &7Players: &a{players}/{maxplayers}\"\n"
                + "    icon = \"random\"\n"
                + "  }\n"
                + "]\n";
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            MyMod.LOG.info("已生成 MiniMOTD 兼容配置: {}", file.getAbsolutePath());
        } catch (IOException e) {
            MyMod.LOG.warn("写入 MiniMOTD 默认配置失败: {}", file, e);
        }
    }

    private static Data parse(File file) {
        try {
            ClassLoader loader = MiniMotdConfig.class.getClassLoader();
            Class<?> factory = Class.forName("com.typesafe.config.ConfigFactory", true, loader);
            Object parsed = factory.getMethod("parseFile", File.class).invoke(null, file);
            Class<?> configType = Class.forName("com.typesafe.config.Config", true, loader);
            Object config = configType.getMethod("resolve").invoke(parsed);

            boolean enabled = ((Boolean) configType.getMethod("getBoolean", String.class)
                    .invoke(config, "motd-enabled")).booleanValue();
            boolean iconsEnabled = true;
            try {
                iconsEnabled = ((Boolean) configType.getMethod("getBoolean", String.class)
                        .invoke(config, "icon-enabled")).booleanValue();
            } catch (Throwable ignored) {
                // Optional key.
            }

            List<Entry> entries = new ArrayList<Entry>();
            Object list = configType.getMethod("getConfigList", String.class).invoke(config, "motds");
            if (list instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) list) {
                    Object line1 = configType.getMethod("getString", String.class).invoke(item, "line1");
                    Object line2 = configType.getMethod("getString", String.class).invoke(item, "line2");
                    String icon = "random";
                    try {
                        icon = String.valueOf(configType.getMethod("getString", String.class)
                                .invoke(item, "icon"));
                    } catch (Throwable ignored) {
                        // Optional key.
                    }
                    entries.add(new Entry(String.valueOf(line1), String.valueOf(line2), icon));
                }
            }
            if (entries.isEmpty()) {
                return null;
            }
            warnedParseFailure = false;
            return new Data(enabled, iconsEnabled, entries);
        } catch (Throwable t) {
            if (!warnedParseFailure) {
                warnedParseFailure = true;
                MyMod.LOG.warn("解析 MiniMOTD 配置失败，回退到 modernui.yaml: {}", file, t);
            }
            return null;
        }
    }

    public static final class Data {

        public final boolean enabled;
        public final boolean iconsEnabled;
        public final List<Entry> motds;

        Data(boolean enabled, boolean iconsEnabled, List<Entry> motds) {
            this.enabled = enabled;
            this.iconsEnabled = iconsEnabled;
            this.motds = Collections.unmodifiableList(motds);
        }
    }

    public static final class Entry {

        public final String line1;
        public final String line2;
        public final String icon;

        Entry(String line1, String line2, String icon) {
            this.line1 = line1 == null ? "" : line1;
            this.line2 = line2 == null ? "" : line2;
            this.icon = icon == null ? "random" : icon;
        }
    }
}
