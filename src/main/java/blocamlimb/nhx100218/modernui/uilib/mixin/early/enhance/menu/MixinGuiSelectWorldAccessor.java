package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import java.util.List;

import net.minecraft.client.gui.GuiSelectWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the save list to the icon-join mixin without widening the vanilla API. */
@Mixin(GuiSelectWorld.class)
public interface MixinGuiSelectWorldAccessor {

    @Accessor("field_146639_s")
    List modernui$getSaveList();
}
