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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.server.RgbStatusPacket;
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
public abstract class MixinS00PacketServerInfo implements RgbStatusPacket {

    @Shadow
    @Final
    private static Gson field_149297_a;

    @Shadow
    private ServerStatusResponse field_149296_b;

    @Unique
    private JsonObject modernui$rgbDescription;

    @Unique
    private static long modernui$lastWriteLog;

    @Unique
    @Override
    public void modernui$setRgbDescription(JsonObject description) {
        modernui$rgbDescription = description;
    }

    @Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
    private void modernui$writeRgbDescription(PacketBuffer data, CallbackInfo callbackInfo) throws IOException {
        JsonObject custom = modernui$rgbDescription;
        if (custom == null) {
            custom = ServerMotdService.directDescription(field_149296_b);
        }
        long now = System.currentTimeMillis();
        boolean log = now - modernui$lastWriteLog > 5000L;
        if (log) {
            modernui$lastWriteLog = now;
            MyMod.LOG.info("MOTD packet mixin: description={} packet={}",
                    Boolean.valueOf(custom != null),
                    Integer.valueOf(System.identityHashCode(this)));
        }
        if (custom == null) {
            return;
        }
        JsonElement serialized = field_149297_a.toJsonTree(field_149296_b);
        if (!(serialized instanceof JsonObject)) {
            return;
        }
        ((JsonObject) serialized).add("description", custom);
        callbackInfo.cancel();
        String json = serialized.toString();
        data.writeStringToBuffer(json);
        if (log) {
            MyMod.LOG.info("MOTD packet: wrote RGB status component ({} chars)", Integer.valueOf(json.length()));
        }
    }
}
