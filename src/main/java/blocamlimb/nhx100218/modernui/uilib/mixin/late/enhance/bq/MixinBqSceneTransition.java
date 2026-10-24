package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.bq;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ScreenTransition;

import betterquesting.api2.client.gui.IScene;

/** 任务书在同一 canvas 内切换场景时，复位界面切换动画，使其每次都播放。 */
@Mixin(targets = "betterquesting.api2.client.gui.SceneController")
public class MixinBqSceneTransition {

    @Inject(method = "setActiveScene(Lbetterquesting/api2/client/gui/IScene;)V", at = @At("HEAD"), remap = false)
    private static void modernui$onScene(IScene scene, CallbackInfo ci) {
        ScreenTransition.poke();
    }
}
