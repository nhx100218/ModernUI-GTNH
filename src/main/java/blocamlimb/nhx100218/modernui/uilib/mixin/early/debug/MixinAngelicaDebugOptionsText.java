package blocamlimb.nhx100218.modernui.uilib.mixin.early.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiScreen;

import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuFrame;
import blocamlimb.nhx100218.modernui.uilib.font.AngelicaBatchTextQueue;
import blocamlimb.nhx100218.modernui.uilib.font.config.FontConfig;

/**
 * Replaces Angelica's bitmap-font text in the F3 options screen with ModernUI's
 * deferred SDF text.
 *
 * <p>The screen draws everything through {@code angelica$drawStringBatched};
 * {@link blocamlimb.nhx100218.modernui.uilib.mixin.early.MixinFontRenderer}
 * captures those calls, and this mixin flushes the queue after the whole screen
 * (including tooltips) has been drawn.</p>
 */
@Mixin(targets = "com.gtnewhorizons.angelica.debug.screen.DebugOptionsScreen", remap = false)
public abstract class MixinAngelicaDebugOptionsText {

    @Inject(method = "func_73863_a", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$beginTextFrame(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        GuiScreen screen = (GuiScreen) (Object) this;
        ModernMenuFrame.drawWorldMask(screen.width, screen.height);
        if (FontConfig.replaceOrigin) {
            AngelicaBatchTextQueue.begin();
        }
    }

    @Inject(method = "func_73863_a", at = @At("RETURN"), remap = false, require = 0)
    private void modernui$flushTextFrame(int mouseX, int mouseY, float partialTicks,
            CallbackInfo callbackInfo) {
        GuiScreen screen = (GuiScreen) (Object) this;
        ModernGuiRenderer.beginUi();
        ModernMenuFrame.drawFrame(screen.width, screen.height);
        if (FontConfig.replaceOrigin) {
            ModernGuiRenderer.beginUi();
            AngelicaBatchTextQueue.flush();
        }
    }
}
