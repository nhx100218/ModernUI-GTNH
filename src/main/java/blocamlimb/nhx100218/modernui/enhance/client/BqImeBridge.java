package blocamlimb.nhx100218.modernui.enhance.client;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import blocamlimb.nhx100218.modernui.enhance.MyMod;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.PanelTextField;
import net.minecraft.client.Minecraft;

/**
 * Bridges BQ's {@code PanelTextField} text input to lwjgl3ify's IME events.
 *
 * <p>lwjgl3ify is optional. All API access is reflective so the class can load
 * and run when lwjgl3ify is absent.</p>
 */
public final class BqImeBridge {

    private static final String INPUT_EVENTS = "me.eigenraven.lwjgl3ify.api.InputEvents";

    private static WeakReference<PanelTextField<?>> focused = new WeakReference<PanelTextField<?>>(null);
    private static boolean hooked;
    private static boolean hookReady;
    private static boolean begun;

    private static Class<?> textEventClass;
    private static Method beginTextInput;
    private static Method endTextInput;

    private BqImeBridge() {}

    /** Records the currently focused BQ text field. */
    public static void trackFocus(PanelTextField<?> field) {
        if (field != null && field.isFocused()) {
            focused = new WeakReference<PanelTextField<?>>(field);
        }
    }

    /** Starts text input mode while a BQ screen is open. */
    public static void begin() {
        if (!ensureHook() || begun) {
            return;
        }
        try {
            beginTextInput.invoke(null);
            begun = true;
        } catch (Throwable t) {
            MyMod.LOG.warn("[BQ] failed to begin IME input", t);
        }
    }

    /** Ends text input mode when the BQ screen closes. */
    public static void end() {
        if (!begun) {
            return;
        }
        begun = false;
        focused = new WeakReference<PanelTextField<?>>(null);
        try {
            endTextInput.invoke(null);
        } catch (Throwable t) {
            MyMod.LOG.warn("[BQ] failed to end IME input", t);
        }
    }

    private static synchronized boolean ensureHook() {
        if (hooked) {
            return hookReady;
        }
        hooked = true;
        try {
            Class<?> inputEventsClass = Class.forName(INPUT_EVENTS);
            Class<?> keyboardListenerClass = Class.forName(INPUT_EVENTS + "$KeyboardListener");
            textEventClass = Class.forName(INPUT_EVENTS + "$TextEvent");
            beginTextInput = inputEventsClass.getMethod("beginTextInput");
            endTextInput = inputEventsClass.getMethod("endTextInput");
            Method addKeyboardListener = inputEventsClass.getMethod("addKeyboardListener", keyboardListenerClass);

            Object listener = Proxy.newProxyInstance(
                    keyboardListenerClass.getClassLoader(),
                    new Class<?>[] { keyboardListenerClass },
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("onTextEvent".equals(name)) {
                                Object event = args == null || args.length == 0 ? null : args[0];
                                deliver(readText(event));
                                return null;
                            }
                            if ("equals".equals(name)) {
                                return Boolean.valueOf(args != null && args.length == 1 && proxy == args[0]);
                            }
                            if ("hashCode".equals(name)) {
                                return Integer.valueOf(System.identityHashCode(proxy));
                            }
                            if ("toString".equals(name)) {
                                return "BqImeBridgeListener";
                            }
                            return null;
                        }
                    });
            addKeyboardListener.invoke(null, listener);
            hookReady = true;
        } catch (Throwable t) {
            hookReady = false;
            MyMod.LOG.warn("[BQ] failed to register IME listener; lwjgl3ify is probably absent", t);
        }
        return hookReady;
    }

    private static String readText(Object event) {
        if (event == null || textEventClass == null) {
            return null;
        }
        try {
            Field field = textEventClass.getField("text");
            return (String) field.get(event);
        } catch (Throwable ignored) {
            try {
                Field field = textEventClass.getDeclaredField("text");
                field.setAccessible(true);
                return (String) field.get(event);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static void deliver(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        // Only bridge characters that BQ's onKeyTyped would reject; ASCII is already handled there.
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

    /** Returns true when the text contains a character BQ's onKeyTyped path will not write. */
    private static boolean modernui$needsBridge(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
