package blocamlimb.nhx100218.modernui.uilib.mixin.early.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.server.ServerMotdService;

/**
 * Captures the client protocol version from the status handshake so the MOTD
 * can use 1.16+ RGB colours for modern clients and the 16-colour fallback for
 * older ones. The version is kept per {@link NetworkManager} and consumed by
 * the status query on the same connection.
 */
@Mixin(net.minecraft.server.network.NetHandlerHandshakeTCP.class)
public abstract class MixinNetHandlerHandshakeTCP {

    @Shadow
    private NetworkManager field_147386_b;

    @Inject(method = "processHandshake", at = @At("HEAD"))
    private void modernui$rememberProtocol(C00Handshake packet, CallbackInfo callbackInfo) {
        ServerMotdService.rememberProtocol(field_147386_b, packet.func_149595_d());
    }
}
