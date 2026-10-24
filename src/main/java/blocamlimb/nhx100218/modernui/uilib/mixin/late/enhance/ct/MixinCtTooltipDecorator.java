package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.ct;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipColors;
import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;
import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRoundedRect;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.util.TooltipDecorator;

/**
 * 路线 A：保留 Chromatic Tooltips 的布局/内容/样式解析，仅把其背景(BACKGROUND)与边框(BORDER)
 * 装饰器的绘制替换为我们的圆角渐变渲染；其余装饰器（纹理/物品/分隔线等）放行。
 */
@Mixin(TooltipDecorator.class)
public class MixinCtTooltipDecorator {

    @Shadow
    protected int[] colors;

    @Shadow
    protected int thickness;

    @Unique
    private static Field modernui$typeField;

    @Inject(method = "draw(DDIILcom/slprime/chromatictooltips/api/TooltipContext;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$draw(double x, double y, int w, int h, TooltipContext ctx, int z, CallbackInfo ci) {
        if (!TooltipConfig.enabled || w <= 0 || h <= 0) {
            return;
        }
        String type = modernui$type(this);
        int f0;
        int f1;
        int f2;
        int f3;
        int c0;
        int c1;
        int c2;
        int c3;
        float bw;
        if ("BACKGROUND".equals(type)) {
            int[] fill = modernui$adaptive(ctx, true);
            f0 = fill[0];
            f1 = fill[1];
            f2 = fill[2];
            f3 = fill[3];
            c0 = c1 = c2 = c3 = 0;
            bw = 0.0F;
        } else if ("BORDER".equals(type)) {
            int[] stroke = modernui$adaptive(ctx, false);
            c0 = stroke[0];
            c1 = stroke[1];
            c2 = stroke[2];
            c3 = stroke[3];
            f0 = f1 = f2 = f3 = 0;
            bw = Math.max(0.0F, thickness);
        } else {
            return;
        }
        // 外扩，恢复未装 CT 时的内边距观感
        double pad = 1.5D;
        TooltipRoundedRect.draw(x - pad, y - pad, x + w + pad, y + h + pad, TooltipConfig.cornerRadius, bw, f0, f1, f2,
                f3, c0, c1, c2, c3, z);
        ci.cancel();
    }

    @Unique
    private static String modernui$type(Object self) {
        try {
            if (modernui$typeField == null) {
                modernui$typeField = TooltipDecorator.class.getDeclaredField("decoratorType");
                modernui$typeField.setAccessible(true);
            }
            Object dt = modernui$typeField.get(self);
            return dt == null ? null : ((Enum<?>) dt).name();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 按物品名解析四角颜色（与 Waila/GT 同源）；失败回退配置默认。 */
    private static int[] modernui$adaptive(TooltipContext ctx, boolean fill) {
        int[] def = fill ? TooltipConfig.fillColor : TooltipConfig.strokeColor;
        try {
            if (ctx != null) {
                net.minecraft.item.ItemStack stack = ctx.getItem();
                if (stack != null) {
                    String name = stack.getDisplayName();
                    try {
                        net.minecraft.item.EnumRarity rarity = stack.getRarity();
                        if (rarity != null && rarity.rarityColor != null) {
                            name = rarity.rarityColor.toString() + name;
                        }
                    } catch (Throwable ignoredRarity) {
                    }
                    int[] c = fromLines(java.util.Collections.singletonList(name), fill);
                    if (c != null) {
                        return c;
                    }
                }
                // 无物品栈（如任务书文本 tooltip）：用 tooltip 首行文本取色
                java.util.List<net.minecraft.util.IChatComponent> dummy = null;
                int[] byText = fromContext(ctx, fill);
                if (byText != null) {
                    return byText;
                }
                if (dummy != null) {
                    return def;
                }
            }
        } catch (Throwable ignored) {
        }
        return def != null && def.length >= 4 ? def : new int[] { 0, 0, 0, 0 };
    }

    private static int[] fromLines(java.util.List<String> lines, boolean fill) {
        int[] c = fill ? TooltipColors.fill(lines) : TooltipColors.stroke(lines);
        if (c != null && c.length == 1) {
            return new int[] { c[0], c[0], c[0], c[0] };
        }
        if (c != null && c.length >= 4) {
            return c;
        }
        return null;
    }

    /** 从 TooltipContext 的组件里取首行文本颜色。 */
    private static int[] fromContext(TooltipContext ctx, boolean fill) {
        if (ctx == null) {
            return null;
        }
        try {
            java.util.List<?> comps = ctx.getContextTooltip();
            if (comps == null) {
                return null;
            }
            for (Object o : comps) {
                if (o instanceof com.slprime.chromatictooltips.component.TextComponent) {
                    java.util.List<String> ls = ((com.slprime.chromatictooltips.component.TextComponent) o).getLines();
                    if (ls != null && !ls.isEmpty()) {
                        int[] c = fromLines(ls, fill);
                        if (c != null) {
                            return c;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int col(int[] c, int i) {
        if (c == null || c.length == 0) {
            return 0;
        }
        if (c.length == 1) {
            return c[0];
        }
        if (c.length == 2) {
            return c[i <= 0 ? 0 : 1];
        }
        return c[Math.min(i, c.length - 1)];
    }
}
