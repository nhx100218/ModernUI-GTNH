package blocamlimb.nhx100218.modernui.enhance.client;

import java.lang.ref.WeakReference;

import blocamlimb.nhx100218.modernui.enhance.MyMod;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.PanelTextField;
import net.minecraft.client.Minecraft;

/**
 * 让 BQ 的自绘文本框（{@code PanelTextField}）接入 lwjgl3ify 的输入法（IME）通道。
 *
 * <p>lwjgl3ify 只为原版 {@code GuiTextField} 屏启用文本输入（{@code MixinGuiTextInput} 仅挂在
 * 少数原版屏上），IME 组合文字也只投递给输入事件监听器。BQ 的搜索框既不是原版
 * {@code GuiTextField}、也不会触发 {@code beginTextInput}，因此输入法既不弹出也收不到中文。
 * 这里在 BQ 屏开/关时开关文本输入，并注册一个监听器把 IME 文本写入当前聚焦的 BQ 文本框。</p>
 */
public final class BqImeBridge {

    private static WeakReference<PanelTextField<?>> focused = new WeakReference<PanelTextField<?>>(null);
    private static boolean hooked;
    private static boolean hookReady;
    private static boolean begun;

    private BqImeBridge() {}

    /** 由文本框绘制时调用，记录当前聚焦的 BQ 文本框（仅聚焦时记录）。 */
    public static void trackFocus(PanelTextField<?> field) {
        if (field != null && field.isFocused()) {
            focused = new WeakReference<PanelTextField<?>>(field);
        }
    }

    /** BQ 屏打开：启用文本输入（弹出输入法）。 */
    public static void begin() {
        if (!ensureHook() || begun) {
            return;
        }
        try {
            me.eigenraven.lwjgl3ify.api.InputEvents.beginTextInput();
            begun = true;
        } catch (Throwable t) {
            MyMod.LOG.warn("[BQ] 启用输入法失败", t);
        }
    }

    /** BQ 屏关闭：关闭文本输入。 */
    public static void end() {
        if (!begun) {
            return;
        }
        begun = false;
        focused = new WeakReference<PanelTextField<?>>(null);
        try {
            me.eigenraven.lwjgl3ify.api.InputEvents.endTextInput();
        } catch (Throwable t) {
            MyMod.LOG.warn("[BQ] 关闭输入法失败", t);
        }
    }

    private static synchronized boolean ensureHook() {
        if (hooked) {
            return hookReady;
        }
        hooked = true;
        try {
            me.eigenraven.lwjgl3ify.api.InputEvents.addKeyboardListener(
                    new me.eigenraven.lwjgl3ify.api.InputEvents.KeyboardListener() {

                        @Override
                        public void onTextEvent(me.eigenraven.lwjgl3ify.api.InputEvents.TextEvent event) {
                            deliver(event == null ? null : event.text);
                        }
                    });
            hookReady = true;
        } catch (Throwable t) {
            hookReady = false;
            MyMod.LOG.warn("[BQ] 注册输入法监听失败（lwjgl3ify 缺失？）", t);
        }
        return hookReady;
    }

    private static void deliver(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        // 只桥接 BQ 自身按键处理会丢弃的字符（中文等非白名单字符）。
        // 白名单内的 ASCII 已由 onKeyTyped 写入，若这里再写一次会变成双份（"a"→"aa"）。
        if (!modernui$needsBridge(text)) {
            return;
        }
        PanelTextField<?> field = focused.get();
        if (field == null || !field.isFocused()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiScreenCanvas)) {
            return;
        }
        field.writeText(text);
    }

    /** 文本中是否含有原版字符白名单之外的字符（即 BQ 的 onKeyTyped 不会写入的字符）。 */
    private static boolean modernui$needsBridge(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
