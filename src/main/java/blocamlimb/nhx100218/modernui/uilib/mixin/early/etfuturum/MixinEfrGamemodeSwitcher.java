package blocamlimb.nhx100218.modernui.uilib.mixin.early.etfuturum;

import net.minecraft.world.WorldSettings;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.client.DebugKeyHandler;

/**
 * Lets EFR's gamemode switcher follow ModernUI's remapped debug modifier.
 *
 * <p>EFR keeps its switcher open while the physical F3 key is down. Without the
 * redirect, opening it with a remapped modifier (the two bound keys) closes it
 * again on the first tick, which looks like a one-frame flash. The key handler
 * also lets the mapped gamemode key cycle the selection like F4 does.</p>
 */
@Mixin(targets = "ganymedes01.etfuturum.client.gui.GuiGamemodeSwitcher", remap = false)
public abstract class MixinEfrGamemodeSwitcher {

    @Shadow
    private boolean mouseUsedForSelection;

    @Shadow
    private WorldSettings.GameType gameMode;

    @Shadow
    private static WorldSettings.GameType nextGameType(WorldSettings.GameType type) {
        throw new AbstractMethodError();
    }

    @Redirect(method = "checkForClose",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z"),
            remap = false, require = 0)
    private boolean modernui$keepOpenWithConfiguredModifier(int keyCode) {
        return DebugKeyHandler.isDebugModifierDown();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true, require = 0)
    private void modernui$cycleWithMappedKey(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        int mapped = DebugKeyHandler.getGamemodeKeyCode(Keyboard.KEY_F4);
        if (mapped == Keyboard.KEY_F4 || keyCode != mapped) {
            return;
        }
        this.mouseUsedForSelection = false;
        this.gameMode = nextGameType(this.gameMode);
        callbackInfo.cancel();
    }
}
