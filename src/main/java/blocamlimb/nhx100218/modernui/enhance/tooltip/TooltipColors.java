package blocamlimb.nhx100218.modernui.enhance.tooltip;

import java.awt.Color;
import java.util.List;

/**
 * tooltip 颜色计算：解析首个颜色码并按 ModernUI 思路推导四角边框色。
 */
public final class TooltipColors {

    /** 原版 16 色表（§0..§f）。 */
    private static final int[] MC_COLORS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private TooltipColors() {}

    public static int[] fill(List<String> lines) {
        return TooltipConfig.fillColor.clone();
    }

    public static int[] stroke(List<String> lines) {
        if (!TooltipConfig.adaptiveColors) {
            return TooltipConfig.strokeColor.clone();
        }
        int base = firstColor(lines);
        if (base < 0) {
            return TooltipConfig.strokeColor.clone();
        }
        int alpha = (TooltipConfig.strokeColor[0] >>> 24) & 0xFF;
        float[] hsv = Color.RGBtoHSB((base >> 16) & 0xFF, (base >> 8) & 0xFF, base & 0xFF, null);
        float s = Math.min(hsv[1], 0.9F);
        float v = clamp(hsv[2], 0.2F, 0.85F);
        float h = hsv[0] * 360.0F;

        int c1 = argb(alpha, h, s, v);
        int c2 = argb(alpha, h + 15.0F, bump(s, 0.12F), bump(v, 0.08F));
        int c3 = argb(alpha, h + 27.0F, bump(s, 0.18F), bump(v, 0.12F));
        int c4 = argb(alpha, h - 10.0F, bump(s, -0.06F), bump(v, -0.04F));
        return new int[] { c1, c2, c3, c4 };
    }

    /** 取首行第一个有效颜色码的 RGB；无则返回 -1。 */
    public static int firstColor(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null) {
                continue;
            }
            for (int j = 0; j + 1 < line.length(); j++) {
                if (line.charAt(j) == '\u00a7') {
                    int code = Character.toLowerCase(line.charAt(j + 1));
                    if (code >= '0' && code <= '9') {
                        return MC_COLORS[code - '0'];
                    }
                    if (code >= 'a' && code <= 'f') {
                        return MC_COLORS[code - 'a' + 10];
                    }
                }
            }
            // 原版 renderToolTip 仅首行携带稀有度颜色；首行无颜色码即认为无自适应色。
            break;
        }
        return -1;
    }

    private static int argb(int alpha, float hueDeg, float s, float v) {
        int rgb = Color.HSBtoRGB(((hueDeg % 360.0F) + 360.0F) % 360.0F / 360.0F, clamp(s, 0.0F, 1.0F),
                clamp(v, 0.0F, 1.0F)) & 0xFFFFFF;
        return (alpha << 24) | rgb;
    }

    private static float bump(float value, float delta) {
        return clamp(value + delta, 0.0F, 1.0F);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
