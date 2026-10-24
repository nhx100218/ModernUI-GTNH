package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.vanilla.VanillaPauseConfig;

import net.minecraft.client.gui.inventory.GuiContainer;

/** 让容器界面在单机下暂停游戏（原版 isGamePaused 每 tick 依据 doesGuiPauseGame 重算）。 */
@Mixin(GuiContainer.class)
public class MixinGuiContainerPause {

    @Inject(method = "doesGuiPauseGame", at = @At("HEAD"), cancellable = true)
    private void modernui$pause(CallbackInfoReturnable<Boolean> cir) {
        if (VanillaPauseConfig.enabled) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
