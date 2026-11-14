package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.achievement.GuiAchievements;

/**
 * Screens that share ModernUI's panorama instead of drawing the vanilla dirt
 * background. Keeping the whitelist in one place lets the screen mixin and the
 * {@code GuiSlot} background mixin agree.
 */
public final class ModernMenuScreens {

    private static final String MODERN_CONFIG_SCREEN =
            "blocamlimb.nhx100218.modernui.uilib.config.modern.ModernConfigScreen";
    private static final String FORGE_MOD_CONFIG = "net.minecraftforge.client.gui.GuiModConfig";
    private static final String FORGE_MOD_LIST = "net.minecraftforge.client.gui.GuiModList";

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
                || screen instanceof GuiSnooper
                || screen instanceof GuiAchievements) {
            return true;
        }
        String name = screen.getClass().getName();
        return MODERN_CONFIG_SCREEN.equals(name)
                || FORGE_MOD_CONFIG.equals(name)
                || FORGE_MOD_LIST.equals(name);
    }

    /** The whitelist also controls list backgrounds; mod list screens use the same panorama. */
    public static boolean usesSharedListBackdrop(GuiScreen screen) {
        return usesSharedBackdrop(screen);
    }
}
