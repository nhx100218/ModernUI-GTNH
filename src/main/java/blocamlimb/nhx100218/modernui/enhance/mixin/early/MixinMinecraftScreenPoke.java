package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ScreenTransition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** 每次显示界面都复位切换动画计时，修复“复用同一界面实例重复打开不再播放动画”。 */
@Mixin(Minecraft.class)
public class MixinMinecraftScreenPoke {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void modernui$poke(GuiScreen screen, CallbackInfo ci) {
        ScreenTransition.poke();
    }
}
