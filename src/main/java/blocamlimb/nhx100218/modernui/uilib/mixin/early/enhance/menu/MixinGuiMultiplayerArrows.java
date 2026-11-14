package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuConfig;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuArrows;

/**
 * Handles the server-list reorder arrows before the vanilla list receives the
 * click.
 *
 * <p>{@code ServerListEntryNormal.mousePressed} receives row-relative
 * coordinates. That path works for the join half but is fragile for the small
 * up/down quarters. Resolving the hovered entry from {@link GuiMultiplayer}
 * keeps the hit test in absolute screen coordinates and lets the list rebuild
 * only after the swap.</p>
 */
@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayerArrows {

    private static final int ICON_SIZE = 32;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$handleServerReorder(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (!MenuConfig.serverListArrows || mouseButton != 0) {
            return;
        }
        GuiMultiplayer self = (GuiMultiplayer) (Object) this;
        ServerSelectionList list = ((MixinGuiMultiplayerAccessor) self).modernui$getServerSelectionList();
        if (list == null) {
            return;
        }
        int slot = list.func_148124_c(mouseX, mouseY);
        if (slot < 0) {
            return;
        }
        GuiListExtended.IGuiListEntry entry = list.getListEntry(slot);
        if (!(entry instanceof ServerListEntryNormal)) {
            return;
        }
        ServerList serverList = self.func_146795_p();
        if (serverList == null || slot >= serverList.countServers()) {
            return;
        }
        int rowX = list.left + list.width / 2 - list.getListWidth() / 2 + 2;
        int rowY = list.top + 4 - list.getAmountScrolled() + slot * list.slotHeight + list.headerPadding;
        int relativeX = mouseX - rowX;
        int relativeY = mouseY - rowY;

        if (ModernMenuArrows.overTopLeftQuarter(relativeX, relativeY, 0, 0, ICON_SIZE) && slot > 0) {
            modernui$swap(self, list, serverList, slot, slot - 1);
            callbackInfo.cancel();
            return;
        }
        if (ModernMenuArrows.overBottomLeftQuarter(relativeX, relativeY, 0, 0, ICON_SIZE)
                && slot < serverList.countServers() - 1) {
            modernui$swap(self, list, serverList, slot, slot + 1);
            callbackInfo.cancel();
        }
    }

    private static void modernui$swap(GuiMultiplayer self, ServerSelectionList list, ServerList serverList,
            int from, int to) {
        serverList.swapServers(from, to);
        list.func_148195_a(serverList);
        self.func_146790_a(to);
    }
}
