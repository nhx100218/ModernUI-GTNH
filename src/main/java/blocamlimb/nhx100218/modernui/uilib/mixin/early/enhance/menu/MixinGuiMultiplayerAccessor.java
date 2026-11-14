package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerSelectionList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the server selection list to the arrow/reorder mixin. */
@Mixin(GuiMultiplayer.class)
public interface MixinGuiMultiplayerAccessor {

    @Accessor("field_146803_h")
    ServerSelectionList modernui$getServerSelectionList();
}
