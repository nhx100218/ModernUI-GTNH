package blocamlimb.nhx100218.modernui.uilib.mixin.early.etfuturum;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.gameevent.InputEvent;

import blocamlimb.nhx100218.modernui.enhance.client.DebugKeyHandler;

/**
 * Makes EFR's "new F3 behavior" cooperate with remapped debug actions.
 *
 * <p>EFR reverts debug-screen changes made while F3 is held, and toggles the
 * debug screen again when F3 is released. A debug-screen action bound to a key
 * other than F3 therefore either flashed or toggled twice. A one-tick grace
 * window covers the revert; the release toggle is skipped once this mod already
 * performed the toggle.</p>
 */
@Mixin(targets = "ganymedes01.etfuturum.core.handlers.ClientEventHandler", remap = false)
public abstract class MixinEfrDebugKeys {

    /**
     * {@code onClientTick} calls {@code Keyboard.isKeyDown} three times:
     * F3 (gamemode), F4 (gamemode), F3 (debug revert). Only the last one matters.
     */
    @Redirect(method = "onClientTick",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z", ordinal = 2),
            remap = false, require = 0)
    private boolean modernui$skipDebugScreenRevert(int keyCode) {
        if (DebugKeyHandler.consumeDebugScreenGrace()) {
            return false;
        }
        return Keyboard.isKeyDown(keyCode);
    }

    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void modernui$skipDuplicateF3Toggle(InputEvent.KeyInputEvent event, CallbackInfo callbackInfo) {
        if (DebugKeyHandler.shouldSuppressEfrF3KeyEvent()) {
            callbackInfo.cancel();
        }
    }
}
