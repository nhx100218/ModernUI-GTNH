package blocamlimb.nhx100218.modernui.enhance.mixin.late.tconstruct;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;

/**
 * 匠魂炉（SmelteryGui）的液体/燃料 tooltip 由 TConstruct 自绘
 * {@code drawToolTip(List,int,int)}，这里替换为 UILib 的圆角 tooltip。
 *
 * <p><b>GL/渲染态侧效应</b>：TConstruct 的 {@code drawToolTip} 在绘制期间把
 * {@code GuiScreen.itemRender.zLevel}（以及 {@code Gui.zLevel}）设为 300，返回前复位为 0；
 * 且其调用方 {@code drawFluidStackTooltip} 只负责复位 {@code Gui.zLevel}。本 mixin 在 HEAD
 * cancel 后，这条 {@code itemRender.zLevel = 0} 的复位被一并跳过，残留值会让匠魂炉随后渲染的
 * 方块/物品按错误 z 偏移绘制（表现为阴影/位置错乱）。这里显式复刻该复位。</p>
 */
@Mixin(targets = "tconstruct.smeltery.gui.SmelteryGui")
public class MixinSmelteryGui {

    /** SmelteryGui 继承自 GuiScreen 的静态物品渲染器（TConstruct 自己的 drawToolTip 也会复位它）。 */
    @Shadow
    private static RenderItem itemRender;

    @Inject(method = "drawToolTip", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$drawToolTip(List<String> lines, int x, int y, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) {
            return;
        }
        if (TooltipRenderer.draw(lines, x, y, mc.fontRenderer, mc.currentScreen.width,
                mc.currentScreen.height)) {
            if (itemRender != null) {
                itemRender.zLevel = 0.0F;
            }
            ci.cancel();
        }
    }
}
