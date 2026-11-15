package blocamlimb.nhx100218.modernui.uilib.mixin.early.etfuturum;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.enhance.client.DebugKeyHandler;

/**
 * Lets EFR's gamemode switcher follow ModernUI's remapped debug modifier.
 *
 * <p>EFR keeps its switcher open while the physical F3 key is down. Without this
 * redirect, opening it with a remapped modifier (the two bound keys) closes it
 * again on the first tick, which looks like a one-frame flash.</p>
 */
@Mixin(targets = "ganymedes01.etfuturum.client.gui.GuiGamemodeSwitcher", remap = false)
public abstract class MixinEfrGamemodeSwitcher {

    @Redirect(method = "checkForClose",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z"),
            remap = false, require = 0)
    private boolean modernui$keepOpenWithConfiguredModifier(int keyCode) {
        return DebugKeyHandler.isDebugModifierDown();
    }
}
