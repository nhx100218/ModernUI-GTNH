package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.stream.GuiStreamUnavailable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lifts the Twitch-unavailable buttons above the 26.3 footer separator.
 * Vanilla places them at {@code height - 50}, so the separator cuts through
 * their lower half; the text itself already sits in the right area.
 */
@Mixin(GuiStreamUnavailable.class)
public abstract class MixinGuiStreamUnavailableLayout {

    @Shadow
    protected List buttonList;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernui$liftButtons(CallbackInfo callbackInfo) {
        GuiStreamUnavailable screen = (GuiStreamUnavailable) (Object) this;
        int y = screen.height - 72;
        for (Object value : buttonList) {
            if (value instanceof GuiButton) {
                ((GuiButton) value).yPosition = y;
            }
        }
    }
}
