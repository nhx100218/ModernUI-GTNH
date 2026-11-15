package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.stream.GuiStreamOptions;

/**
 * Screens that share ModernUI's panorama instead of drawing the vanilla dirt
 * background. Keeping the whitelist in one place lets the screen mixin and the
 * {@code GuiSlot} background mixin agree.
 */
public final class ModernMenuScreens {

    private static final String MODERN_CONFIG_SCREEN =
            "blocamlimb.nhx100218.modernui.uilib.config.modern.ModernConfigScreen";
    private static final String FORGE_MOD_CONFIG = "net.minecraftforge.client.gui.GuiModConfig";
    /** 1.7.10 Forge is obfuscated into {@code cpw.mods.fml.client}, not net.minecraftforge. */
    private static final String FORGE_MOD_LIST = "cpw.mods.fml.client.GuiModList";
    /** Angelica replaces the vanilla video settings screen with this one. */
    private static final String ANGELICA_VIDEO_SCREEN =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.ReeseSodiumVideoOptionsScreen";
    /** Hodgepodge's enhanced sound options page. */
    private static final String HODGEPODGE_SOUND_SCREEN =
            "com.mitchej123.hodgepodge.client.gui.HodgepodgeSoundOptionsGui";
    private static final String STREAM_UNAVAILABLE_SCREEN =
            "net.minecraft.client.gui.stream.GuiStreamUnavailable";

    private ModernMenuScreens() {
    }

    /**
     * Whether the screen is one of the menu screens using the shared backdrop.
     *
     * @param screen current screen, may be null
     * @return true when the shared panorama should replace the screen background
     */
    public static boolean usesSharedBackdrop(GuiScreen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof GuiSelectWorld
                || screen instanceof GuiMultiplayer
                || screen instanceof GuiCreateWorld
                || screen instanceof GuiScreenResourcePacks
                || screen instanceof GuiOptions
                || screen instanceof GuiVideoSettings
                || screen instanceof GuiControls
                || screen instanceof GuiLanguage
                || screen instanceof GuiScreenOptionsSounds
                || screen instanceof ScreenChatOptions
                || screen instanceof GuiStreamOptions
                || screen instanceof GuiSnooper
                || screen instanceof GuiAchievements) {
            return true;
        }
        String name = screen.getClass().getName();
        return MODERN_CONFIG_SCREEN.equals(name)
                || FORGE_MOD_CONFIG.equals(name)
                || FORGE_MOD_LIST.equals(name)
                || ANGELICA_VIDEO_SCREEN.equals(name)
                || HODGEPODGE_SOUND_SCREEN.equals(name)
                || STREAM_UNAVAILABLE_SCREEN.equals(name);
    }

    /** The whitelist also controls list backgrounds; mod list screens use the same panorama. */
    public static boolean usesSharedListBackdrop(GuiScreen screen) {
        return usesSharedBackdrop(screen);
    }

    /**
     * Sub-screens without a {@code GuiSlot} use the shared header/footer frame.
     *
     * @param screen current screen
     * @return true when the screen should receive the generic 26.3 frame
     */
    public static boolean usesMenuFrame(GuiScreen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof GuiScreenOptionsSounds
                || screen instanceof ScreenChatOptions
                || screen instanceof GuiStreamOptions) {
            return true;
        }
        String name = screen.getClass().getName();
        return HODGEPODGE_SOUND_SCREEN.equals(name)
                || STREAM_UNAVAILABLE_SCREEN.equals(name);
    }

    /**
     * Whether list decorations (scrim and separators) should be drawn.
     * Angelica's video page supplies its own surface, so it keeps the shared
     * panorama background but gets no extra scrim or lines.
     *
     * @param screen current screen
     * @return true when the list should receive ModernUI decorations
     */
    public static boolean usesListScrim(GuiScreen screen) {
        return usesSharedListBackdrop(screen)
                && !ANGELICA_VIDEO_SCREEN.equals(screen.getClass().getName());
    }
}
