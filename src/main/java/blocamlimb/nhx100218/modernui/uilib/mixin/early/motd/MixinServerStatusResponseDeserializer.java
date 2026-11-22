package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializationContext;

import net.minecraft.network.ServerStatusResponse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.uilib.client.motd.ModernStatusResponse;

/**
 * Reads {@code modernui_rgb} from the status JSON. Vanilla 1.7.10 ignores this
 * field; the mixin only copies it onto the response for ModernUI's renderer.
 */
@Mixin(ServerStatusResponse.Serializer.class)
public abstract class MixinServerStatusResponseDeserializer {

    @Inject(method = "deserialize", at = @At("RETURN"))
    private void modernui$readLegacyColors(JsonElement element, Type type,
            JsonDeserializationContext context, CallbackInfoReturnable<ServerStatusResponse> callbackInfo) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (!json.has("modernui_rgb") || !json.get("modernui_rgb").isJsonArray()) {
            return;
        }
        ServerStatusResponse response = callbackInfo.getReturnValue();
        if (response instanceof ModernStatusResponse) {
            JsonArray colors = json.getAsJsonArray("modernui_rgb");
            ((ModernStatusResponse) response).modernui$setLegacyColors(colors);
        }
    }
}
