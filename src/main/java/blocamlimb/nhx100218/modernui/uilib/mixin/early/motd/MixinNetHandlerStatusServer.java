package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import com.google.gson.JsonObject;

import io.netty.util.concurrent.GenericFutureListener;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerStatusServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.server.RgbStatusPacket;
import blocamlimb.nhx100218.modernui.server.ServerMotdService;

/**
 * Replaces the cached status response with the configured MOTD when a client
 * pings the server list.
 */
@Mixin(NetHandlerStatusServer.class)
public abstract class MixinNetHandlerStatusServer {

    @Shadow
    private MinecraftServer field_147314_a;

    @Shadow
    private NetworkManager field_147313_b;

    @Inject(method = "processServerQuery", at = @At("HEAD"), cancellable = true)
    private void modernui$sendRgbStatus(C00PacketServerQuery packet, CallbackInfo callbackInfo) {
        ServerStatusResponse response = ServerMotdService.apply(
                field_147314_a, field_147314_a.func_147134_at(),
                ServerMotdService.protocolFor(field_147313_b));
        if (response == null) {
            return;
        }
        S00PacketServerInfo info = new S00PacketServerInfo(response);
        JsonObject custom = ServerMotdService.directDescription(response);
        if (custom != null && info instanceof RgbStatusPacket) {
            ((RgbStatusPacket) info).modernui$setRgbDescription(custom);
        }
        field_147313_b.scheduleOutboundPacket(info, new GenericFutureListener[0]);
        callbackInfo.cancel();
    }

    @Redirect(
            method = "processServerQuery",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;func_147134_at()Lnet/minecraft/network/ServerStatusResponse;"),
            require = 0)
    private ServerStatusResponse modernui$applyMotd(MinecraftServer server) {
        return ServerMotdService.apply(server, server.func_147134_at(),
                ServerMotdService.protocolFor(field_147313_b));
    }
}
