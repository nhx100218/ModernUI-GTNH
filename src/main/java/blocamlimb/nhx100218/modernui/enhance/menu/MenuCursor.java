package blocamlimb.nhx100218.modernui.enhance.menu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;

import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiCursor;
import blocamlimb.nhx100218.modernui.uilib.ui.host.SystemUiCursorHost;
import blocamlimb.nhx100218.modernui.uilib.ui.screen.McScreenBridge;

/**
 * Maps 1.7.10 widgets to the 26.3 mouse cursors.
 *
 * <p>Buttons and clickable list rows use the pointer/hand cursor, text fields
 * use the I-beam and sliders use the horizontal-resize cursor. The actual
 * native cursor is applied through ModernUI's existing cross-backend cursor
 * host, so SDL/Metal and GL paths share one implementation.</p>
 */
public final class MenuCursor {

    private static final Map<Class<?>, Field[]> TEXT_FIELD_CACHE = new ConcurrentHashMap<Class<?>, Field[]>();
    private static final Map<Class<?>, Field[]> SLOT_CACHE = new ConcurrentHashMap<Class<?>, Field[]>();
    private static final Field BUTTON_LIST_FIELD = findButtonListField();
    private static UiCursor lastCursor;

    private MenuCursor() {
    }

    /** Recomputes and applies the cursor for the current screen. */
    public static void update(GuiScreen screen, int mouseX, int mouseY) {
        if (screen == null) {
            apply(UiCursor.DEFAULT);
            return;
        }
        // The ModernUI config scene owns its cursor through SceneCursor.
        if (screen instanceof McScreenBridge) {
            return;
        }

        if (overTextField(screen, mouseX, mouseY)) {
            apply(UiCursor.TEXT);
            return;
        }
        GuiButton button = buttonAt(screen, mouseX, mouseY);
        if (button != null) {
            apply(button instanceof GuiOptionSlider ? UiCursor.EW_RESIZE : UiCursor.POINTER);
            return;
        }
        if (overSlot(screen, mouseX, mouseY)) {
            apply(UiCursor.POINTER);
            return;
        }
        apply(UiCursor.DEFAULT);
    }

    public static void reset() {
        apply(UiCursor.DEFAULT);
    }

    private static void apply(UiCursor cursor) {
        if (cursor == lastCursor) {
            return;
        }
        lastCursor = cursor;
        SystemUiCursorHost.getInstance().applyCursor(cursor);
    }

    private static boolean overTextField(GuiScreen screen, int mouseX, int mouseY) {
        for (Field field : textFields(screen.getClass())) {
            try {
                Object value = field.get(screen);
                if (value instanceof GuiTextField) {
                    GuiTextField fieldValue = (GuiTextField) value;
                    if (fieldValue.getVisible()
                            && mouseX >= fieldValue.xPosition && mouseX < fieldValue.xPosition + fieldValue.width
                            && mouseY >= fieldValue.yPosition && mouseY < fieldValue.yPosition + fieldValue.height) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                // A screen may replace its field while drawing; ignore one frame.
            }
        }
        return false;
    }

    private static GuiButton buttonAt(GuiScreen screen, int mouseX, int mouseY) {
        if (BUTTON_LIST_FIELD == null) {
            return null;
        }
        try {
            Object value = BUTTON_LIST_FIELD.get(screen);
            if (!(value instanceof List)) {
                return null;
            }
            for (Object element : (List) value) {
                if (!(element instanceof GuiButton)) {
                    continue;
                }
                GuiButton button = (GuiButton) element;
                if (button.visible && button.enabled
                        && mouseX >= button.xPosition && mouseX < button.xPosition + button.width
                        && mouseY >= button.yPosition && mouseY < button.yPosition + button.height) {
                    return button;
                }
            }
        } catch (Throwable ignored) {
            // Ignore a screen that is rebuilding its button list.
        }
        return null;
    }

    private static Field findButtonListField() {
        for (String name : new String[] {"buttonList", "field_146292_n"}) {
            try {
                Field field = GuiScreen.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // Try the SRG name next.
            }
        }
        return null;
    }

    private static boolean overSlot(GuiScreen screen, int mouseX, int mouseY) {
        for (Field field : slots(screen.getClass())) {
            try {
                Object value = field.get(screen);
                if (value instanceof GuiSlot) {
                    GuiSlot slot = (GuiSlot) value;
                    if (mouseX >= slot.left && mouseX < slot.left + slot.width
                            && mouseY >= slot.top && mouseY < slot.bottom) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                // See above.
            }
        }
        return false;
    }

    private static Field[] textFields(Class<?> type) {
        Field[] cached = TEXT_FIELD_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (GuiTextField.class.isAssignableFrom(field.getType())) {
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
        TEXT_FIELD_CACHE.put(type, result);
        return result;
    }

    private static Field[] slots(Class<?> type) {
        Field[] cached = SLOT_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> fields = new ArrayList<Field>();
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
        SLOT_CACHE.put(type, result);
        return result;
    }
}
