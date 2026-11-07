package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd;

import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerStatusServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import blocamlimb.nhx100218.modernui.server.ServerMotdService;

/**
 * Replaces the cached status response with the configured MOTD when a client
 * pings the server list.
 */
@Mixin(NetHandlerStatusServer.class)
public abstract class MixinNetHandlerStatusServer {

    @Redirect(
            method = "processServerQuery",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;func_147134_at()Lnet/minecraft/network/ServerStatusResponse;"))
    private ServerStatusResponse modernui$applyMotd(MinecraftServer server) {
        return ServerMotdService.apply(server, server.func_147134_at());
    }
}
