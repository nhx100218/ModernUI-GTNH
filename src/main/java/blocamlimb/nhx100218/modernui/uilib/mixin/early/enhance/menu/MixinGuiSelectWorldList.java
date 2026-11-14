package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.util.List;

import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuConfig;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuArrows;

/**
 * Adds the 26.3 world-icon join arrow to the vanilla singleplayer list.
 *
 * <p>EFR already draws a world thumbnail in the same 32x32 box, so the arrow is
 * layered at TAIL and the click test uses the icon box rather than the row.</p>
 */
@Mixin(targets = "net.minecraft.client.gui.GuiSelectWorld$List")
public abstract class MixinGuiSelectWorldList {

    private static final int ICON_SIZE = 32;

    @Shadow
    @Final
    GuiSelectWorld this$0;

    @Inject(method = "drawSlot", at = @At("TAIL"), require = 0)
    private void modernui$drawWorldJoinArrow(int slotIndex, int x, int y, int slotHeight, Tessellator tessellator,
            int mouseX, int mouseY, CallbackInfo callbackInfo) {
        List saves = saves();
        if (!MenuConfig.worldQuickJoin || saves == null || slotIndex < 0 || slotIndex >= saves.size()) {
            return;
        }
        boolean hovered = ModernMenuArrows.overIcon(mouseX, mouseY, x, y, ICON_SIZE);
        if (!hovered) {
            return;
        }
        ModernGuiRenderer.beginUi();
        ModernMenuArrows.drawIconShade(x, y, ICON_SIZE);
        ModernMenuArrows.drawJoin(x, y, ICON_SIZE, true);
    }

    @Inject(method = "elementClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$quickJoinWorld(int slotIndex, boolean doubleClick, int mouseX, int mouseY,
            CallbackInfo callbackInfo) {
        List saves = saves();
        if (!MenuConfig.worldQuickJoin || saves == null || slotIndex < 0 || slotIndex >= saves.size()) {
            return;
        }
        GuiSlot slot = (GuiSlot) (Object) this;
        int iconX = this$0.width / 2 - slot.getListWidth() / 2;
        int iconY = slot.top + 4 - (int) slot.getAmountScrolled() + slotIndex * slot.slotHeight;
        if (ModernMenuArrows.overIcon(mouseX, mouseY, iconX, iconY, ICON_SIZE)) {
            this$0.func_146615_e(slotIndex);
            callbackInfo.cancel();
        }
    }

    private List saves() {
        return ((MixinGuiSelectWorldAccessor) this$0).modernui$getSaveList();
    }
}
