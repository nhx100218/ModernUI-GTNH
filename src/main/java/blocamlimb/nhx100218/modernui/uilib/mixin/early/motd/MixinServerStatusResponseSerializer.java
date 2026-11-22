package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.network.ServerStatusResponse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.server.ServerMotdService;

/**
 * Replaces the status description with the exact RGB component for 1.16+
 * clients. The legacy path keeps the normal {@link ServerStatusResponse}
 * description, so 1.7.10 clients still receive legacy colour codes.
 */
@Mixin(ServerStatusResponse.Serializer.class)
public abstract class MixinServerStatusResponseSerializer {

    @Inject(
            method = "serialize(Lnet/minecraft/network/ServerStatusResponse;Ljava/lang/reflect/Type;"
                    + "Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
            at = @At("RETURN"))
    private void modernui$replaceDescription(ServerStatusResponse response, Type type,
            JsonSerializationContext context, CallbackInfoReturnable<JsonElement> callbackInfo) {
        JsonObject description = ServerMotdService.directDescription(response);
        if (description == null) {
            return;
        }
        JsonElement result = callbackInfo.getReturnValue();
        if (result instanceof JsonObject) {
            ((JsonObject) result).add("description", description);
        }
    }
}
