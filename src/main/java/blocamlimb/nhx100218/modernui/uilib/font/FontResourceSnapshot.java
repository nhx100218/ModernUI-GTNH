package blocamlimb.nhx100218.modernui.uilib.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

/** 后台一次性读取并冻结的字体文件、系统字体与平台 hint 快照。 */
final class FontResourceSnapshot {

    static final int MAX_ASSET_FONT_FILES = 256;
    static final long MAX_ASSET_FONT_FILE_BYTES = 128L * 1024L * 1024L;
    static final long MAX_ASSET_FONT_TOTAL_BYTES = 256L * 1024L * 1024L;

    /**
     * Fonts shipped inside the mod jar. They are read directly from the
     * classpath and never extracted to {@code <game>/fonts}; this mirrors
     * ModernUI's {@code assets/modernui/font/...} registration and makes the
     * defaults independent of a writable game directory.
     */
    private static final String[] BUNDLED_FONT_RESOURCES = {
            "/assets/modernui/fonts/bundled/inter-frozen-medium.otf",
            "/assets/modernui/fonts/bundled/inter-frozen-medium-italic.otf",
            "/assets/modernui/fonts/bundled/inter-bold.ttf",
            "/assets/modernui/fonts/bundled/inter-bold-italic.ttf",
            "/assets/modernui/fonts/bundled/source-han-sans-cn-medium.otf",
            "/assets/modernui/fonts/bundled/SourceHanSansSC-Bold.otf",
    };

    private final List<AssetFontResource> assetFonts;
    private final Font[] installedFonts;
    private final String[] defaultOrderHints;
    private final FontResourceFingerprint fingerprint;

    private FontResourceSnapshot(List<AssetFontResource> assetFonts, Font[] installedFonts,
            String[] defaultOrderHints) {
        this.assetFonts = assetFonts;
        this.installedFonts = installedFonts.clone();
        this.defaultOrderHints = defaultOrderHints.clone();
        this.fingerprint = FontResourceFingerprint.create(this);
    }

    static FontResourceSnapshot capture(FontGenerationBuildRequest request) {
        return capture(request, MAX_ASSET_FONT_FILES, MAX_ASSET_FONT_FILE_BYTES,
                MAX_ASSET_FONT_TOTAL_BYTES);
    }

    static FontResourceSnapshot capture(FontGenerationBuildRequest request, int maxAssetFontFiles,
            long maxAssetFontFileBytes, long maxAssetFontTotalBytes) {
        if (request == null) {
            throw new IllegalArgumentException("generation build request 不得为 null");
        }
        if (maxAssetFontFiles < 0 || maxAssetFontFileBytes < 0L || maxAssetFontTotalBytes < 0L) {
            throw new IllegalArgumentException("asset font snapshot 上限不得为负");
        }
        assertNotInterrupted();
        List<AssetFontResource> assetFonts = captureAssetFonts(request, maxAssetFontFiles,
                maxAssetFontFileBytes, maxAssetFontTotalBytes);
        assertNotInterrupted();
        Font[] installedFonts;
        try {
            installedFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        } catch (RuntimeException | Error failure) {
            // 系统没有可用字体时（issue #71：Alpine 等精简镜像缺 fontconfig 或字体包），AWT 在构造
            // 字体管理器的这一步就抛原生异常。这里只补一句可定位的上下文并原样上抛：是否降级由调用方
            // （FontService.ensureLayoutRuntimeReady）按契约决定，快照层不做策略。
            throw new IllegalStateException("无法枚举系统字体：AWT 字体子系统初始化失败（系统未安装字体，"
                    + "或缺少 fontconfig）", failure);
        }
        assertNotInterrupted();
        if (installedFonts != null) {
            Arrays.sort(installedFonts, new Comparator<Font>() {
                @Override
                public int compare(Font left, Font right) {
                    return fontDescriptorKey(left).compareTo(fontDescriptorKey(right));
                }
            });
        }
        return new FontResourceSnapshot(assetFonts, installedFonts == null ? new Font[0] : installedFonts,
                request.getDefaultOrderHints());
    }

    List<AssetFontResource> getAssetFonts() {
        return assetFonts;
    }

    Font[] getInstalledFonts() {
        return installedFonts.clone();
    }

    String[] getDefaultOrderHints() {
        return defaultOrderHints.clone();
    }

    FontResourceFingerprint getFingerprint() {
        return fingerprint;
    }

