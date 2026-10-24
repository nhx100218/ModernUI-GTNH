package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollModel;

import net.minecraft.client.gui.inventory.GuiContainerCreative;

/**
 * 创造模式物品栏滚动的指数平滑（对齐 Minecraft-Smooth-Scrolling 的 Creative Screen）。
 *
 * <p>1.7.10 的创造栏按行重排槽位，无法像现代版那样逐像素重绘，这里平滑 {@code currentScroll}
 * 目标本身，把整行跳变收成缓动，属 best-effort。</p>
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreativeSmoothScroll {

    @Shadow
    private float currentScroll;

    @Shadow
    private boolean isScrolling;

    private final SmoothScrollModel modernui$model = new SmoothScrollModel();
    private boolean modernui$tracking;

    @Inject(method = "handleMouseInput", at = @At("TAIL"))
    private void modernui$capture(CallbackInfo ci) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.creativeSmoothness == 0) {
            return;
        }
        modernui$model.setTarget(currentScroll);
        modernui$tracking = true;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void modernui$animate(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.creativeSmoothness == 0 || !modernui$tracking) {
            return;
        }
        if (isScrolling) {
            modernui$model.snap(currentScroll);
            return;
        }
        currentScroll = (float) modernui$model.update(
                SmoothScrollConfig.smoothness(SmoothScrollConfig.creativeSmoothness));
    }
}
