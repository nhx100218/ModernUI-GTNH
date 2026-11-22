package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.server.S00PacketServerInfo;

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
 * <p>Deliberately avoids the vanilla Gson/serializer chain: the JSON is built
 * here from the response fields and the description is the 1.16+ RGB
 * component. This keeps the 26.3 status response independent of the legacy
 * {@code IChatComponent} model.</p>
 */
@Mixin(S00PacketServerInfo.class)
public abstract class MixinS00PacketServerInfo implements RgbStatusPacket {

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

        JsonObject json = new JsonObject();
        json.add("description", custom);

        ServerStatusResponse.PlayerCountData players = field_149296_b == null
                ? null : field_149296_b.func_151318_b();
        if (players != null) {
            JsonObject playerJson = new JsonObject();
            playerJson.addProperty("max", Integer.valueOf(players.func_151332_a()));
            playerJson.addProperty("online", Integer.valueOf(players.func_151333_b()));
            GameProfile[] sample = players.func_151331_c();
            if (sample != null && sample.length > 0) {
                JsonArray sampleJson = new JsonArray();
                for (GameProfile profile : sample) {
                    if (profile == null) {
                        continue;
                    }
                    JsonObject entry = new JsonObject();
                    entry.addProperty("id", profile.getId() == null ? "" : profile.getId().toString());
                    entry.addProperty("name", profile.getName() == null ? "" : profile.getName());
                    sampleJson.add(entry);
                }
                playerJson.add("sample", sampleJson);
            }
            json.add("players", playerJson);
        }

        ServerStatusResponse.MinecraftProtocolVersionIdentifier version = field_149296_b == null
                ? null : field_149296_b.func_151322_c();
        if (version != null) {
            JsonObject versionJson = new JsonObject();
            versionJson.addProperty("name", version.func_151303_a());
            versionJson.addProperty("protocol", Integer.valueOf(version.func_151304_b()));
            json.add("version", versionJson);
        }

        String favicon = field_149296_b == null ? null : field_149296_b.func_151316_d();
        if (favicon != null) {
            json.addProperty("favicon", favicon);
        }

        callbackInfo.cancel();
        String text = json.toString();
        data.writeStringToBuffer(text);
        if (log) {
            MyMod.LOG.info("MOTD packet: wrote RGB status component ({} chars)", Integer.valueOf(text.length()));
        }
    }
}
