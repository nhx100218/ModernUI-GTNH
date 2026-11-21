package blocamlimb.nhx100218.modernui.enhance.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.client.GuiScrollingList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;

import blocamlimb.nhx100218.modernui.enhance.command.ChatCommandSuggestions;
import blocamlimb.nhx100218.modernui.uilib.ui.base.props.UiCursor;
import blocamlimb.nhx100218.modernui.uilib.ui.host.SystemUiCursorHost;
import blocamlimb.nhx100218.modernui.uilib.ui.screen.McScreenBridge;

/**
 * Maps 1.7.10 widgets to the 26.3 mouse cursors.
 *
 * <p>Buttons, clickable list rows and sliders use the pointer cursor, text
 * fields use the I-beam. The actual native cursor is applied through
 * ModernUI's existing cross-backend cursor host, so SDL/Metal and GL paths
 * share one implementation.</p>
 *
 * <p>The host itself is the single cache of the applied cursor. This class must
 * not keep its own "last cursor" field: ModernUI scenes write the host directly
 * through {@code SceneCursor}, and a second cache would make the two writers
 * disagree (stuck hand or stuck arrow after changing screens).</p>
 */
public final class MenuCursor {

    /** Angelica replaces the vanilla video settings screen with this one. */
    private static final String ANGELICA_VIDEO_SCREEN =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.ReeseSodiumVideoOptionsScreen";
    private static final String ANGELICA_FRAME =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame";
    private static final String ANGELICA_TAB_FRAME =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame";
    private static final String ANGELICA_CONTROL =
            "me.jellysquid.mods.sodium.client.gui.options.control.ControlElement";
    private static final String ANGELICA_FLAT_BUTTON =
            "me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget";
    private static final String ANGELICA_SCROLL_BAR =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent";

    private static final Map<Class<?>, Field[]> TEXT_FIELD_CACHE = new ConcurrentHashMap<Class<?>, Field[]>();
    private static final Map<Class<?>, Field[]> LIST_FIELD_CACHE = new ConcurrentHashMap<Class<?>, Field[]>();
    private static final Map<String, Field> NAMED_FIELD_CACHE = new ConcurrentHashMap<String, Field>();
    private static final Map<String, Method> NAMED_METHOD_CACHE = new ConcurrentHashMap<String, Method>();
    private static final Map<Class<?>, Method> MOUSE_OVER_CACHE = new ConcurrentHashMap<Class<?>, Method>();
    private static final Map<Class<?>, Method> CONTAINS_CURSOR_CACHE = new ConcurrentHashMap<Class<?>, Method>();

    private static final Field BUTTON_LIST_FIELD = findButtonListField();

    private static final Class<?> ANGELICA_FRAME_CLASS = optionalClass(ANGELICA_FRAME);
    private static final Class<?> ANGELICA_TAB_FRAME_CLASS = optionalClass(ANGELICA_TAB_FRAME);
    private static final Class<?> ANGELICA_CONTROL_CLASS = optionalClass(ANGELICA_CONTROL);
    private static final Class<?> ANGELICA_FLAT_BUTTON_CLASS = optionalClass(ANGELICA_FLAT_BUTTON);

    private MenuCursor() {
    }

