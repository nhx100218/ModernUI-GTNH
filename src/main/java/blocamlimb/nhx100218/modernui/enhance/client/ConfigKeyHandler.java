package blocamlimb.nhx100218.modernui.enhance.client;

import org.lwjgl.input.Keyboard;

import blocamlimb.nhx100218.modernui.uilib.config.modern.ModernConfigEntry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Ctrl+K 打开 ModernUI-GTNH 的唯一配置页。 */
public class ConfigKeyHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        try {
            if (Keyboard.getEventKeyState()
                    && Keyboard.getEventKey() == Keyboard.KEY_K
                    && GuiScreen.isCtrlKeyDown()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen == null) {
                    ModernConfigEntry.open();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
