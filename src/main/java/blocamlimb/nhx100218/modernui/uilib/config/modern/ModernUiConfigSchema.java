package blocamlimb.nhx100218.modernui.uilib.config.modern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import blocamlimb.nhx100218.modernui.config.schema.ConfigSchema;

/** Schema for the uilib/ModernUI configuration screen. */
public final class ModernUiConfigSchema {

    private ModernUiConfigSchema() {
    }

    public static ConfigSchema create() {
        return create(key -> key);
    }

    public static ConfigSchema create(Function<String, String> tr) {
        return blocamlimb.nhx100218.modernui.enhance.Config.appendTo(
                ConfigSchema.builder("modernui"), tr)
                .title(tr.apply("modernui.config.screenTitle"))
                .section("general")
                    .title(tr.apply("modernui.md.tab.general"))
                    .bool("useDebug").defaultValue(Boolean.FALSE)
                        .label(tr.apply("modernui.config.general.useDebug")).helper(tr.apply("modernui.config.general.useDebug.tooltip")).build()
                    .bool("uiDebug").defaultValue(Boolean.FALSE)
                        .label(tr.apply("modernui.config.general.uiDebug")).helper(tr.apply("modernui.config.general.uiDebug.tooltip")).build()
                    .bool("fontRuntimeDebug").defaultValue(Boolean.FALSE)
                        .label(tr.apply("modernui.config.general.fontRuntimeDebug")).helper(tr.apply("modernui.config.general.fontRuntimeDebug.tooltip")).build()
                    .choice("netTransport").options("vanilla", "forge").defaultValue("vanilla")
                        .label(tr.apply("modernui.config.general.netTransport")).helper(tr.apply("modernui.config.general.netTransport.tooltip")).build()
                    .choice("pickerDensity").options("auto", "compact", "standard", "roomy").defaultValue("auto")
                        .label(tr.apply("modernui.config.general.pickerDensity")).helper(tr.apply("modernui.config.general.pickerDensity.tooltip")).build()
                    .choice("configPageTheme").options("flat", "glass").defaultValue("flat")
                        .label(tr.apply("modernui.config.general.configPageTheme")).helper(tr.apply("modernui.config.general.configPageTheme.tooltip")).build()
                    .choice("backdropQuality").options("full", "eco", "solid").defaultValue("full")
                        .label(tr.apply("modernui.config.general.backdropQuality")).helper(tr.apply("modernui.config.general.backdropQuality.tooltip")).build()
                .endSection()
                .section("fontSystem")
                    .title(tr.apply("modernui.md.tab.fontsystem"))
                    .number("lerpMode").defaultValue(Double.valueOf(3.0)).range(0, 3)
                        .label(tr.apply("modernui.config.font.lerpMode")).helper(tr.apply("modernui.config.font.lerpMode.tooltip")).build()
                    .number("aaMode").defaultValue(Double.valueOf(2.0)).range(1, 2)
                        .label(tr.apply("modernui.config.font.aaMode")).helper(tr.apply("modernui.config.font.aaMode.tooltip")).build()
                    .number("brightnessGain").defaultValue(Double.valueOf(2.0))
                        .label(tr.apply("modernui.config.font.brightnessGain")).helper(tr.apply("modernui.config.font.brightnessGain.tooltip")).build()
                    .number("spaceWidth").defaultValue(Double.valueOf(4.0))
                        .label(tr.apply("modernui.config.font.spaceWidth")).helper(tr.apply("modernui.config.font.spaceWidth.tooltip")).build()
                    .number("characterSpacing").defaultValue(Double.valueOf(0.1))
                        .label(tr.apply("modernui.config.font.characterSpacing")).helper(tr.apply("modernui.config.font.characterSpacing.tooltip")).build()
                    .number("shadowOffsetX").defaultValue(Double.valueOf(0.5))
                        .label(tr.apply("modernui.config.font.shadowOffsetX")).helper(tr.apply("modernui.config.font.shadowOffsetX.tooltip")).build()
                    .number("shadowOffsetY").defaultValue(Double.valueOf(0.5))
                        .label(tr.apply("modernui.config.font.shadowOffsetY")).helper(tr.apply("modernui.config.font.shadowOffsetY.tooltip")).build()
                    .number("renderOffset").defaultValue(Double.valueOf(0.0))
                        .label(tr.apply("modernui.config.font.renderOffset")).helper(tr.apply("modernui.config.font.renderOffset.tooltip")).build()
                    .number("smoothRangeMin").defaultValue(Double.valueOf(0.0)).range(0, 1)
                        .label(tr.apply("modernui.config.font.smoothRangeMin")).helper(tr.apply("modernui.config.font.smoothRangeMin.tooltip")).build()
                    .number("smoothRangeMax").defaultValue(Double.valueOf(0.9)).range(0, 1)
                        .label(tr.apply("modernui.config.font.smoothRangeMax")).helper(tr.apply("modernui.config.font.smoothRangeMax.tooltip")).build()
                    .number("drawStageUploadIntervalMs").defaultValue(Double.valueOf(20.0)).range(0, 1000)
                        .label(tr.apply("modernui.config.font.drawStageUploadIntervalMs")).helper(tr.apply("modernui.config.font.drawStageUploadIntervalMs.tooltip")).build()
                    .number("drawStageUploadLimitPerSecond").defaultValue(Double.valueOf(20.0)).range(0, 1000)
                        .label(tr.apply("modernui.config.font.drawStageUploadLimitPerSecond")).helper(tr.apply("modernui.config.font.drawStageUploadLimitPerSecond.tooltip")).build()
                    .number("drawStageUploadBatchSize").defaultValue(Double.valueOf(2.0)).range(0, 256)
                        .label(tr.apply("modernui.config.font.drawStageUploadBatchSize")).helper(tr.apply("modernui.config.font.drawStageUploadBatchSize.tooltip")).build()
                    .number("aaStrength").defaultValue(Double.valueOf(12.0)).range(1, 120)
                        .label(tr.apply("modernui.config.font.aaStrength")).helper(tr.apply("modernui.config.font.aaStrength.tooltip")).build()
                    .bool("replaceOrigin").defaultValue(Boolean.TRUE)
                        .label(tr.apply("modernui.config.font.replaceOrigin")).helper(tr.apply("modernui.config.font.replaceOrigin.tooltip")).build()
                    .bool("customInvCountFont").defaultValue(Boolean.FALSE)
                        .label(tr.apply("modernui.config.font.customInvCountFont")).helper(tr.apply("modernui.config.font.customInvCountFont.tooltip")).build()
                    .simpleList("fontSort").defaultValue(new ArrayList<String>(Arrays.asList("Inter Frozen Medium", "Source Han Sans CN Medium")))
                        .label(tr.apply("modernui.config.font.fontSort")).helper(tr.apply("modernui.config.font.fontSort.tooltip")).build()
                    .simpleList("characterFontRules").defaultValue(new ArrayList<String>())
                        .label(tr.apply("modernui.config.font.characterFontRules")).helper(tr.apply("modernui.config.font.characterFontRules.tooltip")).build()
                    .string("westernNormalFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.westernNormalFont")).helper(tr.apply("modernui.config.font.westernNormalFont.tooltip")).build()
                    .string("westernBoldFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.westernBoldFont")).helper(tr.apply("modernui.config.font.westernBoldFont.tooltip")).build()
                    .string("westernItalicFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.westernItalicFont")).helper(tr.apply("modernui.config.font.westernItalicFont.tooltip")).build()
                    .string("cjkNormalFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.cjkNormalFont")).helper(tr.apply("modernui.config.font.cjkNormalFont.tooltip")).build()
                    .string("cjkBoldFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.cjkBoldFont")).helper(tr.apply("modernui.config.font.cjkBoldFont.tooltip")).build()
                    .string("cjkItalicFont").defaultValue("")
                        .label(tr.apply("modernui.config.font.cjkItalicFont")).helper(tr.apply("modernui.config.font.cjkItalicFont.tooltip")).build()
                .endSection()
                .section("fontSizeSetting")
                    .title(tr.apply("modernui.md.tab.fontsize"))
                    .number("glyphGenerationSize").legacyAliases("awtCharSize").defaultValue(Double.valueOf(48.0)).range(8, 256)
                        .label(tr.apply("modernui.config.font.glyphGenerationSize")).helper(tr.apply("modernui.config.font.glyphGenerationSize.tooltip")).build()
                    .number("gameCharSize").legacyAliases("charSize").defaultValue(Double.valueOf(8.0)).range(1, 72)
                        .label(tr.apply("modernui.config.font.gameCharSize")).helper(tr.apply("modernui.config.font.gameCharSize.tooltip")).build()
                .endSection()
                .build();
    }
}