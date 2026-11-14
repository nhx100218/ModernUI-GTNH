package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuScreens;

/**
 * Gives menu search fields the white 26.3 border instead of vanilla's grey one.
 *
 * <p>The singleplayer search box comes from EFR; the mod list may use the same
 * {@link GuiTextField} path. Only the first background rectangle (the border)
 * is recoloured, the inner black field is untouched.</p>
 */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldSearchBorder {

    @Redirect(method = "drawTextBox",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V",
                    ordinal = 0),
            require = 0)
    private static void modernui$whiteSearchBorder(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, usesWhiteBorder() ? 0xFFFFFFFF : color);
    }

    private static boolean usesWhiteBorder() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return false;
        }
        GuiScreen screen = minecraft.currentScreen;
        if (screen instanceof GuiSelectWorld) {
            return true;
        }
        return screen != null
                && "cpw.mods.fml.client.GuiModList".equals(screen.getClass().getName())
                && ModernMenuScreens.usesSharedListBackdrop(screen);
    }
}