    private static List<AssetFontResource> captureAssetFonts(FontGenerationBuildRequest request,
            int maxAssetFontFiles, long maxAssetFontFileBytes, long maxAssetFontTotalBytes) {
        prepareFontDirectory(request);
        List<AssetFontResource> resources = new ArrayList<AssetFontResource>();
        long capturedBytes = 0L;

        if (request.shouldIncludeBundledFonts()) {
            for (String path : BUNDLED_FONT_RESOURCES) {
                assertNotInterrupted();
                if (resources.size() >= maxAssetFontFiles) {
                    throw new IllegalStateException("字体资源文件数量超过上限: " + maxAssetFontFiles);
                }
                long remainingBytes = maxAssetFontTotalBytes - capturedBytes;
                byte[] content = readBundledFont(path, Math.min(maxAssetFontFileBytes, remainingBytes));
                if (content == null) {
                    continue;
                }
                capturedBytes += content.length;
                resources.add(new AssetFontResource(path, content, null));
            }
        }

        List<File> files = listAssetFontFiles(request.getFontDirectory(), maxAssetFontFiles);
        appendConfiguredFontFiles(request, files);
        sortAssetFontFiles(files);
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (File file : files) {
            assertNotInterrupted();
            String path = file.getAbsolutePath();
            if (!seen.add(path)) {
                continue;
            }
            if (resources.size() >= maxAssetFontFiles) {
                throw new IllegalStateException("字体资源文件数量超过上限: " + maxAssetFontFiles);
            }
            try {
                long remainingBytes = maxAssetFontTotalBytes - capturedBytes;
                byte[] content = readStableBytes(file, Math.min(maxAssetFontFileBytes, remainingBytes));
                capturedBytes += content.length;
                resources.add(new AssetFontResource(file.getName(), content, path));
            } catch (IOException exception) {
                throw new IllegalStateException("无法冻结字体资源: " + file.getName(), exception);
            }
        }
        return java.util.Collections.unmodifiableList(resources);
    }

