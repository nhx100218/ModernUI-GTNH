package blocamlimb.nhx100218.modernui.enhance.mixin.late.bq;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.client.BqImeBridge;

/**
 * BQ 屏（{@code GuiScreenCanvas}）打开/关闭时开关 lwjgl3ify 文本输入，使输入法在 BQ 界面也能激活。
 * 见 {@link BqImeBridge}。
 */
@Mixin(targets = "betterquesting.api2.client.gui.GuiScreenCanvas")
public class MixinBqScreenCanvas {

    @Inject(method = "func_73866_w_", at = @At("HEAD"), remap = false)
    private void modernui$beginIme(CallbackInfo ci) {
        BqImeBridge.begin();
    }

    @Inject(method = "func_146281_b", at = @At("HEAD"), remap = false)
    private void modernui$endIme(CallbackInfo ci) {
        BqImeBridge.end();
    }
}
