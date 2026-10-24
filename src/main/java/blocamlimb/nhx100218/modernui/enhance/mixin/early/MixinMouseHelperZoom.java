package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ZoomState;
import net.minecraft.util.MouseHelper;

/** 缩放时按倍率降低视角移动速度：在读取原始鼠标增量后缩放它。 */
@Mixin(MouseHelper.class)
public abstract class MixinMouseHelperZoom {

    @Shadow
    public int deltaX;

    @Shadow
    public int deltaY;

    @Inject(method = "mouseXYChange", at = @At("RETURN"))
    private void modernui$scaleDelta(CallbackInfo ci) {
        float zoom = ZoomState.current();
        if (zoom > 1.0001F) {
            deltaX = Math.round(deltaX / zoom);
            deltaY = Math.round(deltaY / zoom);
        }
    }
}
