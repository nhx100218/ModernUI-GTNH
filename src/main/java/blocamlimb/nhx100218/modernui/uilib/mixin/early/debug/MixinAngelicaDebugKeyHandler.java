package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizons.angelica.mixins.interfaces.IGameSettingsExt;

import blocamlimb.nhx100218.modernui.enhance.client.DebugKeyHandler;

/**
 * Keeps Angelica's FPS graph in the state ModernUI just toggled.
 *
 * <p>Angelica writes {@code showFpsGraph = altPressedAtF3} when F3 is released.
 * If the graph was toggled through a remapped debug key, that write would undo
 * the toggle, so the first write after a ModernUI toggle is skipped.</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.DebugKeyHandler", remap = false)
public abstract class MixinAngelicaDebugKeyHandler {

    @Redirect(method = "onKey",
            at = @At(value = "INVOKE",
                    target = "Lcom/gtnewhorizons/angelica/mixins/interfaces/IGameSettingsExt;"
                            + "angelica$setShowFpsGraph(Z)V"),
            remap = false, require = 0)
    private void modernui$keepToggledFpsGraph(IGameSettingsExt settings, boolean value) {
        if (DebugKeyHandler.consumeAngelicaFpsGraphOverride()) {
            return;
        }
        settings.angelica$setShowFpsGraph(value);
    }
}
