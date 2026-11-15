package blocamlimb.nhx100218.modernui.enhance.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * Remappable debug actions for the 1.7.10 client.
 *
 * <p>Action bindings intentionally default to {@code KEY_NONE}; vanilla 1.7.10
 * already owns the hardcoded F3 combinations. Assigning a key here gives the
 * action a second, remappable trigger without toggling twice on the defaults.
 * The debug modifier itself defaults to F3.</p>
 */
public final class DebugKeyHandler {

    private static final String CATEGORY = "key.categories.modernui.debug";
    private static final int UNBOUND = Keyboard.KEY_NONE;

    private static final KeyBinding MODIFIER = new KeyBinding(
            "key.modernui.debug.modifier", Keyboard.KEY_F3, CATEGORY);
    private static final KeyBinding DEBUG_SCREEN = new KeyBinding(
            "key.modernui.debug.screen", UNBOUND, CATEGORY);
    private static final KeyBinding HITBOXES = new KeyBinding(
            "key.modernui.debug.hitboxes", UNBOUND, CATEGORY);
    private static final KeyBinding ADVANCED_TOOLTIPS = new KeyBinding(
            "key.modernui.debug.advancedTooltips", UNBOUND, CATEGORY);
    private static final KeyBinding RELOAD_CHUNKS = new KeyBinding(
            "key.modernui.debug.reloadChunks", UNBOUND, CATEGORY);
    private static final KeyBinding RENDER_DISTANCE = new KeyBinding(
            "key.modernui.debug.renderDistance", UNBOUND, CATEGORY);
    private static final KeyBinding CLEAR_CHAT = new KeyBinding(
            "key.modernui.debug.clearChat", UNBOUND, CATEGORY);
    private static final KeyBinding COPY_LOCATION = new KeyBinding(
            "key.modernui.debug.copyLocation", UNBOUND, CATEGORY);
    private static final KeyBinding PAUSE_FOCUS = new KeyBinding(
            "key.modernui.debug.pauseFocus", UNBOUND, CATEGORY);
    private static final KeyBinding PROFILER = new KeyBinding(
            "key.modernui.debug.profiler", UNBOUND, CATEGORY);
    private static final KeyBinding GAMEMODE_SWITCHER = new KeyBinding(
            "key.modernui.debug.gamemode", UNBOUND, CATEGORY);

    public DebugKeyHandler() {
    }

    public static void install() {
        ClientRegistry.registerKeyBinding(MODIFIER);
        ClientRegistry.registerKeyBinding(DEBUG_SCREEN);
        ClientRegistry.registerKeyBinding(HITBOXES);
        ClientRegistry.registerKeyBinding(ADVANCED_TOOLTIPS);
        ClientRegistry.registerKeyBinding(RELOAD_CHUNKS);
        ClientRegistry.registerKeyBinding(RENDER_DISTANCE);
        ClientRegistry.registerKeyBinding(CLEAR_CHAT);
        ClientRegistry.registerKeyBinding(COPY_LOCATION);
        ClientRegistry.registerKeyBinding(PAUSE_FOCUS);
        ClientRegistry.registerKeyBinding(PROFILER);
        ClientRegistry.registerKeyBinding(GAMEMODE_SWITCHER);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        if (DEBUG_SCREEN.isPressed()) {
            mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
            mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
        }

        if (!Keyboard.isKeyDown(MODIFIER.getKeyCode())) {
            return;
        }

        if (HITBOXES.isPressed()) {
            RenderManager.debugBoundingBox = !RenderManager.debugBoundingBox;
        }
        if (ADVANCED_TOOLTIPS.isPressed()) {
            mc.gameSettings.advancedItemTooltips = !mc.gameSettings.advancedItemTooltips;
            mc.gameSettings.saveOptions();
        }
        if (RELOAD_CHUNKS.isPressed() && mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
        if (RENDER_DISTANCE.isPressed()) {
            mc.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE,
                    GuiScreen.isShiftKeyDown() ? -1 : 1);
            if (mc.renderGlobal != null) {
                mc.renderGlobal.loadRenderers();
            }
        }
        if (CLEAR_CHAT.isPressed() && mc.ingameGUI != null) {
            mc.ingameGUI.getChatGUI().clearChatMessages();
        }
        if (COPY_LOCATION.isPressed() && mc.thePlayer != null) {
            GuiScreen.setClipboardString(String.format("%.2f, %.2f, %.2f",
                    Double.valueOf(mc.thePlayer.posX), Double.valueOf(mc.thePlayer.posY),
                    Double.valueOf(mc.thePlayer.posZ)));
        }
        if (PAUSE_FOCUS.isPressed()) {
            mc.gameSettings.pauseOnLostFocus = !mc.gameSettings.pauseOnLostFocus;
            mc.gameSettings.saveOptions();
        }
        if (PROFILER.isPressed()) {
            mc.gameSettings.showDebugInfo = true;
            mc.gameSettings.showDebugProfilerChart = !mc.gameSettings.showDebugProfilerChart;
        }
        if (GAMEMODE_SWITCHER.isPressed()) {
            openEfrGamemodeSwitcher(mc);
        }
    }

    private static void openEfrGamemodeSwitcher(Minecraft mc) {
        try {
            Class<?> type = Class.forName("ganymedes01.etfuturum.client.gui.GuiGamemodeSwitcher");
            if (mc.currentScreen != null && type.isInstance(mc.currentScreen)) {
                return;
            }
            mc.displayGuiScreen((GuiScreen) type.newInstance());
        } catch (Throwable ignored) {
            // EFR not installed or its debug switcher is disabled.
        }
    }
}
