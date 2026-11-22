package blocamlimb.nhx100218.modernui.enhance.vanilla;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fixes a few third-party config files at startup (idempotent).
 *
 * <ul>
 *   <li>{@code config/Waila.cfg}: black background gradient and a dark border.</li>
 *   <li>{@code config/chromatictooltipscompat.cfg}: disables Waila compat.</li>
 * </ul>
 *
 * <p>Bundled fonts are no longer extracted to {@code <game>/fonts}; the font
 * engine reads them directly from the mod jar like ModernUI does.</p>
 *
 * <p>Called by the coremod {@code EarlyMixins.injectData} before mods load.</p>
 */
public final class StartupConfigFixer {

    private static final Logger LOG = LogManager.getLogger("ModernUI/StartupConfigFixer");

    private static final int WAILA_BG = 0x000000;
    private static final int WAILA_BORDER = 0x303030;

    private StartupConfigFixer() {}

    public static void fix(File configDir) {
        if (configDir == null || !configDir.isDirectory()) {
            return;
        }
        migrateLegacyConfig(configDir);
        fixWaila(new File(configDir, "Waila.cfg"));
        fixChromaticCompat(new File(configDir, "chromatictooltipscompat.cfg"));
    }

    /** First launch renames the old config file, without overwriting the new one. */
    private static void migrateLegacyConfig(File configDir) {
        File modern = new File(configDir, "modernui.yaml");
        File legacy = new File(configDir, "qzuilib-modern.yaml");
        if (modern.isFile()) {
            return;
        }
        try {
            if (legacy.isFile()) {
                Files.copy(legacy.toPath(), modern.toPath());
                LOG.info("Migrated old config {} to {}", legacy.getName(), modern.getName());
            }
        } catch (IOException e) {
            LOG.warn("Failed to migrate old config", e);
        }
    }

    private static void fixWaila(File file) {
        rewriteIfChanged(file, text -> {
            String result = setValue(text, "I:waila.cfg.bgcolor", Integer.toString(WAILA_BORDER));
            result = setValue(result, "I:waila.cfg.gradient1", Integer.toString(WAILA_BG));
            result = setValue(result, "I:waila.cfg.gradient2", Integer.toString(WAILA_BG));
            return result;
        });
    }

    private static void fixChromaticCompat(File file) {
        rewriteIfChanged(file, text -> setValue(text, "B:wailaEnabled", "false"));
    }

    /** Replaces a {@code key=value} line, preserving indentation. */
    private static String setValue(String text, String key, String value) {
        Matcher matcher = Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "=).*$").matcher(text);
        StringBuffer buffer = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + value));
        }
        if (!found) {
            return text;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private interface TextTransform {
        String apply(String text);
    }

    private static void rewriteIfChanged(File file, TextTransform transform) {
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            String original = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String updated = transform.apply(original);
            if (!updated.equals(original)) {
                Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
                LOG.info("Fixed config {}", file.getName());
            }
        } catch (IOException | RuntimeException failure) {
            LOG.warn("Failed to fix config: {}", file, failure);
        }
    }
}
