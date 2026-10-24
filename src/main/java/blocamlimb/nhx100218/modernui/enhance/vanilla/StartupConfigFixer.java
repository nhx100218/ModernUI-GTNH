package blocamlimb.nhx100218.modernui.enhance.vanilla;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 启动时自动修正若干第三方配置文件（幂等）：
 *
 * <ul>
 *   <li>{@code config/Waila.cfg}：背景渐变改为黑色（{@code gradient1/2=0}），外边框改为深灰
 *       （{@code bgcolor=0x303030}）。</li>
 *   <li>{@code config/chromatictooltipscompat.cfg}：把 {@code wailaEnabled} 关掉。</li>
 *   <li>{@code config/modernui.yaml}：把内置字体排到 {@code fontSystem.fontSort} 最前，
 *       并清掉已不存在的合并字体条目。</li>
 * </ul>
 *
 * <p>由 coremod {@code EarlyMixins.injectData} 在模组加载前调用。</p>
 */
public final class StartupConfigFixer {

    private static final Logger LOG = LogManager.getLogger("ModernUI/StartupConfigFixer");

    private static final int WAILA_BG = 0x000000;
    private static final int WAILA_BORDER = 0x303030;

    // ModernUI 用 Font#getName() 取值，运行时为英文名（而非本地化的“思源黑体”）。
    private static final String[] BUNDLED_FAMILIES = {
            "Inter Frozen Medium",
            "Source Han Sans CN Medium"
    };

    private static final String[] STALE_FONT_ENTRIES = {
            "Inter Frozen + Source Han Sans CN Medium", // 旧合并字体（out.ttf）
            "\u601d\u6e90\u9ed1\u4f53 CN Medium" // 旧本地化名条目
    };

    private StartupConfigFixer() {}

    public static void fix(File configDir) {
        if (configDir == null || !configDir.isDirectory()) {
            return;
        }
        migrateLegacyConfig(configDir);
        extractBundledFonts(configDir.getParentFile());
        fixWaila(new File(configDir, "Waila.cfg"));
        fixChromaticCompat(new File(configDir, "chromatictooltipscompat.cfg"));
        fixFontSort(new File(configDir, "modernui.yaml"));
    }

    /** jar 内资源路径 -> 释放到 {@code <游戏根>/fonts/} 的文件名。 */
    private static final String[][] BUNDLED_FONT_FILES = {
            { "/assets/modernui/fonts/bundled/inter-frozen-medium.otf", "inter-frozen-medium.otf" },
            { "/assets/modernui/fonts/bundled/inter-frozen-medium-italic.otf",
                    "inter-frozen-medium-italic.otf" },
            { "/assets/modernui/fonts/bundled/inter-bold.ttf", "inter-bold.ttf" },
            { "/assets/modernui/fonts/bundled/inter-bold-italic.ttf", "inter-bold-italic.ttf" },
            { "/assets/modernui/fonts/bundled/source-han-sans-cn-medium.otf", "source-han-sans-cn-medium.otf" },
            { "/assets/modernui/fonts/bundled/SourceHanSansSC-Bold.otf", "SourceHanSansSC-Bold.otf" }
    };

    /**
     * 把内置字体释放到 {@code <游戏根>/fonts/}。ModernUI 会自动扫描该目录（与用户自放字体同一机制），
     * 从而无需改动其字体内核即可被发现。已存在且非空则跳过。
     */
    private static void extractBundledFonts(File gameRoot) {
        if (gameRoot == null) {
            return;
        }
        // 规范扫描目录是 <游戏根>/fonts；同时镜像到 config/fonts，便于版本隔离实例统一管理。
        File[] targets = { new File(gameRoot, "fonts"), new File(new File(gameRoot, "config"), "fonts") };
        for (File fontsDir : targets) {
            if (!fontsDir.isDirectory() && !fontsDir.mkdirs()) {
                continue;
            }
            for (String[] entry : BUNDLED_FONT_FILES) {
                File target = new File(fontsDir, entry[1]);
                if (target.isFile() && target.length() > 0) {
                    continue;
                }
                try (InputStream input = StartupConfigFixer.class.getResourceAsStream(entry[0])) {
                    if (input == null) {
                        LOG.warn("内置字体资源缺失: {}", entry[0]);
                        continue;
                    }
                    Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("已释放内置字体: {}", entry[1]);
                } catch (IOException e) {
                    LOG.warn("释放内置字体失败: {}", entry[1], e);
                }
            }
        }
    }


    /** 首次启动把旧配置文件名迁移为现代单一配置名（幂等，不覆盖已有新文件）。 */
    private static void migrateLegacyConfig(File configDir) {
        File modern = new File(configDir, "modernui.yaml");
        File legacy = new File(configDir, "qzuilib-modern.yaml");
        if (modern.isFile()) {
            return;
        }
        try {
            if (legacy.isFile()) {
                Files.copy(legacy.toPath(), modern.toPath());
                LOG.info("已把旧配置 {} 迁移为 {}", legacy.getName(), modern.getName());
            }
        } catch (IOException e) {
            LOG.warn("迁移旧配置失败", e);
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

    private static void fixFontSort(File file) {
        rewriteIfChanged(file, text -> {
            String result = text;
            // 移除失效条目 + 已有内置条目，再按固定顺序重插，保证 Inter(1) 在思源黑体(2) 之前。
            for (String stale : STALE_FONT_ENTRIES) {
                result = removeLine(result, stale);
            }
            for (String family : BUNDLED_FAMILIES) {
                result = removeLine(result, family);
            }
            Matcher matcher = Pattern.compile("(?m)^(\\s*)fontSort:\\s*$").matcher(result);
            if (!matcher.find()) {
                return result;
            }
            String indent = matcher.group(1);
            StringBuilder inserted = new StringBuilder();
            for (String family : BUNDLED_FAMILIES) {
                inserted.append('\n').append(indent).append("- ").append(family);
            }
            int end = matcher.end();
            return result.substring(0, end) + inserted + result.substring(end);
        });
    }

    private static String removeLine(String text, String name) {
        return text.replaceAll("(?m)^\\s*- " + Pattern.quote(name) + "\\s*\\r?\\n", "");
    }

    /** 把形如 {@code key=value} 的整行替换为 {@code key=newValue}，保留缩进；找不到则原样返回。 */
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
                LOG.info("已修正配置文件 {}", file.getName());
            }
        } catch (IOException | RuntimeException failure) {
            LOG.warn("修正配置文件失败: {}", file, failure);
        }
    }
}
