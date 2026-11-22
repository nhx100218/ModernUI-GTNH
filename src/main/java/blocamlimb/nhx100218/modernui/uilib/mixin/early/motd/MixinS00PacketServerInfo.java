package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.server.S00PacketServerInfo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.server.ServerMotdService;
import blocamlimb.nhx100218.modernui.uilib.MyMod;

/**
 * Writes the RGB MOTD component directly into the status packet.
 *
 * <p>The Gson adapter path is avoided here on purpose: the status JSON is
 * produced normally (including Forge's extra status fields), then only the
 * {@code description} field is swapped for the 1.16+ RGB component.</p>
 */
@Mixin(S00PacketServerInfo.class)
public abstract class MixinS00PacketServerInfo {

    @Shadow
    @Final
    private static Gson field_149297_a;

    @Shadow
    private ServerStatusResponse field_149296_b;

    private static long modernui$lastWriteLog;

    @Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
    private void modernui$writeRgbDescription(PacketBuffer data, CallbackInfo callbackInfo) throws IOException {
        if (ServerMotdService.directDescription(field_149296_b) == null) {
            return;
        }
        JsonElement serialized = field_149297_a.toJsonTree(field_149296_b);
        if (!(serialized instanceof JsonObject)) {
            return;
        }
        if (!ServerMotdService.applyDirectDescription(field_149296_b, (JsonObject) serialized)) {
            return;
        }
        callbackInfo.cancel();
        String json = serialized.toString();
        data.writeStringToBuffer(json);
        long now = System.currentTimeMillis();
        if (now - modernui$lastWriteLog > 5000L) {
            modernui$lastWriteLog = now;
            MyMod.LOG.info("MOTD packet: wrote RGB status component ({} chars)", Integer.valueOf(json.length()));
        }
    }
}
