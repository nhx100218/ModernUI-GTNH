package blocamlimb.nhx100218.modernui.enhance.client;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.client.event.GuiScreenEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Draws the 26.3 separators for Iris' custom list screens after the whole screen
 * has finished rendering.
 *
 * <p>Doing this inside {@code IrisGuiSlot.drawScreen} fights Angelica's GL state
 * tracker and makes the shader screen flicker. The Forge post-draw event runs in
 * the same place as the other ModernUI menu decorations, outside the list's own
 * render pass.</p>
 */
public final class IrisListSeparators {

    private static final Map<Class<?>, Field[]> SLOT_FIELDS = new ConcurrentHashMap<Class<?>, Field[]>();

    public IrisListSeparators() {
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.gui;
        if (screen == null || !usesIrisLists(screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !ModernMenuScreens.usesSharedListBackdrop(screen)) {
            return;
        }
        for (Field field : slotFields(screen.getClass())) {
            try {
                Object value = field.get(screen);
                if (value instanceof GuiSlot) {
                    GuiSlot slot = (GuiSlot) value;
                    ModernGuiRenderer.beginUi();
                    ModernMenuFrame.drawSeparators(slot.left, slot.top, slot.left + slot.width, slot.bottom);
                }
            } catch (Throwable ignored) {
                // Ignore a list that is being rebuilt.
            }
        }
    }

    private static boolean usesIrisLists(GuiScreen screen) {
        String name = screen.getClass().getName();
        return "net.coderbot.iris.gui.screen.ShaderPackScreen".equals(name)
                || "com.gtnewhorizons.angelica.client.gui.AtlasPackScreen".equals(name)
                || "com.gtnewhorizons.angelica.client.gui.DynamicLightsEntityScreen".equals(name);
    }

    private static Field[] slotFields(Class<?> type) {
        Field[] cached = SLOT_FIELDS.get(type);
        if (cached != null) {
            return cached;
        }
        java.util.List<Field> fields = new java.util.ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (GuiSlot.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (Throwable ignored) {
                        // Inaccessible field: skip it.
                    }
                }
            }
            current = current.getSuperclass();
        }
        Field[] result = fields.toArray(new Field[0]);
        SLOT_FIELDS.put(type, result);
        return result;
    }
}
