package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.HotbarSmoothScroll;

import net.minecraft.entity.player.InventoryPlayer;

/**
 * 快捷栏滚轮：捕获 {@code changeCurrentItem} 的前后槽位与方向，交给
 * {@link HotbarSmoothScroll#onWheel} 决定是否环绕（键盘 1-9 直接改槽位、不走此方法）。
 */
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayerHotbar {

    @Shadow
    public int currentItem;

    private int modernui$oldSlot;

    @Inject(method = "changeCurrentItem", at = @At("HEAD"))
    private void modernui$head(int direction, CallbackInfo ci) {
        modernui$oldSlot = currentItem;
    }

    @Inject(method = "changeCurrentItem", at = @At("RETURN"))
    private void modernui$ret(int direction, CallbackInfo ci) {
        HotbarSmoothScroll.onWheel(modernui$oldSlot, currentItem, direction);
    }
}
