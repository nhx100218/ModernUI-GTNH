package blocamlimb.nhx100218.modernui.enhance.client;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/**
 * Remappable 26.3-style debug actions for the 1.7.10 client.
 *
 * <p>Every action is an ordinary key binding in the "ModernUI Debug" category.
 * The debug modifier defaults to F3 and an action fires when the modifier and
 * the bound key are held together, mirroring modern Minecraft.</p>
 *
 * <p>The action keys default to the letters vanilla 1.7.10 already uses
 * (B/H/A/F/P) and F3 for the debug screen. Those combinations are detected as
 * "already handled by vanilla", so pressing them does not toggle twice: vanilla
 * performs the action once. Rebinding an action to any other key makes this
 * handler perform it instead.</p>
 */
public final class DebugKeyHandler {

    private static final String CATEGORY = "key.categories.modernui.debug";

    private static final KeyBinding MODIFIER = new KeyBinding(
            "key.modernui.debug.modifier", Keyboard.KEY_F3, CATEGORY);
    /** Vanilla F3 owns the debug screen until this binding is assigned a key. */
    private static final KeyBinding DEBUG_SCREEN = new KeyBinding(
            "key.modernui.debug.screen", Keyboard.KEY_NONE, CATEGORY);
    private static final KeyBinding HITBOXES = new KeyBinding(
            "key.modernui.debug.hitboxes", Keyboard.KEY_B, CATEGORY);
    private static final KeyBinding ADVANCED_TOOLTIPS = new KeyBinding(
            "key.modernui.debug.advancedTooltips", Keyboard.KEY_H, CATEGORY);
    private static final KeyBinding RELOAD_CHUNKS = new KeyBinding(
            "key.modernui.debug.reloadChunks", Keyboard.KEY_A, CATEGORY);
    private static final KeyBinding RENDER_DISTANCE = new KeyBinding(
            "key.modernui.debug.renderDistance", Keyboard.KEY_F, CATEGORY);
    private static final KeyBinding CLEAR_CHAT = new KeyBinding(
            "key.modernui.debug.clearChat", Keyboard.KEY_D, CATEGORY);
    /** F3+C is the vanilla "crash the game" combo in 1.7.10, so it starts unbound. */
    private static final KeyBinding COPY_LOCATION = new KeyBinding(
            "key.modernui.debug.copyLocation", Keyboard.KEY_NONE, CATEGORY);
    private static final KeyBinding PAUSE_FOCUS = new KeyBinding(
            "key.modernui.debug.pauseFocus", Keyboard.KEY_P, CATEGORY);
    private static final KeyBinding PROFILER = new KeyBinding(
            "key.modernui.debug.profiler", Keyboard.KEY_L, CATEGORY);
    private static final KeyBinding GAMEMODE_SWITCHER = new KeyBinding(
            "key.modernui.debug.gamemode", Keyboard.KEY_F4, CATEGORY);
    private static final KeyBinding FPS_GRAPH = new KeyBinding(
            "key.modernui.debug.fpsGraph", Keyboard.KEY_LMENU, CATEGORY);

    private static final KeyBinding[] ACTIONS = new KeyBinding[] {
            DEBUG_SCREEN,
            HITBOXES,
            ADVANCED_TOOLTIPS,
            RELOAD_CHUNKS,
            RENDER_DISTANCE,
            CLEAR_CHAT,
            COPY_LOCATION,
            PAUSE_FOCUS,
            PROFILER,
            GAMEMODE_SWITCHER,
            FPS_GRAPH
    };

    /** Vanilla 1.7.10 handles these keys itself while physical F3 is held. */
    private static final int[] VANILLA_F3_KEYS = new int[] {
            Keyboard.KEY_A,
            Keyboard.KEY_B,
            Keyboard.KEY_H,
            Keyboard.KEY_F,
            Keyboard.KEY_P,
            Keyboard.KEY_F3
    };

