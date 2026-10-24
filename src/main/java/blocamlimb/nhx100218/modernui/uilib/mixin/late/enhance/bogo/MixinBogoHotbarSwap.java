package blocamlimb.nhx100218.modernui.uilib.mixin.late.enhance.bogo;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.HotbarColumnSmooth;

import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * 让 BogoSorter 的「快捷栏竖向滚动」（按住 Alt 滚轮，{@code HotbarSwap}）高亮条带与快捷栏一致的
 * 平滑缓动：把 {@code verticalIndex} 的整数跳变缓动成连续位移（用像素级平移实现亚像素滑动），
 * 只平移高亮条本身，不动上方物品，避免出格或边框异常。
 */
@Mixin(targets = "com.cleanroommc.bogosorter.common.HotbarSwap")
public class MixinBogoHotbarSwap {

    @Shadow(remap = false)
    private static int verticalIndex;

    /** 每帧按当前 {@code verticalIndex} 推进缓动。 */
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void modernui$tick(RenderGameOverlayEvent.Post event, CallbackInfo ci) {
        HotbarColumnSmooth.update(verticalIndex);
    }

    /**
     * 高亮条原按 {@code verticalIndex} 整数定位；这里按缓动值补上亚像素位移
     * （{@code dy = (verticalIndex - displayed) * ROW_PX}），使滑动连续。
     */
    @Redirect(method = "render", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", remap = true))
    private void modernui$strip(GuiIngame gui, int x, int y, int u, int v, int w, int h) {
        if (HotbarColumnSmooth.active()) {
            float dy = (verticalIndex - HotbarColumnSmooth.displayed()) * HotbarColumnSmooth.ROW_PX;
            if (dy != 0.0F) {
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, dy, 0.0F);
                gui.drawTexturedModalRect(x, y, u, v, w, h);
                GL11.glPopMatrix();
                return;
            }
        }
        gui.drawTexturedModalRect(x, y, u, v, w, h);
    }
}
