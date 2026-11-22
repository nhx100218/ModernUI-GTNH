package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd.client;

import com.google.gson.JsonArray;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.ServerStatusResponse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.uilib.client.motd.ModernServerData;
import blocamlimb.nhx100218.modernui.uilib.client.motd.ModernStatusResponse;

/**
 * Bridges the status response's exact colours to the server-list entry.
 *
 * <p>Forge calls {@code bindServerListData(ServerData, ServerStatusResponse)}
 * from the pinger after reading the response, which is the one place both
 * objects are available.</p>
 */
@Mixin(targets = "cpw.mods.fml.client.FMLClientHandler", remap = false)
public abstract class MixinFMLClientHandler {

    @Inject(method = "bindServerListData", at = @At("HEAD"), remap = false, require = 0)
    private void modernui$copyMotdColors(ServerData data, ServerStatusResponse response,
            CallbackInfo callbackInfo) {
        if (data instanceof ModernServerData && response instanceof ModernStatusResponse) {
            JsonArray colors = ((ModernStatusResponse) response).modernui$getLegacyColors();
            if (colors != null) {
                ((ModernServerData) data).modernui$setLegacyMotdColors(colors);
            }
        }
    }
}
