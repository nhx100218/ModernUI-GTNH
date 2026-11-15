package blocamlimb.nhx100218.modernui.enhance.client;

import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuCursor;

/**
 * Applies the 26.3 mouse cursor every rendered frame.
 *
 * <p>Several mod screens (notably Angelica's video settings) override
 * {@code drawScreen} without calling {@code GuiScreen.drawScreen}, so a hook on
 * that method never runs. Driving the cursor from the render tick covers every
 * screen, and lets the host be the only cache shared with ModernUI scenes.</p>
 */
public final class MenuCursorTicker {

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return;
        }
        if (!Mouse.isCreated()) {
            return;
        }
        GuiScreen screen = mc.currentScreen;
        int mouseX = Mouse.getX() * screen.width / mc.displayWidth;
        int mouseY = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;
        MenuCursor.update(screen, mouseX, mouseY);
    }
}
