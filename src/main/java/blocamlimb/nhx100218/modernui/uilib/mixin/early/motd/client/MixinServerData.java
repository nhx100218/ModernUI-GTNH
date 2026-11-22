package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd.client;

import com.google.gson.JsonArray;

import net.minecraft.client.multiplayer.ServerData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import blocamlimb.nhx100218.modernui.uilib.client.motd.ModernServerData;

/** Attaches the exact MOTD colours to the server-list entry data. */
@Mixin(ServerData.class)
public abstract class MixinServerData implements ModernServerData {

    @Unique
    private JsonArray modernui$legacyMotdColors;

    @Unique
    @Override
    public void modernui$setLegacyMotdColors(JsonArray colors) {
        modernui$legacyMotdColors = colors;
    }

    @Unique
    @Override
    public JsonArray modernui$getLegacyMotdColors() {
        return modernui$legacyMotdColors;
    }
}
