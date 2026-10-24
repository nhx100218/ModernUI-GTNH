package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

/** 按 C 缩放：改 FOV、按住时隐藏手臂、按倍率降低视角灵敏度。 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererZoom {

    @Shadow
    private float fovModifierHand;

    @Shadow
    private Minecraft mc;

    @Inject(method = "updateFovModifierHand", at = @At("RETURN"))
    private void modernui$zoomFov(CallbackInfo ci) {
        float zoom = ZoomState.update();
        if (zoom > 1.0001F) {
            fovModifierHand /= zoom;
        }
    }

    // 在读取原始鼠标增量之后、计算朝向之前缩放（晚于 lwjgl3ify 的 MouseHelper 注入）。
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/MouseHelper;mouseXYChange()V", shift = At.Shift.AFTER))
    private void modernui$scaleMouse(float partialTicks, CallbackInfo ci) {
        float zoom = ZoomState.current();
        if (zoom <= 1.0001F || mc == null || mc.mouseHelper == null) {
            return;
        }
        // 额外再慢一点
        zoom *= Math.max(1.0F, blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomConfig.sensitivity);
        // lwjgl3ify 下视角走 FloatMouseHelper 的 float 增量，两个都缩放。
        if (mc.mouseHelper instanceof me.eigenraven.lwjgl3ify.api.FloatMouseHelper) {
            me.eigenraven.lwjgl3ify.api.FloatMouseHelper helper =
                    (me.eigenraven.lwjgl3ify.api.FloatMouseHelper) mc.mouseHelper;
            helper.lwjgl3ify$setFloatDX(helper.lwjgl3ify$getFloatDX() / zoom);
            helper.lwjgl3ify$setFloatDY(helper.lwjgl3ify$getFloatDY() / zoom);
        }
        mc.mouseHelper.deltaX = Math.round(mc.mouseHelper.deltaX / zoom);
        mc.mouseHelper.deltaY = Math.round(mc.mouseHelper.deltaY / zoom);
    }

    // 按住即隐藏，不等待视角完全恢复。
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void modernui$hideHand(float partialTicks, int xOffset, CallbackInfo ci) {
        if (ZoomState.isActive()) {
            ci.cancel();
        }
    }
}
