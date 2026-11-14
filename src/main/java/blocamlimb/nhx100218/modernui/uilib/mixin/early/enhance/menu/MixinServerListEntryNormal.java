package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.menu.MenuConfig;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernGuiRenderer;
import blocamlimb.nhx100218.modernui.enhance.menu.ModernMenuArrows;

/**
 * Adds the 26.3 server-icon actions: join on the right half, move up/down on the
 * left quarters.
 *
 * <p>The sprite sheet is the vanilla resource-pack selector, so no new GUI art is
 * shipped. Reordering writes through {@link ServerList#swapServers(int, int)},
 * which also saves {@code servers.dat}.</p>
 */
@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal {

    private static final int ICON_SIZE = 32;

    @Shadow
    private GuiMultiplayer field_148303_c;

    @Inject(method = "drawEntry", at = @At("TAIL"), require = 0)
    private void modernui$drawServerArrows(int slotIndex, int x, int y, int width, int height,
            Tessellator tessellator, int mouseX, int mouseY, boolean hovered, CallbackInfo callbackInfo) {
        if (!MenuConfig.serverListArrows || !hovered) {
            return;
        }
        ServerList serverList = field_148303_c.func_146795_p();
        int count = serverList == null ? 0 : serverList.countServers();
        if (slotIndex < 0 || slotIndex >= count) {
            return;
        }
        boolean overJoin = ModernMenuArrows.overRightHalf(mouseX, mouseY, x, y, ICON_SIZE);
        boolean overUp = slotIndex > 0 && ModernMenuArrows.overTopLeftQuarter(mouseX, mouseY, x, y, ICON_SIZE);
        boolean overDown = slotIndex < count - 1
                && ModernMenuArrows.overBottomLeftQuarter(mouseX, mouseY, x, y, ICON_SIZE);
        ModernGuiRenderer.beginUi();
        ModernMenuArrows.drawIconShade(x, y, ICON_SIZE);
        ModernMenuArrows.drawServerJoin(x, y, ICON_SIZE, overJoin);
        if (slotIndex > 0) {
            ModernMenuArrows.drawMoveUp(x, y, ICON_SIZE, overUp);
        }
        if (slotIndex < count - 1) {
            ModernMenuArrows.drawMoveDown(x, y, ICON_SIZE, overDown);
        }
    }

    @Inject(method = "mousePressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void modernui$handleServerArrows(int slotIndex, int mouseX, int mouseY, int mouseButton,
            int relativeX, int relativeY, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!MenuConfig.serverListArrows || mouseButton != 0) {
            return;
        }
        ServerList serverList = field_148303_c.func_146795_p();
        int count = serverList == null ? 0 : serverList.countServers();
        if (slotIndex < 0 || slotIndex >= count) {
            return;
        }
        if (ModernMenuArrows.overRightHalf(relativeX, relativeY, 0, 0, ICON_SIZE)) {
            field_148303_c.func_146790_a(slotIndex);
            field_148303_c.func_146796_h();
            callbackInfo.setReturnValue(Boolean.TRUE);
            callbackInfo.cancel();
            return;
        }
        if (ModernMenuArrows.overTopLeftQuarter(relativeX, relativeY, 0, 0, ICON_SIZE) && slotIndex > 0) {
            modernui$swap(slotIndex, slotIndex - 1);
            callbackInfo.setReturnValue(Boolean.TRUE);
            callbackInfo.cancel();
            return;
        }
        if (ModernMenuArrows.overBottomLeftQuarter(relativeX, relativeY, 0, 0, ICON_SIZE)
                && slotIndex < count - 1) {
            modernui$swap(slotIndex, slotIndex + 1);
            callbackInfo.setReturnValue(Boolean.TRUE);
            callbackInfo.cancel();
        }
    }

    private void modernui$swap(int from, int to) {
        ServerList serverList = field_148303_c.func_146795_p();
        if (serverList == null) {
            return;
        }
        serverList.swapServers(from, to);
        ServerSelectionList list = ((MixinGuiMultiplayerAccessor) field_148303_c).modernui$getServerSelectionList();
        if (list != null) {
            list.func_148195_a(serverList);
        }
        field_148303_c.func_146790_a(to);
    }
}