    private final boolean[] wasDown = new boolean[ACTIONS.length];
    private boolean modifierWasDown;
    private boolean actionFiredWithModifier;

    /** Set after this handler toggles the debug screen itself (remapped key). */
    private static volatile boolean suppressEfrF3Release;
    /** Number of EFR ticks that must not revert our debug-screen toggle. */
    private static volatile int debugScreenGraceTicks;
    /** Set after this handler toggled Angelica's FPS graph. */
    private static volatile boolean suppressAngelicaFpsGraph;

    public DebugKeyHandler() {
    }

    public static void install() {
        for (KeyBinding binding : new KeyBinding[] {
                MODIFIER, DEBUG_SCREEN, HITBOXES, ADVANCED_TOOLTIPS, RELOAD_CHUNKS,
                RENDER_DISTANCE, CLEAR_CHAT, COPY_LOCATION, PAUSE_FOCUS, PROFILER,
                GAMEMODE_SWITCHER, FPS_GRAPH }) {
            ClientRegistry.registerKeyBinding(binding);
        }
    }

    /** Whether the configured debug modifier is currently held (F3 when unbound). */
    public static boolean isDebugModifierDown() {
        int keyCode = MODIFIER.getKeyCode();
        return Keyboard.isKeyDown(keyCode == Keyboard.KEY_NONE ? Keyboard.KEY_F3 : keyCode);
    }

    /**
     * Consumed by EFR's tick handler so its "new F3 behavior" does not revert a
     * debug-screen toggle performed through a remapped key.
     */
    public static boolean consumeDebugScreenGrace() {
        if (debugScreenGraceTicks <= 0) {
            return false;
        }
        debugScreenGraceTicks = 0;
        return true;
    }

    /**
     * Consumed by EFR's key handler: after we toggled the debug screen with a
     * remapped key, EFR must not toggle it again when F3 is released.
     */
    public static boolean shouldSuppressEfrF3KeyEvent() {
        if (!suppressEfrF3Release) {
            return false;
        }
        if (Keyboard.getEventKey() == Keyboard.KEY_F3 && !Keyboard.getEventKeyState()) {
            suppressEfrF3Release = false;
            return true;
        }
        return false;
    }

    /**
     * Key that cycles gamemodes inside EFR's switcher. Falls back to F4 when the
     * action is unbound.
     */
    public static int getGamemodeKeyCode(int fallback) {
        int keyCode = GAMEMODE_SWITCHER.getKeyCode();
        return keyCode == Keyboard.KEY_NONE ? fallback : keyCode;
    }

    /**
     * Consumed by Angelica's F3 release handler: do not overwrite the FPS graph
     * state that this mod just toggled through a remapped key.
     */
    public static boolean consumeAngelicaFpsGraphOverride() {
        if (!suppressAngelicaFpsGraph) {
            return false;
        }
        suppressAngelicaFpsGraph = false;
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        boolean active = mc.thePlayer != null && mc.currentScreen == null;
        boolean modifier = isDebugModifierDown();
        boolean modifierPressed = modifier && !modifierWasDown;
        boolean modifierReleased = !modifier && modifierWasDown;
        modifierWasDown = modifier;
        if (modifierPressed) {
            actionFiredWithModifier = false;
        }

        for (int i = 0; i < ACTIONS.length; i++) {
            KeyBinding binding = ACTIONS[i];
            int keyCode = binding.getKeyCode();
            boolean down = active && keyCode != Keyboard.KEY_NONE && Keyboard.isKeyDown(keyCode);
            boolean pressed = down && !wasDown[i];
            wasDown[i] = down;
            if (!pressed) {
                continue;
            }
            // The debug screen is a standalone remap (26.3 lets it live on any
            // key); every other action stays behind the debug modifier.
            boolean needsModifier = i != 0;
            if (needsModifier && !modifier) {
                continue;
            }
            if (vanillaHandles(keyCode)) {
                if (needsModifier) {
                    actionFiredWithModifier = true;
                }
                continue;
            }
            perform(i, mc);
            if (needsModifier) {
                actionFiredWithModifier = true;
            }
        }

        // A remapped debug modifier pressed on its own opens the debug screen,
        // just like F3 does in vanilla 1.7.10.
        if (active && modifierReleased && MODIFIER.getKeyCode() != Keyboard.KEY_F3
                && !actionFiredWithModifier) {
            toggleDebugScreen(mc);
        }
    }

