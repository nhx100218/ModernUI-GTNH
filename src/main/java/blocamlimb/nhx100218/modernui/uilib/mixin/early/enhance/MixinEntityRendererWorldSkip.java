package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.ui.screen.McScreenBridge;
import blocamlimb.nhx100218.modernui.uilib.util.AngelicaRendererCompat;

/**
 * SDL GPU 下打开 ModernUI 全屏界面时跳过世界渲染。
 *
 * <p>配置界面是 2420x1668 级别的全屏 UI；SDL/D3D12 后端同时渲染世界和这层 UI
 * 会持续数秒满载并最终挂死 GPU。界面本身已有不透明底色，跳过世界渲染只影响
 * 背景，不影响 UI/交互；关闭界面后下一帧立即恢复世界渲染。</p>
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererWorldSkip {

    @Inject(method = "renderWorld(FJ)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$skipWorldBehindSdlScreen(float partialTicks, long finishTimeNano,
            CallbackInfo callbackInfo) {
        if (!AngelicaRendererCompat.isSdlGpuActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !(minecraft.currentScreen instanceof McScreenBridge)) {
            return;
        }
        // 先清掉上一帧 UI 可能残留的 scissor / stencil / 写掩码；否则 glClear 只清
        // 一个很窄的裁剪区，其余区域保留上一帧内容，形成从左上角发散的残影/闪烁。
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        // 与 ConfigTheme.ROOT_BG 接近的深色底；界面自己会再画不透明背景。
        GL11.glClearColor(0.043F, 0.039F, 0.047F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        callbackInfo.cancel();
    }
}
