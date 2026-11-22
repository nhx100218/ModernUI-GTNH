package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import com.google.gson.JsonArray;

import net.minecraft.network.ServerStatusResponse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import blocamlimb.nhx100218.modernui.uilib.client.motd.ModernStatusResponse;

/** Carries the custom exact-colour field through the vanilla status response. */
@Mixin(ServerStatusResponse.class)
public abstract class MixinServerStatusResponse implements ModernStatusResponse {

    @Unique
    private JsonArray modernui$legacyColors;

    @Unique
    @Override
    public void modernui$setLegacyColors(JsonArray colors) {
        modernui$legacyColors = colors;
    }

    @Unique
    @Override
    public JsonArray modernui$getLegacyColors() {
        return modernui$legacyColors;
    }
}
