package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import blocamlimb.nhx100218.modernui.enhance.vanilla.HotbarSmoothScroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

/**
 * callee 侧识别并偏移快捷栏选中框：纹理坐标 (0,22) 是原版快捷栏选中框。用缓动后的连续槽位重绘，
 * 并额外绘制 ± 一个周期的副本 + 裁剪到快捷栏范围，实现 8→0 / 0→8 的环绕滑入。
 */
@Mixin(Gui.class)
public abstract class MixinGuiHotbarTexture {

    @Shadow
    public float zLevel;

    private int modernui$logs;

    @Inject(method = "drawTexturedModalRect(IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void modernui$draw(int x, int y, int u, int v, int width, int height, CallbackInfo ci) {
        if (u != 0 || v != 22) {
            return;
        }
        int current = 0;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                current = mc.thePlayer.inventory.currentItem;
            }
        } catch (Throwable ignored) {
        }
        float displayed = HotbarSmoothScroll.update(current);
        int offsetPixels = Math.round((displayed - (float) current) * 20.0F);
        // 把偏移折到 [-90, 90]（±半周期），避免连续环绕后偏移越积越大导致选中框消失；
        // 精确落在整周期上时收回原版绘制（避免静止时对边残留副本）。
        int cycle = HotbarSmoothScroll.SLOTS * 20;
        offsetPixels -= cycle * (int) Math.round((double) offsetPixels / (double) cycle);
        if ((modernui$logs++ % 120) == 0) {
            MyMod.LOG.info("[GuiDraw] selector x={} current={} displayed={} offset={}", Integer.valueOf(x),
                    Integer.valueOf(current), Float.valueOf(displayed), Integer.valueOf(offsetPixels));
        }
        if (offsetPixels == 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        // 快捷栏左边界：x = left - 1 + current*20
        int left = x + 1 - current * 20;
        int top = y;
        int bottom = y + 24;
        int scLeft = left - 1;
        int scRight = left + 183;

        ci.cancel();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        if (mc != null && left + 91 > 0) {
            int guiWidth = 2 * (left + 91);
            int scale = Math.max(1, mc.displayWidth / guiWidth);
            GL11.glScissor(scLeft * scale, mc.displayHeight - bottom * scale,
                    (scRight - scLeft) * scale, (bottom - top) * scale);
        }
        int baseX = x + offsetPixels;
        modernui$drawAt(baseX, y, u, v, width, height);
        // 只在选中框确实滑出某一侧时才画另一侧的进入副本，避免静止/普通滑动时的对边残边。
        if (baseX + width > scRight + 2) {
            modernui$drawAt(baseX - cycle, y, u, v, width, height);
        } else if (baseX < scLeft - 2) {
            modernui$drawAt(baseX + cycle, y, u, v, width, height);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void modernui$drawAt(int x, int y, int u, int v, int width, int height) {
        float f = 0.00390625F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double) x, (double) (y + height), (double) zLevel, (double) (u * f), (double) ((v + height) * f));
        tessellator.addVertexWithUV((double) (x + width), (double) (y + height), (double) zLevel, (double) ((u + width) * f), (double) ((v + height) * f));
        tessellator.addVertexWithUV((double) (x + width), (double) y, (double) zLevel, (double) ((u + width) * f), (double) (v * f));
        tessellator.addVertexWithUV((double) x, (double) y, (double) zLevel, (double) (u * f), (double) (v * f));
        tessellator.draw();
    }
}
