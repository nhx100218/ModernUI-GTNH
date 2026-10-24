package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.modularui;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.screen.RichTooltip;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRenderer;
import net.minecraft.item.ItemStack;

/**
 * 把 ModularUI2（格雷科技等 GTNH 界面）的 tooltip 方形背景替换为圆角。
 */
@Mixin(targets = "com.cleanroommc.modularui.drawable.GuiDraw")
public class MixinModularUiTooltip {

    @Inject(method = "drawTooltipBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private static void modernui$drawTooltipBackground(ItemStack stack, List<String> lines, int x, int y,
            int width, int height, RichTooltip tooltip, CallbackInfo ci) {
        // 适中留白（4px），既不过宽也不贴边。
        TooltipRenderer.drawSimpleBackground(x - 4, y - 4, width + 8, height + 8);
        ci.cancel();
    }
}
