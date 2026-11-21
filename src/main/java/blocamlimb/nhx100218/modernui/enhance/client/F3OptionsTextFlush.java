package blocamlimb.nhx100218.modernui.enhance.client;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * Flushes the debug-options SDF text after the whole screen has rendered, and
 * adds the ModernUI header/footer separators around the list area. The colour
 * blocks and layout stay Angelica's own.
 */
public final class F3OptionsTextFlush {

    private static final Field LIST_X = fieldOf("listX");
    private static final Field LIST_W = fieldOf("listW");
    private static final Field VIEW_TOP = fieldOf("viewTop");
    private static final Field VIEW_H = fieldOf("viewH");

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.gui;
        if (screen == null || !"com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen"
                .equals(screen.getClass().getName())) {
            return;
        }
        drawSeparators(screen);
        F3FontBridge.flushScreenText();
        F3OptionsTooltip.draw(Minecraft.getMinecraft());
    }

    private static void drawSeparators(GuiScreen screen) {
        try {
            if (LIST_X == null || LIST_W == null || VIEW_TOP == null || VIEW_H == null) {
                return;
            }
            int left = LIST_X.getInt(screen);
            int width = LIST_W.getInt(screen);
            int top = VIEW_TOP.getInt(screen);
            int height = VIEW_H.getInt(screen);
            int right = left + width;
            int bottom = top + height;
            if (right <= left || bottom - top < 4) {
                return;
            }
            ModernGuiRenderer.beginUi();
            ModernMenuFrame.drawSeparators(left, top, right, bottom);
        } catch (Throwable ignored) {
            // Never let decoration break the screen.
        }
    }

    private static Field fieldOf(String name) {
        try {
            Field field = Class.forName("com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen")
                    .getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable absent) {
            return null;
        }
    }
}
