package blocamlimb.nhx100218.modernui.uilib.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import blocamlimb.nhx100218.modernui.uilib.MyMod;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontCatalog;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontFamilyNames;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontOrderPlanner;
import blocamlimb.nhx100218.modernui.uilib.font.util.FontOrderSnapshot;

/** FontService 内部的 snapshot-only catalog prepare/publish owner。 */
final class FontGenerationRegistry {

    private final FontCatalog fontCatalog;
    private final FontOrderPlanner fontOrderPlanner = new FontOrderPlanner();

    FontGenerationRegistry(FontCatalog fontCatalog) {
        if (fontCatalog == null) {
            throw new IllegalArgumentException("fontCatalog 不得为 null");
        }
        this.fontCatalog = fontCatalog;
    }

    PreparedCatalog prepare(FontRuntimeSettings settings, FontResourceSnapshot resources) {
        if (settings == null || resources == null) {
            throw new IllegalArgumentException("generation registry 输入不得为 null");
        }
        List<Font> fonts = new ArrayList<Font>();
        LoadedAssetFonts assetFonts = loadAssetFonts(settings, resources);
        fonts.addAll(assetFonts.fonts);
        fonts.addAll(loadInstalledFonts(settings, resources));
        FontResourceSnapshot.assertNotInterrupted();
        String[] orderHints = settings.isFontSortConfigured()
                ? resolveConfiguredPaths(settings.getFontSort(), assetFonts.familyByPath)
                : resources.getDefaultOrderHints();
        FontOrderSnapshot orderSnapshot = fontOrderPlanner.plan(fonts, orderHints,
                settings.isFontSortConfigured());
        return new PreparedCatalog(fontCatalog.prepareSnapshot(orderSnapshot.getOrderedFonts()), orderSnapshot);
    }

    void validate(PreparedCatalog preparedCatalog) {
        if (preparedCatalog == null) {
            throw new IllegalArgumentException("preparedCatalog 不得为 null");
        }
        fontCatalog.validate(preparedCatalog.getCatalogSnapshot());
        if (preparedCatalog.getCatalogSnapshot().getFonts().isEmpty()) {
            throw new IllegalStateException("字体 catalog candidate 不得为空");
        }
    }

    void publish(PreparedCatalog preparedCatalog) {
        fontCatalog.publish(preparedCatalog.getCatalogSnapshot());
    }

    void completePublication(PreparedCatalog preparedCatalog) {
        FontConfig.applyFontOrderSnapshot(preparedCatalog.getOrderSnapshot());
        MyMod.LOG.info("字体注册完成，可用字体数量：{}，缺失字体数量：{}",
                Integer.valueOf(fontCatalog.getFonts().size()),
                Integer.valueOf(FontConfig.getMissingFontSnapshot().length));
    }

    private LoadedAssetFonts loadAssetFonts(FontRuntimeSettings settings, FontResourceSnapshot resources) {
        List<Font> fonts = new ArrayList<Font>();
        Map<String, String> familyByPath = new LinkedHashMap<String, String>();
        float derivedSize = (float) settings.getGlyphGenerationSize();
        for (FontResourceSnapshot.AssetFontResource resource : resources.getAssetFonts()) {
            FontResourceSnapshot.assertNotInterrupted();
            try (ByteArrayInputStream input = resource.openContentStream()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(derivedSize);
                fonts.add(font);
                String sourcePath = resource.getSourcePath();
                if (sourcePath != null) {
                    familyByPath.put(normalizePath(sourcePath), FontFamilyNames.familyName(font));
                }
            } catch (FontFormatException exception) {
                MyMod.LOG.error("字体格式无效：{}", resource.getName(), exception);
            } catch (IOException exception) {
                MyMod.LOG.error("读取字体失败：{}", resource.getName(), exception);
            }
        }
        return new LoadedAssetFonts(fonts, familyByPath);
    }

    /**
     * Replaces configured file paths with the family names of the fonts that
     * were actually loaded from those paths, so a path can be the primary
     * family in the stack while the engine still matches by family name.
     */
    private static String[] resolveConfiguredPaths(String[] configured, Map<String, String> familyByPath) {
        if (configured == null || configured.length == 0 || familyByPath.isEmpty()) {
            return configured == null ? new String[0] : configured;
        }
        String[] resolved = new String[configured.length];
        for (int i = 0; i < configured.length; i++) {
            String value = configured[i];
            String family = value == null ? null : familyByPath.get(normalizePath(value));
            resolved[i] = family == null || family.isEmpty() ? value : family;
        }
        return resolved;
    }

    private static String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('\\', '/').toLowerCase(Locale.ENGLISH);
    }

    private List<Font> loadInstalledFonts(FontRuntimeSettings settings, FontResourceSnapshot resources) {
        List<Font> fonts = new ArrayList<Font>();
        float derivedSize = (float) settings.getGlyphGenerationSize();
        for (Font font : resources.getInstalledFonts()) {
            FontResourceSnapshot.assertNotInterrupted();
            fonts.add(font.deriveFont(derivedSize));
        }
        return fonts;
    }

    /** 已完成发现、排序和不可变复制的内部 catalog candidate。 */
    private static final class LoadedAssetFonts {

        private final List<Font> fonts;
        private final Map<String, String> familyByPath;

        private LoadedAssetFonts(List<Font> fonts, Map<String, String> familyByPath) {
            this.fonts = fonts;
            this.familyByPath = familyByPath;
        }
    }

    static final class PreparedCatalog {

        private final FontCatalog.Snapshot catalogSnapshot;
        private final FontOrderSnapshot orderSnapshot;

        private PreparedCatalog(FontCatalog.Snapshot catalogSnapshot, FontOrderSnapshot orderSnapshot) {
            this.catalogSnapshot = catalogSnapshot;
            this.orderSnapshot = orderSnapshot;
        }

        FontCatalog.Snapshot getCatalogSnapshot() {
            return catalogSnapshot;
        }

        FontOrderSnapshot getOrderSnapshot() {
            return orderSnapshot;
        }
    }
}
