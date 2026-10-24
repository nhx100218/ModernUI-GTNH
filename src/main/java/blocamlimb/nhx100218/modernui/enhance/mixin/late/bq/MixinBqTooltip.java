package blocamlimb.nhx100218.modernui.enhance.mixin.late.bq;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipConfig;
import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRenderer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

/** 任务书(BetterQuesting)物品 tooltip：改用我方圆角样式（避免原版/NEl 式背景）。 */
@Mixin(targets = { "betterquesting.api2.client.gui.GuiScreenCanvas",
        "betterquesting.api2.client.gui.GuiContainerCanvas" })
public class MixinBqTooltip {

    private static boolean modernui$ctChecked;
    private static boolean modernui$ctPresent;

    @Inject(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$tooltip(List<?> textLines, int mouseX, int mouseY, FontRenderer font, CallbackInfo ci) {
        if (!TooltipConfig.enabled) {
            return;
        }
        // 装了 Chromatic Tooltips 时交给 CT 处理，避免双方重复绘制导致空白
        if (!modernui$ctChecked) {
            modernui$ctChecked = true;
            try {
                modernui$ctPresent = cpw.mods.fml.common.Loader.isModLoaded("chromatictooltips");
            } catch (Throwable ignored) {
                modernui$ctPresent = false;
            }
        }
        if (modernui$ctPresent) {
            return;
        }
        GuiScreen self = (GuiScreen) (Object) this;
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) textLines;
        if (TooltipRenderer.draw(lines, mouseX, mouseY, font, self.width, self.height)) {
            ci.cancel();
        }
    }
}
