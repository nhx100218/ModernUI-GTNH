package blocamlimb.nhx100218.modernui.server;

import java.io.File;

import blocamlimb.nhx100218.modernui.config.runtime.Authority;
import blocamlimb.nhx100218.modernui.config.runtime.ConfigManager;
import blocamlimb.nhx100218.modernui.config.runtime.DraftBuffer;
import blocamlimb.nhx100218.modernui.config.runtime.SaveOutcome;
import blocamlimb.nhx100218.modernui.uilib.MyMod;
import blocamlimb.nhx100218.modernui.uilib.config.modern.ModernUiConfigSchema;

/**
 * Server-only settings stored in {@code config/modernui.yaml}.
 *
 * <p>The values are copied out of the config authority at startup, so runtime
 * consumers never touch the config stack. {@link #persist(String, Object)} is
 * used by {@code /mug} to write live changes back to the same YAML file.</p>
 */
public final class ServerFeatureConfig {

    public static final String TAB_ENABLED = "server.tab.enabled";

    public static boolean tabEnabled;

    private static volatile File configFile;

    private ServerFeatureConfig() {}

    public static void apply(Authority authority) {
        if (authority == null) {
            return;
        }
        tabEnabled = authority.getBool(TAB_ENABLED);
    }

    public static void attach(File file) {
        configFile = file;
    }

    public static File configFile() {
        return configFile;
    }

    /**
     * Writes one field and re-applies the authority. Must be called on the
     * server thread.
     */
    public static boolean persist(String path, Object value) {
        File file = configFile;
        if (file == null) {
            MyMod.LOG.warn("无法保存服务端配置：配置文件路径未初始化");
            return false;
        }
        try {
            ConfigManager manager = ConfigManager.bootstrap(file, ModernUiConfigSchema.create());
            DraftBuffer draft = manager.openDraft();
            draft.setDraft(path, value);
            SaveOutcome outcome = manager.save(draft);
            if (!outcome.isSuccess()) {
                MyMod.LOG.warn("保存服务端配置失败 {} = {}: {}", path, value, outcome.errorMessage());
                return false;
            }
            apply(manager.authority());
            return true;
        } catch (Throwable t) {
            MyMod.LOG.warn("保存服务端配置失败 {} = {}", path, value, t);
            return false;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