    /**
     * Reads one bundled font from the classpath.
     *
     * @param path         absolute resource path
     * @param maxBytes     per-file byte limit
     * @return bytes, or null when the resource is absent
     */
    private static byte[] readBundledFont(String path, long maxBytes) {
        if (maxBytes < 0L) {
            throw new IllegalStateException("字体资源超过 snapshot bytes 上限");
        }
        try (InputStream input = FontResourceSnapshot.class.getResourceAsStream(path)) {
            if (input == null) {
                return null;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                assertNotInterrupted();
                if (read == 0) {
                    continue;
                }
                total += read;
                if ((long) total > maxBytes) {
                    throw new IOException("字体资源超过 snapshot bytes 上限");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("无法冻结内置字体: " + path, exception);
        }
    }

    /**
     * Adds explicitly configured font files/directories.
     *
     * <p>The player may put a family name or a file path in the font stack.
     * Family names are resolved later against system/bundled fonts; path values
     * are read here so a custom file is available even outside
     * {@code <game>/fonts}.</p>
     */
    private static void appendConfiguredFontFiles(FontGenerationBuildRequest request, List<File> files) {
        String[] configured = request.getSettings().getFontSort();
        if (configured.length == 0) {
            return;
        }
        File gameRoot = request.getFontDirectory().getParentFile();
        for (String value : configured) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            String normalized = value.trim().replace('\\', '/');
            if (!isFontFile(normalized) && !new File(normalized).isDirectory()) {
                // Not a path; it is a family name resolved by FontOrderPlanner.
                continue;
            }
            File candidate = new File(normalized);
            if (!candidate.isAbsolute() && gameRoot != null) {
                candidate = new File(gameRoot, normalized);
            }
            if (candidate.isDirectory()) {
                File[] entries = candidate.listFiles((dir, name) -> isFontFile(name));
                if (entries != null) {
                    java.util.Collections.addAll(files, entries);
                }
            } else if (candidate.isFile()) {
                files.add(candidate);
            }
        }
    }

    static void sortAssetFontFiles(List<File> files) {
        java.util.Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                int normalized = left.getName().toLowerCase(Locale.ENGLISH)
                        .compareTo(right.getName().toLowerCase(Locale.ENGLISH));
                return normalized != 0 ? normalized : left.getName().compareTo(right.getName());
            }
        });
    }

    private static void prepareFontDirectory(FontGenerationBuildRequest request) {
        if (!request.shouldCreateFontDirectoryIfMissing()) {
            return;
        }
        File fontDirectory = request.getFontDirectory();
        try {
            Files.createDirectories(fontDirectory.toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建字体资源目录: " + fontDirectory.getAbsolutePath(),
                    exception);
        }
    }

    private static List<File> listAssetFontFiles(File fontDirectory, int maxAssetFontFiles) {
        List<File> files = new ArrayList<File>();
        Path directory = fontDirectory.toPath();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                assertNotInterrupted();
                File file = entry.toFile();
                if (!isFontFile(file.getName())) {
                    continue;
                }
                if (files.size() >= maxAssetFontFiles) {
                    throw new IllegalStateException("字体资源文件数量超过上限: " + maxAssetFontFiles);
                }
                files.add(file);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法枚举字体资源目录: " + fontDirectory.getAbsolutePath(), exception);
        }
        return files;
    }

    private static byte[] readStableBytes(File file, long maxBytes) throws IOException {
        return readStableBytes(file, maxBytes, null);
    }

    static byte[] readStableBytes(File file, long maxBytes, Runnable afterFirstRead) throws IOException {
        if (file == null || maxBytes < 0L) {
            throw new IllegalArgumentException("字体资源与 bytes 上限无效");
        }
        Path path = file.toPath();
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class);
        if (!before.isRegularFile()) {
            throw new IOException("字体资源不是普通文件");
        }
        if (before.size() > maxBytes || before.size() > Integer.MAX_VALUE) {
            throw new IOException("字体资源超过 snapshot bytes 上限");
        }
        byte[] content = readBytes(file, before.size());
        if (afterFirstRead != null) {
            afterFirstRead.run();
        }
        assertStableFile(before, Files.readAttributes(path, BasicFileAttributes.class), content.length);
        verifyContent(file, content);
        assertStableFile(before, Files.readAttributes(path, BasicFileAttributes.class), content.length);
        return content;
    }

    private static byte[] readBytes(File file, long expectedLength) throws IOException {
        byte[] content = new byte[(int) expectedLength];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < content.length) {
                assertNotInterrupted();
                int read = input.read(content, offset, content.length - offset);
                if (read < 0) {
                    throw new IOException("字体资源在 snapshot 期间截断");
                }
                offset += read;
            }
            if (input.read() >= 0) {
                throw new IOException("字体资源在 snapshot 期间增长");
            }
            return content;
        }
    }

    private static void verifyContent(File file, byte[] expected) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int offset = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                assertNotInterrupted();
                if (read <= 0) {
                    continue;
                }
                if (offset > expected.length - read) {
                    throw new IOException("字体资源在 snapshot 期间增长");
                }
                for (int index = 0; index < read; index++) {
                    if (buffer[index] != expected[offset + index]) {
                        throw new IOException("字体资源在 snapshot 期间发生变化");
                    }
                }
                offset += read;
            }
            if (offset != expected.length) {
                throw new IOException("字体资源在 snapshot 期间截断");
            }
        }
    }

    private static void assertStableFile(BasicFileAttributes before, BasicFileAttributes after,
            int contentLength) throws IOException {
        Object beforeKey = before.fileKey();
        Object afterKey = after.fileKey();
        boolean sameKey = beforeKey == null ? afterKey == null : beforeKey.equals(afterKey);
        if (!sameKey || before.size() != after.size() || before.size() != contentLength
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            throw new IOException("字体资源在 snapshot 期间发生变化");
        }
    }

    private static boolean isFontFile(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
        return normalized.endsWith(".ttf") || normalized.endsWith(".otf") || normalized.endsWith(".ttc");
    }

    private static String fontDescriptorKey(Font font) {
        StringBuilder builder = new StringBuilder();
        appendDescriptor(builder, font.getName());
        appendDescriptor(builder, font.getFamily(Locale.ENGLISH));
        appendDescriptor(builder, font.getFontName(Locale.ENGLISH));
        appendDescriptor(builder, font.getPSName());
        builder.append(font.getStyle()).append('|');
        builder.append(Float.floatToIntBits(font.getSize2D())).append('|');
        builder.append(font.getNumGlyphs()).append('|');
        builder.append(font.getMissingGlyphCode()).append('|');
        double[] matrix = new double[6];
        font.getTransform().getMatrix(matrix);
        for (double value : matrix) {
            builder.append(Long.toHexString(Double.doubleToLongBits(value))).append('|');
        }
        return builder.toString();
    }

    private static void appendDescriptor(StringBuilder builder, String value) {
        String resolved = value == null ? "" : value;
        builder.append(resolved.length()).append(':').append(resolved).append('|');
    }

    static void assertNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("generation candidate 已取消");
        }
    }

    /** 单个自定义字体文件的相对名称与 immutable bytes。 */
    static final class AssetFontResource {

        private final String name;
        private final byte[] content;
        private final String sourcePath;

        private AssetFontResource(String name, byte[] content, String sourcePath) {
            this.name = name;
            this.content = content;
            this.sourcePath = sourcePath;
        }

        String getName() {
            return name;
        }

        /**
         * Absolute path of a user-provided file, or null for bundled resources.
         *
         * @return source file path, or null
         */
        String getSourcePath() {
            return sourcePath;
        }

        int getContentLength() {
            return content.length;
        }

        void updateDigest(MessageDigest digest) {
            digest.update(content);
        }

        ByteArrayInputStream openContentStream() {
            return new ByteArrayInputStream(content);
        }
    }
}
