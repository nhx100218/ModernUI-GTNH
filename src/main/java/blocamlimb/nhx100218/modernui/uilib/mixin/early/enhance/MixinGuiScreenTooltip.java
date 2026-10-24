package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/**
 * 接管原版悬浮提示（tooltip）。
 *
 * <p>整合包内 ChromaticTooltips 在 {@code renderToolTip}/{@code drawHoveringText} 的 HEAD
 * 注入并 cancel（priority=999）。本 mixin 以更高 priority（1500）先于其运行并接管，从而用
 * UILib 的 tooltip 样式覆盖它。</p>
 */
@Mixin(value = GuiScreen.class, priority = 1500)
public abstract class MixinGuiScreenTooltip {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    public Minecraft mc;

    @Shadow
    protected FontRenderer fontRendererObj;

    /** 物品 tooltip 的真正入口：原版在此组装文本行后调用 drawHoveringText。 */
    @SuppressWarnings("unchecked")
    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    private void modernui$renderToolTip(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack == null || !TooltipRenderer.isEnabled()) {
            return;
        }
        List<String> lines = (List<String>) stack
                .getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
        for (int k = 0; k < lines.size(); k++) {
            if (k == 0) {
                lines.set(k, stack.getRarity().rarityColor + lines.get(k));
            } else {
                lines.set(k, EnumChatFormatting.GRAY + lines.get(k));
            }
        }
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) {
            font = this.fontRendererObj;
        }
        if (TooltipRenderer.draw(lines, x, y, font, this.width, this.height)) {
            ci.cancel();
        }
    }

    // drawHoveringText 是 Forge 新增方法，运行期保留可读名，故 remap = false。
    // 用于创造栏等非物品 tooltip。
    @Inject(method = "drawHoveringText", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$drawHoveringText(List<String> textLines, int x, int y, FontRenderer font,
            CallbackInfo ci) {
        if (TooltipRenderer.draw(textLines, x, y, font, this.width, this.height)) {
            ci.cancel();
        }
    }
}
