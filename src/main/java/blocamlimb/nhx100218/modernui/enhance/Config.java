package blocamlimb.nhx100218.modernui.enhance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.ScreenTransition;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;

import blocamlimb.nhx100218.modernui.config.runtime.Authority;
import blocamlimb.nhx100218.modernui.config.schema.ConfigSchema;

/** 附属增强设置段：统一并入 ModernUI-GTNH 的单一配置文件与单一配置页（不单独落盘）。 */
public final class Config {

    private Config() {}

    // ---------- Schema ----------

    /** 把附属增强的设置段追加到外部 schema builder（不新建 title，便于与库配置合并为单一 yaml）。 */
    public static ConfigSchema.Builder appendTo(ConfigSchema.Builder builder, Function<String, String> tr) {
        return builder
                .section("tooltip")
                    .title(tr.apply("modernui.config.cat.tooltip"))
                    .bool("enabled").defaultValue(TooltipConfig.enabled).label(l(tr, "tooltip", "enabled")).helper(h(tr, "tooltip", "enabled")).build()
                    .bool("rounded").defaultValue(TooltipConfig.rounded).label(l(tr, "tooltip", "rounded")).helper(h(tr, "tooltip", "rounded")).build()
                    .bool("centerTitle").defaultValue(TooltipConfig.centerTitle).label(l(tr, "tooltip", "centerTitle")).helper(h(tr, "tooltip", "centerTitle")).build()
                    .bool("titleBreak").defaultValue(TooltipConfig.titleBreak).label(l(tr, "tooltip", "titleBreak")).helper(h(tr, "tooltip", "titleBreak")).build()
                    .bool("adaptiveColors").defaultValue(TooltipConfig.adaptiveColors).label(l(tr, "tooltip", "adaptiveColors")).helper(h(tr, "tooltip", "adaptiveColors")).build()
                    .integer("borderColorCycle").defaultValue(Long.valueOf(TooltipConfig.borderColorCycle)).range(0, 10000).slider().label(l(tr, "tooltip", "borderColorCycle")).helper(h(tr, "tooltip", "borderColorCycle")).build()
                    .number("borderWidth").defaultValue(Double.valueOf(TooltipConfig.borderWidth)).range(0, 8).slider().label(l(tr, "tooltip", "borderWidth")).helper(h(tr, "tooltip", "borderWidth")).build()
                    .number("cornerRadius").defaultValue(Double.valueOf(TooltipConfig.cornerRadius)).range(0, 16).slider().label(l(tr, "tooltip", "cornerRadius")).helper(h(tr, "tooltip", "cornerRadius")).build()
                    .integer("modularUiMaxWidth").defaultValue(Long.valueOf(TooltipConfig.modularUiMaxWidth)).range(0, 2000).slider().label(l(tr, "tooltip", "modularUiMaxWidth")).helper(h(tr, "tooltip", "modularUiMaxWidth")).build()
                    .bool("modularUiNoWrap").defaultValue(TooltipConfig.modularUiNoWrap).label(l(tr, "tooltip", "modularUiNoWrap")).helper(h(tr, "tooltip", "modularUiNoWrap")).build()
                    .simpleList("fillColor").defaultValue(hexList(TooltipConfig.fillColor)).label(l(tr, "tooltip", "fillColor")).helper(h(tr, "tooltip", "fillColor")).build()
                    .simpleList("strokeColor").defaultValue(hexList(TooltipConfig.strokeColor)).label(l(tr, "tooltip", "strokeColor")).helper(h(tr, "tooltip", "strokeColor")).build()
                .endSection()
                .section("waila")
                    .title(tr.apply("modernui.config.cat.waila"))
                    .bool("enabled").defaultValue(TooltipConfig.waila).label(l(tr, "waila", "enabled")).helper(h(tr, "waila", "enabled")).build()
                    .number("cornerRadius").defaultValue(Double.valueOf(TooltipConfig.wailaCornerRadius)).range(0, 16).slider().label(l(tr, "waila", "cornerRadius")).helper(h(tr, "waila", "cornerRadius")).build()
                    .number("borderWidth").defaultValue(Double.valueOf(TooltipConfig.wailaBorderWidth)).range(0, 8).slider().label(l(tr, "waila", "borderWidth")).helper(h(tr, "waila", "borderWidth")).build()
                .endSection()
                .section("smoothscroll")
                    .title(tr.apply("modernui.config.cat.smoothscroll"))
                    .bool("enabled").defaultValue(SmoothScrollConfig.enabled).label(l(tr, "smoothscroll", "enabled")).helper(h(tr, "smoothscroll", "enabled")).build()
                    .integer("listSmoothness").defaultValue(Long.valueOf(SmoothScrollConfig.listSmoothness)).range(0, 100).slider().label(l(tr, "smoothscroll", "listSmoothness")).helper(h(tr, "smoothscroll", "listSmoothness")).build()
                    .integer("listAmount").defaultValue(Long.valueOf(SmoothScrollConfig.listAmount)).range(0, 200).slider().label(l(tr, "smoothscroll", "listAmount")).helper(h(tr, "smoothscroll", "listAmount")).build()
                    .integer("chatSmoothness").defaultValue(Long.valueOf(SmoothScrollConfig.chatSmoothness)).range(0, 100).slider().label(l(tr, "smoothscroll", "chatSmoothness")).helper(h(tr, "smoothscroll", "chatSmoothness")).build()
                    .integer("chatAmount").defaultValue(Long.valueOf(SmoothScrollConfig.chatAmount)).range(0, 200).slider().label(l(tr, "smoothscroll", "chatAmount")).helper(h(tr, "smoothscroll", "chatAmount")).build()
                    .integer("chatOpenSmoothness").defaultValue(Long.valueOf(SmoothScrollConfig.chatOpenSmoothness)).range(0, 100).slider().label(l(tr, "smoothscroll", "chatOpenSmoothness")).helper(h(tr, "smoothscroll", "chatOpenSmoothness")).build()
                    .integer("creativeSmoothness").defaultValue(Long.valueOf(SmoothScrollConfig.creativeSmoothness)).range(0, 100).slider().label(l(tr, "smoothscroll", "creativeSmoothness")).helper(h(tr, "smoothscroll", "creativeSmoothness")).build()
                    .integer("creativeAmount").defaultValue(Long.valueOf(SmoothScrollConfig.creativeAmount)).range(0, 200).slider().label(l(tr, "smoothscroll", "creativeAmount")).helper(h(tr, "smoothscroll", "creativeAmount")).build()
                    .integer("textSmoothness").defaultValue(Long.valueOf(SmoothScrollConfig.textSmoothness)).range(0, 100).slider().label(l(tr, "smoothscroll", "textSmoothness")).helper(h(tr, "smoothscroll", "textSmoothness")).build()
                    .integer("textAmount").defaultValue(Long.valueOf(SmoothScrollConfig.textAmount)).range(0, 300).slider().label(l(tr, "smoothscroll", "textAmount")).helper(h(tr, "smoothscroll", "textAmount")).build()
                .endSection()
                .section("screentransition")
                    .title(tr.apply("modernui.config.cat.screentransition"))
                    .bool("enabled").defaultValue(ScreenTransition.enabled).label(l(tr, "screentransition", "enabled")).helper(h(tr, "screentransition", "enabled")).build()
                    .integer("durationMs").defaultValue(Long.valueOf(ScreenTransition.durationMs)).range(0, 2000).slider().label(l(tr, "screentransition", "durationMs")).helper(h(tr, "screentransition", "durationMs")).build()
                    .number("offset").defaultValue(Double.valueOf(ScreenTransition.offset)).range(0, 64).slider().label(l(tr, "screentransition", "offset")).helper(h(tr, "screentransition", "offset")).build()
                    .number("scale").defaultValue(Double.valueOf(ScreenTransition.scale)).range(0, 4).slider().label(l(tr, "screentransition", "scale")).helper(h(tr, "screentransition", "scale")).build()
                .endSection()
                .section("zoom")
                    .title(tr.apply("modernui.config.cat.zoom"))
                    .bool("enabled").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.enabled).label(l(tr, "zoom", "enabled")).helper(h(tr, "zoom", "enabled")).build()
                    .number("factor").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.factor)).range(1, 20).slider().label(l(tr, "zoom", "factor")).helper(h(tr, "zoom", "factor")).build()
                    .bool("smooth").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.smooth).label(l(tr, "zoom", "smooth")).helper(h(tr, "zoom", "smooth")).build()
                    .number("speed").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.speed)).range(0.02, 1).slider().label(l(tr, "zoom", "speed")).helper(h(tr, "zoom", "speed")).build()
                    .number("sensitivity").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.sensitivity)).range(1, 5).slider().label(l(tr, "zoom", "sensitivity")).helper(h(tr, "zoom", "sensitivity")).build()
                    .integer("keyCode").defaultValue(Long.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.keyCode)).range(0, 255).slider().label(l(tr, "zoom", "keyCode")).helper(h(tr, "zoom", "keyCode")).build()
                .endSection()
                .section("chat")
                    .title(tr.apply("modernui.config.cat.chat"))
                    .bool("headsEnabled").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headsEnabled).label(l(tr, "chat", "headsEnabled")).helper(h(tr, "chat", "headsEnabled")).build()
                    .number("headSize").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headSize)).range(4, 16).slider().label(l(tr, "chat", "headSize")).helper(h(tr, "chat", "headSize")).build()
                    .number("headGap").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headGap)).range(0, 8).slider().label(l(tr, "chat", "headGap")).helper(h(tr, "chat", "headGap")).build()
                .endSection()
                .section("performance")
                    .title(tr.apply("modernui.config.cat.performance"))
                    .bool("unfocusedFramerate").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.framerateEnabled).label(l(tr, "performance", "unfocusedFramerate")).helper(h(tr, "performance", "unfocusedFramerate")).build()
                    .integer("unfocusedFramerateLimit").defaultValue(Long.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.framerateLimit)).range(1, 120).slider().label(l(tr, "performance", "unfocusedFramerateLimit")).helper(h(tr, "performance", "unfocusedFramerateLimit")).build()
                    .bool("unfocusedVolume").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.volumeEnabled).label(l(tr, "performance", "unfocusedVolume")).helper(h(tr, "performance", "unfocusedVolume")).build()
                    .number("unfocusedVolumeFactor").defaultValue(Double.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.volumeFactor)).range(0, 1).slider().label(l(tr, "performance", "unfocusedVolumeFactor")).helper(h(tr, "performance", "unfocusedVolumeFactor")).build()
                    .bool("preload").defaultValue(Boolean.TRUE).label(l(tr, "performance", "preload")).helper(h(tr, "performance", "preload")).build()
                .endSection()
                .section("ui")
                    .title(tr.apply("modernui.config.cat.ui"))
                    .bool("textUndoEnabled").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.textUndoEnabled).label(l(tr, "ui", "textUndoEnabled")).helper(h(tr, "ui", "textUndoEnabled")).build()
                    .integer("textUndoLimit").defaultValue(Long.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.textUndoLimit)).range(1, 1000).slider().label(l(tr, "ui", "textUndoLimit")).helper(h(tr, "ui", "textUndoLimit")).build()
                    .bool("pauseContainers").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.VanillaPauseConfig.enabled).label(l(tr, "ui", "pauseContainers")).helper(h(tr, "ui", "pauseContainers")).build()
                .endSection()
                .section("textselection")
                    .title(tr.apply("modernui.config.cat.textselection"))
                    .bool("selectionEnabled").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionEnabled).label(l(tr, "textselection", "selectionEnabled")).helper(h(tr, "textselection", "selectionEnabled")).build()
                    .integer("selectionColor").defaultValue(Long.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionColor)).range(0, 0xFFFFFF).slider().label(l(tr, "textselection", "selectionColor")).helper(h(tr, "textselection", "selectionColor")).build()
                    .integer("selectionAlpha").defaultValue(Long.valueOf(blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionAlpha)).range(0, 255).slider().label(l(tr, "textselection", "selectionAlpha")).helper(h(tr, "textselection", "selectionAlpha")).build()
                    .number("selectionOffsetX").defaultValue(Double.valueOf((double) blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionOffsetX)).range(-16, 16).input().label(l(tr, "textselection", "selectionOffsetX")).helper(h(tr, "textselection", "selectionOffsetX")).build()
                    .number("selectionOffsetY").defaultValue(Double.valueOf((double) blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionOffsetY)).range(-16, 16).input().label(l(tr, "textselection", "selectionOffsetY")).helper(h(tr, "textselection", "selectionOffsetY")).build()
                .endSection()
                .section("emoji")
                    .title(tr.apply("modernui.config.cat.emoji"))
                    .bool("enabled").defaultValue(blocamlimb.nhx100218.modernui.enhance.vanilla.EmojiConfig.enabled).label(l(tr, "emoji", "enabled")).helper(h(tr, "emoji", "enabled")).build()
                .endSection()
                ;
    }

    private static String l(Function<String, String> tr, String section, String key) {
        return tr.apply("modernui.config." + section + "." + key);
    }

    private static String h(Function<String, String> tr, String section, String key) {
        return tr.apply("modernui.config." + section + "." + key + ".tooltip");
    }

    private static List<String> hexList(int[] values) {
        List<String> list = new ArrayList<String>();
        for (int value : values) {
            list.add(String.format("%08X", value));
        }
        return list;
    }

    // ---------- 回灌 ----------

    /** 从权威源全量回灌运行态静态字段（启动加载 / 保存 / 磁盘重载统一入口）。 */
    public static void apply(Authority authority) {
        if (authority == null) {
            return;
        }

        TooltipConfig.enabled = authority.getBool("tooltip.enabled");
        TooltipConfig.rounded = authority.getBool("tooltip.rounded");
        TooltipConfig.centerTitle = authority.getBool("tooltip.centerTitle");
        TooltipConfig.titleBreak = authority.getBool("tooltip.titleBreak");
        TooltipConfig.adaptiveColors = authority.getBool("tooltip.adaptiveColors");
        TooltipConfig.borderColorCycle = (int) Math.round(authority.getNumber("tooltip.borderColorCycle"));
        TooltipConfig.borderWidth = (float) authority.getNumber("tooltip.borderWidth");
        TooltipConfig.cornerRadius = (float) authority.getNumber("tooltip.cornerRadius");
        TooltipConfig.modularUiMaxWidth = (int) Math.round(authority.getNumber("tooltip.modularUiMaxWidth"));
        TooltipConfig.modularUiNoWrap = authority.getBool("tooltip.modularUiNoWrap");
        readColors(authority, "tooltip.fillColor", TooltipConfig.fillColor);
        readColors(authority, "tooltip.strokeColor", TooltipConfig.strokeColor);

        TooltipConfig.waila = authority.getBool("waila.enabled");
        TooltipConfig.wailaCornerRadius = (float) authority.getNumber("waila.cornerRadius");
        TooltipConfig.wailaBorderWidth = (float) authority.getNumber("waila.borderWidth");

        SmoothScrollConfig.enabled = authority.getBool("smoothscroll.enabled");
        SmoothScrollConfig.listSmoothness = (int) Math.round(authority.getNumber("smoothscroll.listSmoothness"));
        SmoothScrollConfig.listAmount = (int) Math.round(authority.getNumber("smoothscroll.listAmount"));
        SmoothScrollConfig.chatSmoothness = (int) Math.round(authority.getNumber("smoothscroll.chatSmoothness"));
        SmoothScrollConfig.chatAmount = (int) Math.round(authority.getNumber("smoothscroll.chatAmount"));
        SmoothScrollConfig.chatOpenSmoothness = (int) Math.round(authority.getNumber("smoothscroll.chatOpenSmoothness"));
        SmoothScrollConfig.creativeSmoothness = (int) Math.round(authority.getNumber("smoothscroll.creativeSmoothness"));
        SmoothScrollConfig.creativeAmount = (int) Math.round(authority.getNumber("smoothscroll.creativeAmount"));
        SmoothScrollConfig.textSmoothness = (int) Math.round(authority.getNumber("smoothscroll.textSmoothness"));
        SmoothScrollConfig.textAmount = (int) Math.round(authority.getNumber("smoothscroll.textAmount"));

        ScreenTransition.enabled = authority.getBool("screentransition.enabled");
        ScreenTransition.durationMs = (int) Math.round(authority.getNumber("screentransition.durationMs"));
        ScreenTransition.offset = (float) authority.getNumber("screentransition.offset");
        ScreenTransition.scale = (float) authority.getNumber("screentransition.scale");

        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.enabled = authority.getBool("zoom.enabled");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.factor = (float) authority.getNumber("zoom.factor");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.smooth = authority.getBool("zoom.smooth");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.speed = (float) authority.getNumber("zoom.speed");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.sensitivity = (float) authority.getNumber("zoom.sensitivity");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.keyCode = (int) Math.round(authority.getNumber("zoom.keyCode"));

        blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headsEnabled = authority.getBool("chat.headsEnabled");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headSize = (float) authority.getNumber("chat.headSize");
        blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig.headGap = (float) authority.getNumber("chat.headGap");

        blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.framerateEnabled = authority.getBool("performance.unfocusedFramerate");
        blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.framerateLimit = (int) Math.round(authority.getNumber("performance.unfocusedFramerateLimit"));
        blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.volumeEnabled = authority.getBool("performance.unfocusedVolume");
        blocamlimb.nhx100218.modernui.enhance.vanilla.UnfocusedConfig.volumeFactor = (float) authority.getNumber("performance.unfocusedVolumeFactor");
        blocamlimb.nhx100218.modernui.enhance.client.PreloadService.enabled = authority.getBool("performance.preload");

        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.textUndoEnabled = authority.getBool("ui.textUndoEnabled");
        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.textUndoLimit = (int) Math.round(authority.getNumber("ui.textUndoLimit"));
        blocamlimb.nhx100218.modernui.enhance.vanilla.VanillaPauseConfig.enabled = authority.getBool("ui.pauseContainers");

        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionEnabled = authority.getBool("textselection.selectionEnabled");
        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionColor = (int) Math.round(authority.getNumber("textselection.selectionColor"));
        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionAlpha = (int) Math.round(authority.getNumber("textselection.selectionAlpha"));
        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionOffsetX = (float) authority.getNumber("textselection.selectionOffsetX");
        blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig.selectionOffsetY = (float) authority.getNumber("textselection.selectionOffsetY");

        blocamlimb.nhx100218.modernui.enhance.vanilla.EmojiConfig.enabled = authority.getBool("emoji.enabled");
    }

    private static void readColors(Authority authority, String path, int[] target) {
        Object value = authority.get(path);
        if (!(value instanceof List)) {
            return;
        }
        List<?> list = (List<?>) value;
        for (int i = 0; i < target.length && i < list.size(); i++) {
            try {
                target[i] = (int) Long.parseLong(String.valueOf(list.get(i)).replace("#", "").trim(), 16);
            } catch (NumberFormatException ignored) {
                // 保留默认值
            }
        }
    }
}
