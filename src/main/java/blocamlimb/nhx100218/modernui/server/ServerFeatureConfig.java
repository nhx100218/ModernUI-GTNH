package blocamlimb.nhx100218.modernui.server;

import java.io.File;

import blocamlimb.nhx100218.modernui.config.runtime.Authority;

/**
 * Runtime state for optional server integrations.
 *
 * <p>Server switches are no longer stored in {@code config/modernui.yaml}: the
 * MOTD is owned entirely by {@code config/MiniMOTD/main.conf}, and the
 * ServerUtilities tab is controlled at runtime by {@code /mug tab on|off}.
 * That keeps the client config page free of server-only keys.</p>
 */
public final class ServerFeatureConfig {

    /** Whether the ServerUtilities tab integration may render. */
    public static volatile boolean tabEnabled;

    private static volatile File configFile;

    private ServerFeatureConfig() {}

    /**
     * Kept for the reload pipeline. Server toggles are runtime state now, so
     * reloading modernui.yaml must not flip them back.
     */
    public static void apply(Authority authority) {
        // no-op: no server keys live in the shared config file
    }

    public static void attach(File file) {
        configFile = file;
    }

    public static File configFile() {
        return configFile;
    }
}
