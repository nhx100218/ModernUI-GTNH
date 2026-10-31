package blocamlimb.nhx100218.modernui.uilib.config.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import blocamlimb.nhx100218.modernui.config.ui.ConfigScreen;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.UiSurface;
import blocamlimb.nhx100218.modernui.uilib.ui.screen.McScreenBridge;

/**
 * 新架构配置页的 MC GuiScreen 宿主。
 *
 * <p>把反射拿到的 {@code ConfigScreen}（经 {@link ConfigUiBridge} 转成 {@link UiSurface}）
 * 包进 {@link McScreenBridge}，接入 MC GuiScreen 生命周期。</p>
 *
 * <h3>合规边界</h3>
 * <ul>
 *   <li>本类位于 {@code uilib.config.modern}（mod 配置接入包），非 {@code uilib.ui.*} 通用组件包。</li>
 *   <li>只依赖 uilib 自身的宿主/surface 契约与 MC 客户端类型，零 {@code config.ui.*} 依赖。</li>
 *   <li>{@link McScreenBridge} 构造器为 {@code protected}，子类跨包可访问（通过继承）。</li>
 * </ul>
 */
public class ModernConfigScreen extends McScreenBridge {

    /**
     * 创建新架构配置页宿主。
     *
     * @param parentScreen 关闭后返回的父界面，可为 null
     * @param surface      配置页渲染面（由 {@link ConfigUiBridge} 反射构建）
     */
    public ModernConfigScreen(GuiScreen parentScreen, UiSurface surface) {
        super(parentScreen, surface);
        if (surface instanceof ConfigScreen) {
            ((ConfigScreen) surface).setWorldBackdrop(hasWorldContext());
        }
    }

    @Override
    public void drawDefaultBackground() {
        if (!hasWorldContext()) {
            Gui.drawRect(0, 0, width, height, 0xFF000000);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean hasWorldContext() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.theWorld != null;
    }
}