    /** Recomputes and applies the cursor for the current screen. */
    public static void update(GuiScreen screen, int mouseX, int mouseY) {
        if (screen == null) {
            apply(UiCursor.DEFAULT);
            return;
        }
        // ModernUI scenes own their cursor through SceneCursor/CursorBackend.
        if (screen instanceof McScreenBridge) {
            return;
        }
        if (ANGELICA_VIDEO_SCREEN.equals(screen.getClass().getName())) {
            apply(angelicaCursor(screen, mouseX, mouseY));
            return;
        }

        if (screen instanceof net.minecraft.client.gui.GuiChat
                && ChatCommandSuggestions.isPopupHovered(mouseX, mouseY)) {
            apply(UiCursor.POINTER);
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
        if (overClickableListRow(screen, mouseX, mouseY)) {
            apply(UiCursor.POINTER);
            return;
        }
        apply(UiCursor.DEFAULT);
    }

    public static void reset() {
        apply(UiCursor.DEFAULT);
    }

    private static void apply(UiCursor cursor) {
        // No local de-duplication: SystemUiCursorHost already skips same-value
        // applications, and its state is the only one shared with SceneCursor.
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
                        && button.width <= screen.width - 8 && button.height <= 40
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

    /**
     * 26.3 shows the hand only over the world/server icon (the quick-join
     * affordance); the text and the selection outline stay on the arrow cursor.
     * The mod list has no per-row icon, so its clickable rows use the hand.
     */
    private static boolean overClickableListRow(GuiScreen screen, int mouseX, int mouseY) {
        if (screen instanceof GuiSelectWorld || screen instanceof GuiMultiplayer) {
            for (Field field : listFields(screen.getClass())) {
                try {
                    Object value = field.get(screen);
                    if (value instanceof GuiSlot && overListIcon((GuiSlot) value, mouseX, mouseY)) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    // Ignore a list that is being rebuilt.
                }
            }
            return false;
        }
        if ("cpw.mods.fml.client.GuiModList".equals(screen.getClass().getName())) {
            for (Field field : listFields(screen.getClass())) {
                try {
                    Object value = field.get(screen);
                    if (value instanceof GuiScrollingList) {
                        GuiScrollingList list = (GuiScrollingList) value;
                        if (list.func_27256_c(mouseX, mouseY) >= 0
                                || overScrollingListGeometry(list, mouseX, mouseY)) {
                            return true;
                        }
                    }
                } catch (Throwable ignored) {
                    // Ignore a list that is being rebuilt.
                }
            }
        }
        return false;
    }

    private static final int LIST_ICON_SIZE = 32;

    /** Hand only over the 32x32 world/server icon of the row below the mouse. */
    private static boolean overListIcon(GuiSlot slot, int mouseX, int mouseY) {
        if (slot.slotHeight <= 0 || mouseY < slot.top || mouseY >= slot.bottom) {
            return false;
        }
        int contentHeight = slot.func_148135_f() + (slot.bottom - slot.top - 4);
        if (contentHeight <= 0) {
            return false;
        }
        int rows = contentHeight / slot.slotHeight;
        int y = slot.top + 4 - slot.getAmountScrolled() + slot.headerPadding;
        int row = (mouseY - y) / slot.slotHeight;
        if (row < 0 || row >= rows || mouseY < y + row * slot.slotHeight) {
            return false;
        }
        int iconX = slot.left + slot.width / 2 - slot.getListWidth() / 2 + 2;
        int iconY = y + row * slot.slotHeight;
        return mouseX >= iconX && mouseX < iconX + LIST_ICON_SIZE
                && mouseY >= iconY && mouseY < iconY + LIST_ICON_SIZE;
    }

    /** Same row geometry for Forge's {@link GuiScrollingList} (mod list). */
    private static boolean overScrollingListGeometry(GuiScrollingList list, int mouseX, int mouseY) {
        int left = intField(list, "left", -1);
        int top = intField(list, "top", -1);
        int bottom = intField(list, "bottom", -1);
        int width = intField(list, "listWidth", -1);
        int slotHeight = intField(list, "slotHeight", 0);
        if (left < 0 || top < 0 || bottom < 0 || width <= 0 || slotHeight <= 0) {
            return false;
        }
        if (mouseY < top || mouseY >= bottom) {
            return false;
        }
        if (mouseX < left - 2 || mouseX > left + width + 2) {
            return false;
        }
        float scroll = floatField(list, "scrollDistance", 0.0F);
        int header = intField(list, "field_27261_r", 0);
        int y = mouseY - top - header + (int) scroll - 4;
        if (y < 0) {
            return false;
        }
        Integer size = intMethodValue(list, "getSize");
        return size == null || y / slotHeight < size.intValue();
    }

    private static int intField(Object owner, String name, int fallback) {
        Object value = namedFieldValue(owner, name);
        return value instanceof Integer ? ((Integer) value).intValue() : fallback;
    }

    private static float floatField(Object owner, String name, float fallback) {
        Object value = namedFieldValue(owner, name);
        return value instanceof Float ? ((Float) value).floatValue() : fallback;
    }

    private static Integer intMethodValue(Object owner, String name) {
        Object value = namedMethodValue(owner, name);
        return value instanceof Integer ? (Integer) value : null;
    }

    // ------------------------------------------------------------------
    // Angelica (Reese's Sodium Options) video settings
    // ------------------------------------------------------------------

    private static UiCursor angelicaCursor(GuiScreen screen, int mouseX, int mouseY) {
        Object search = namedFieldValue(screen, "searchTextField");
        if (search != null && mouseOver(search, mouseX, mouseY)) {
            return UiCursor.TEXT;
        }
        for (String name : new String[] {"applyButton", "undoButton", "closeButton"}) {
            Object button = namedFieldValue(screen, name);
            if (button != null && flatButtonHovered(button, mouseX, mouseY)) {
                return UiCursor.POINTER;
            }
        }
        Object frame = namedFieldValue(screen, "frame");
        if (frame != null && angelicaHit(frame, mouseX, mouseY, 0)) {
            return UiCursor.POINTER;
        }
        return UiCursor.DEFAULT;
    }

    private static boolean angelicaHit(Object node, int mouseX, int mouseY, int depth) {
        if (node == null || depth > 8) {
            return false;
        }
        if (ANGELICA_SCROLL_BAR.equals(node.getClass().getName())) {
            return false;
        }
        if (ANGELICA_FRAME_CLASS != null && ANGELICA_FRAME_CLASS.isInstance(node)) {
            if (ANGELICA_TAB_FRAME_CLASS != null && ANGELICA_TAB_FRAME_CLASS.isInstance(node)) {
                Object tabSection = namedFieldValue(node, "tabSection");
                if (tabSection != null && containsCursor(tabSection, mouseX, mouseY)) {
                    return true;
                }
            }
            Object children = namedMethodValue(node, "children");
            if (children instanceof Iterable) {
                for (Object child : (Iterable<?>) children) {
                    if (angelicaHit(child, mouseX, mouseY, depth + 1)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (ANGELICA_CONTROL_CLASS != null && ANGELICA_CONTROL_CLASS.isInstance(node)) {
            Object dimensions = namedMethodValue(node, "getDimensions");
            return dimensions != null && containsCursor(dimensions, mouseX, mouseY);
        }
        if (ANGELICA_FLAT_BUTTON_CLASS != null && ANGELICA_FLAT_BUTTON_CLASS.isInstance(node)) {
            return flatButtonHovered(node, mouseX, mouseY);
        }
        return mouseOver(node, mouseX, mouseY);
    }

    private static boolean flatButtonHovered(Object button, int mouseX, int mouseY) {
        Object visible = namedFieldValue(button, "visible");
        Object enabled = namedFieldValue(button, "enabled");
        if (visible instanceof Boolean && !((Boolean) visible).booleanValue()) {
            return false;
        }
        if (enabled instanceof Boolean && !((Boolean) enabled).booleanValue()) {
            return false;
        }
        Object dim = namedFieldValue(button, "dim");
        return dim != null && containsCursor(dim, mouseX, mouseY);
    }

    private static boolean mouseOver(Object element, int mouseX, int mouseY) {
        Method method = MOUSE_OVER_CACHE.get(element.getClass());
        if (method == null) {
            try {
                method = element.getClass().getMethod("isMouseOver", Double.TYPE, Double.TYPE);
            } catch (Throwable ignored) {
                method = MissingMethod.INSTANCE;
            }
            MOUSE_OVER_CACHE.put(element.getClass(), method);
        }
        if (method == MissingMethod.INSTANCE) {
            return false;
        }
        try {
            Object result = method.invoke(element, Double.valueOf(mouseX), Double.valueOf(mouseY));
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean containsCursor(Object dimensions, int mouseX, int mouseY) {
        Method method = CONTAINS_CURSOR_CACHE.get(dimensions.getClass());
        if (method == null) {
            try {
                method = dimensions.getClass().getMethod("containsCursor", Double.TYPE, Double.TYPE);
            } catch (Throwable ignored) {
                method = MissingMethod.INSTANCE;
            }
            CONTAINS_CURSOR_CACHE.put(dimensions.getClass(), method);
        }
        if (method == MissingMethod.INSTANCE) {
            return false;
        }
        try {
            Object result = method.invoke(dimensions, Double.valueOf(mouseX), Double.valueOf(mouseY));
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Sentinel used to cache "this class has no such method" without null lookups. */
    private static final class MissingMethod {

        private static final Method INSTANCE = missing();

        private static Method missing() {
            try {
                return MissingMethod.class.getDeclaredMethod("noop");
            } catch (Throwable ignored) {
                return null;
            }
        }

        @SuppressWarnings("unused")
        private void noop() {
        }
    }

    private static Object namedFieldValue(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        String key = owner.getClass().getName() + "#" + name;
        Field field = NAMED_FIELD_CACHE.get(key);
        if (field == null) {
            field = findField(owner.getClass(), name);
            NAMED_FIELD_CACHE.put(key, field == null ? MissingField.INSTANCE : field);
        }
        if (field == MissingField.INSTANCE) {
            return null;
        }
        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object namedMethodValue(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        String key = owner.getClass().getName() + "#" + name;
        Method method = NAMED_METHOD_CACHE.get(key);
        if (method == null) {
            try {
                method = owner.getClass().getMethod(name);
            } catch (Throwable ignored) {
                try {
                    method = owner.getClass().getDeclaredMethod(name);
                    method.setAccessible(true);
                } catch (Throwable ignoredAgain) {
                    method = MissingMethod.INSTANCE;
                }
            }
            NAMED_METHOD_CACHE.put(key, method);
        }
        if (method == MissingMethod.INSTANCE) {
            return null;
        }
        try {
            return method.invoke(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** Placeholder so the named-field cache can distinguish "missing" from "not cached yet". */
    private static final class MissingField {

        private static final Field INSTANCE = missing();

        private static Field missing() {
            try {
                return MissingField.class.getDeclaredField("INSTANCE");
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static Field findButtonListField() {
        Field field = findField(GuiScreen.class, "buttonList");
        return field != null ? field : findField(GuiScreen.class, "field_146292_n");
    }

    private static Class<?> optionalClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
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

    private static Field[] listFields(Class<?> type) {
        Field[] cached = LIST_FIELD_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (GuiSlot.class.isAssignableFrom(field.getType())
                        || GuiScrollingList.class.isAssignableFrom(field.getType())) {
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
        LIST_FIELD_CACHE.put(type, result);
        return result;
    }
}
