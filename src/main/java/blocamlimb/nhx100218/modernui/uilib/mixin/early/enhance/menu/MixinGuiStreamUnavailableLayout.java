package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.stream.GuiStreamUnavailable;

import org.spongepowered.asm.mixin.Mixin;
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

    private static final Field BUTTON_LIST = findButtonListField();

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernui$liftButtons(CallbackInfo callbackInfo) {
        GuiStreamUnavailable screen = (GuiStreamUnavailable) (Object) this;
        if (BUTTON_LIST == null) {
            return;
        }
        try {
            Object value = BUTTON_LIST.get(screen);
            if (!(value instanceof List)) {
                return;
            }
            int y = screen.height - 72;
            for (Object element : (List) value) {
                if (element instanceof GuiButton) {
                    ((GuiButton) element).yPosition = y;
                }
            }
        } catch (Throwable ignored) {
            // Leave vanilla layout if the field cannot be read.
        }
    }

    private static Field findButtonListField() {
        for (String name : new String[] {"buttonList", "field_146292_n"}) {
            try {
                Field field = GuiScreen.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // Try the SRG name next.
            }
        }
        return null;
    }
}
