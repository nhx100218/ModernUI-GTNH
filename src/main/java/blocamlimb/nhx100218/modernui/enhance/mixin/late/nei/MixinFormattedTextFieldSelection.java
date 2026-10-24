package blocamlimb.nhx100218.modernui.enhance.mixin.late.nei;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;

/**
 * NEI 的 {@code FormattedTextField} 覆写了 {@code drawTextBox} 并自带 private {@code drawCursorVertical}
 * （原版 XOR 蓝框），此处同样替换为 ModernUI 风格半透明浅蓝填充。
 */
@Mixin(targets = "codechicken.nei.FormattedTextField")
public class MixinFormattedTextFieldSelection {

    @Inject(method = "drawCursorVertical(IIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$selection(int x1, int y1, int x2, int y2, CallbackInfo ci) {
        if (!UiConfig.selectionEnabled) {
            return;
        }
        // 整体按配置偏移对齐 ModernUI 替换后的字体字形（见 UiConfig.selectionOffsetX/Y，支持小数）。
        float left = Math.min(x1, x2) + UiConfig.selectionOffsetX;
        float right = Math.max(x1, x2) + UiConfig.selectionOffsetX;
        float top = Math.min(y1, y2) + UiConfig.selectionOffsetY;
        float bottom = Math.max(y1, y2) + UiConfig.selectionOffsetY;
        int maxX = ((GuiTextField) (Object) this).xPosition + ((GuiTextField) (Object) this).width;
        if (left > maxX) {
            left = maxX;
        }
        if (right > maxX) {
            right = maxX;
        }

        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_I(UiConfig.selectionColor, UiConfig.selectionAlpha);
        tessellator.addVertex((double) left, (double) bottom, 0.0D);
        tessellator.addVertex((double) right, (double) bottom, 0.0D);
        tessellator.addVertex((double) right, (double) top, 0.0D);
        tessellator.addVertex((double) left, (double) top, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        ci.cancel();
    }
}
