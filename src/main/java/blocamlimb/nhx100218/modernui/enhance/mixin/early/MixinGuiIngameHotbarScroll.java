package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.HotbarSmoothScroll;

import net.minecraftforge.client.GuiIngameForge;

/**
 * 快捷栏选中框平滑：在 {@code GuiIngameForge.renderHotbar} 调用栈内打标，
 * 真正的选中框纹理偏移由 callee 侧 {@code MixinGuiHotbarTexture} 完成（不依赖调用点序号/调用点方法）。
 */
@Mixin(GuiIngameForge.class)
public class MixinGuiIngameHotbarScroll {

    @Inject(method = "renderHotbar", at = @At("HEAD"), remap = false)
    private void modernui$head(int width, int height, float partialTicks, CallbackInfo ci) {
        HotbarSmoothScroll.inRenderHotbar = true;
        HotbarSmoothScroll.drawCounter = 0;
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"), remap = false)
    private void modernui$return(int width, int height, float partialTicks, CallbackInfo ci) {
        HotbarSmoothScroll.inRenderHotbar = false;
    }
}
