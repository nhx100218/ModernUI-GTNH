package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.ct;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.component.TextComponent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

/** CT 文本组件：把等于物品名的标题行居中，并在其下画一条灰线（受 centerTitle/titleBreak 控制）。 */
@Mixin(TextComponent.class)
public class MixinCtTextComponent {

    @Shadow
    protected List<String> textLines;

    @Shadow
    protected int spacing;

    @Inject(method = "draw(IIILcom/slprime/chromatictooltips/api/TooltipContext;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$draw(int x, int y, int width, TooltipContext ctx, CallbackInfo ci) {
        if (!TooltipConfig.enabled || textLines == null || textLines.isEmpty()) {
            return;
        }
        ItemStack stack = ctx == null ? null : ctx.getItem();
        String itemName = stack == null ? null : strip(stack.getDisplayName());
        boolean hasTitle = false;
        for (String line : textLines) {
            if (itemName != null && itemName.equals(strip(line))) {
                hasTitle = true;
                break;
            }
        }
        if (!hasTitle) {
            return;
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int lineStep = fr.FONT_HEIGHT + (spacing > 0 ? spacing : 2);
        int yy = y;
        for (String line : textLines) {
            boolean title = itemName != null && itemName.equals(strip(line));
            int lw = fr.getStringWidth(line);
            int lx = title && TooltipConfig.centerTitle ? x + (width - lw) / 2 : x;
            fr.drawStringWithShadow(line, lx, yy, 0xFFFFFF);
            if (title && TooltipConfig.titleBreak) {
                int ly = yy + fr.FONT_HEIGHT - 1;
                Gui.drawRect(x - 1, ly, x + width + 1, ly + 1, 0xFF555555);
            }
            yy += lineStep;
        }
        ci.cancel();
    }

    private static String strip(String s) {
        return s == null ? null : s.replaceAll("\u00a7.", "");
    }
}
