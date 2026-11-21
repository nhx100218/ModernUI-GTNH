package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.gui.GuiSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Calls {@code GuiSlot.overlayBackground} with a custom alpha.
 *
 * <p>Iris' custom lists override {@code drawScreen}; suppressing the fade by
 * cancelling the method removed its tessellator flush and GL-state restore,
 * which made the shader screen flicker. Calling the real method with alpha 0
 * keeps the state machine intact and only hides the fade pixels.</p>
 */
@Mixin(GuiSlot.class)
public interface MixinGuiSlotFadeAccessor {

    @Invoker("overlayBackground")
    void modernui$overlayBackground(int top, int bottom, int topAlpha, int bottomAlpha);
}
