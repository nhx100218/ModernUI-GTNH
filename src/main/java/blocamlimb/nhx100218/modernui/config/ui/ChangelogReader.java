package blocamlimb.nhx100218.modernui.config.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the latest changelog section from the bundled {@code CHANGELOG.md}.
 *
 * <p>The config dashboard shows the current version's notes. Chinese clients
 * get the {@code ### 中文} section; every other locale gets
 * {@code ### English}.</p>
 */
public final class ChangelogReader {

    private static final String RESOURCE = "/CHANGELOG.md";

    private ChangelogReader() {
    }

    public static boolean isChineseClient() {
        return Locale.getDefault().getLanguage().toLowerCase(Locale.ENGLISH).startsWith("zh");
    }

    public static Changelog latest() {
        return latest(isChineseClient());
    }

    public static Changelog latest(boolean chinese) {
        String text = readResource();
        if (text == null || text.isEmpty()) {
            return fallback(chinese);
        }
        String[] lines = text.split("\\r?\\n");
        int sectionStart = -1;
        int sectionEnd = lines.length;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("## [")) {
                if (sectionStart < 0) {
                    sectionStart = i;
                } else {
                    sectionEnd = i;
                    break;
                }
            }
        }
        if (sectionStart < 0) {
            return fallback(chinese);
        }
        String heading = lines[sectionStart].trim();
        String version = "ModernUI-GTNH";
        int open = heading.indexOf('[');
        int close = heading.indexOf(']');
        if (open >= 0 && close > open) {
            version = heading.substring(open + 1, close);
        }
        String languageHeading = chinese ? "### 中文" : "### English";
        int languageStart = -1;
        for (int i = sectionStart + 1; i < sectionEnd; i++) {
            if (lines[i].trim().equals(languageHeading)) {
                languageStart = i + 1;
                break;
            }
        }
        if (languageStart < 0) {
            // Fall back to the other language when the requested one is absent.
            languageHeading = chinese ? "### English" : "### 中文";
            for (int i = sectionStart + 1; i < sectionEnd; i++) {
                if (lines[i].trim().equals(languageHeading)) {
                    languageStart = i + 1;
                    break;
                }
            }
        }
        List<String> body = new ArrayList<String>();
        if (languageStart >= 0) {
            for (int i = languageStart; i < sectionEnd; i++) {
                String line = clean(lines[i]);
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("### ")) {
                    continue;
                }
                if (line.startsWith("#### ")) {
                    continue;
                }
                body.add(line);
            }
        }
        if (body.isEmpty()) {
            return fallback(chinese);
        }
        String title = chinese
                ? "Modern UI " + version + " 更新内容"
                : "Modern UI " + version + " Changelog";
        return new Changelog(title, body);
    }

    private static String readResource() {
        try (InputStream input = ChangelogReader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                return null;
            }
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            return out.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Removes Markdown decorations that the scene text renderer cannot use. */
    private static String clean(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty()) {
            return "";
        }
        if (line.startsWith("- ")) {
            line = "• " + line.substring(2);
        }
        line = line.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");
        line = line.replace("`", "");
        line = line.replace("**", "");
        return line;
    }

    private static Changelog fallback(boolean chinese) {
        List<String> lines = new ArrayList<String>();
        lines.add(chinese ? "• 暂无内置更新日志。" : "• No bundled changelog was found.");
        return new Changelog(chinese ? "Modern UI 更新内容" : "Modern UI Changelog", lines);
    }

    public static final class Changelog {

        public final String title;
        public final List<String> lines;

        Changelog(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }
    }
}
