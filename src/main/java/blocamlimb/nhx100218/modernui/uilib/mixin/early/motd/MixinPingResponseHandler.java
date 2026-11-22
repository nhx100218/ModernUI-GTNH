package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import net.minecraft.network.PingResponseHandler;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.server.ServerMotdService;

/**
 * Legacy {@code 0xFE} pings use {@link MinecraftServer#getMOTD()}, i.e. the
 * {@code server.properties} text, and therefore bypass the modern status
 * response. Some proxies and modern launchers still query servers this way.
 */
@Mixin(PingResponseHandler.class)
public abstract class MixinPingResponseHandler {

    @Redirect(
            method = "channelRead",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getMOTD()Ljava/lang/String;"))
    private String modernui$legacyMotd(MinecraftServer server) {
        return ServerMotdService.legacyMotd(server);
    }
}