    private static boolean vanillaHandles(int keyCode) {
        // Vanilla only reacts to the physical F3 key, regardless of our binding.
        if (MODIFIER.getKeyCode() != Keyboard.KEY_F3 || !Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            return false;
        }
        for (int vanillaKey : VANILLA_F3_KEYS) {
            if (keyCode == vanillaKey) {
                return true;
            }
        }
        return false;
    }

    private void perform(int action, Minecraft mc) {
        switch (action) {
            case 0:
                toggleDebugScreen(mc);
                break;
            case 1:
                RenderManager.debugBoundingBox = !RenderManager.debugBoundingBox;
                break;
            case 2:
                mc.gameSettings.advancedItemTooltips = !mc.gameSettings.advancedItemTooltips;
                mc.gameSettings.saveOptions();
                break;
            case 3:
                if (mc.renderGlobal != null) {
                    mc.renderGlobal.loadRenderers();
                }
                break;
            case 4:
                mc.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE,
                        GuiScreen.isShiftKeyDown() ? -1 : 1);
                if (mc.renderGlobal != null) {
                    mc.renderGlobal.loadRenderers();
                }
                break;
            case 5:
                if (mc.ingameGUI != null) {
                    mc.ingameGUI.getChatGUI().clearChatMessages();
                }
                break;
            case 6:
                if (mc.thePlayer != null) {
                    GuiScreen.setClipboardString(String.format("%.2f, %.2f, %.2f",
                            Double.valueOf(mc.thePlayer.posX), Double.valueOf(mc.thePlayer.posY),
                            Double.valueOf(mc.thePlayer.posZ)));
                }
                break;
            case 7:
                mc.gameSettings.pauseOnLostFocus = !mc.gameSettings.pauseOnLostFocus;
                mc.gameSettings.saveOptions();
                break;
            case 8:
                mc.gameSettings.showDebugInfo = true;
                mc.gameSettings.showDebugProfilerChart = !mc.gameSettings.showDebugProfilerChart;
                markDebugScreenRemapped();
                break;            case 9:
                openEfrGamemodeSwitcher(mc);
                break;
            case 10:
                toggleAngelicaFpsGraph(mc);
                break;
            default:
                break;
        }
    }

    private static void toggleDebugScreen(Minecraft mc) {
        mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
        mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
        markDebugScreenRemapped();
    }

    private static void markDebugScreenRemapped() {
        // EFR only rewrites the debug screen while the physical F3 key is held,
        // so only that case needs the grace window and the release suppression.
        if (Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            suppressEfrF3Release = true;
            debugScreenGraceTicks = 1;
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

    private static void toggleAngelicaFpsGraph(Minecraft mc) {
        try {
            Class<?> ext = Class.forName("com.gtnewhorizons.angelica.mixins.interfaces.IGameSettingsExt");
            if (!ext.isInstance(mc.gameSettings)) {
                return;
            }
            boolean current = ((Boolean) ext.getMethod("angelica$showFpsGraph").invoke(mc.gameSettings))
                    .booleanValue();
            ext.getMethod("angelica$setShowFpsGraph", Boolean.TYPE)
                    .invoke(mc.gameSettings, Boolean.valueOf(!current));
            if (Keyboard.isKeyDown(Keyboard.KEY_F3)) {
                suppressAngelicaFpsGraph = true;
            }
        } catch (Throwable ignored) {
            // Angelica not installed or the FPS graph is unavailable.
        }
    }
}
