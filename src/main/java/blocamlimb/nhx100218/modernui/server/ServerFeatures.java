package blocamlimb.nhx100218.modernui.server;

import net.minecraft.entity.player.EntityPlayerMP;

import blocamlimb.nhx100218.modernui.config.runtime.ConfigManager;
import blocamlimb.nhx100218.modernui.uilib.MyMod;
import blocamlimb.nhx100218.modernui.uilib.config.modern.ModernUiConfigSchema;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Boots the server-only features on both dedicated and integrated servers.
 */
public final class ServerFeatures {

    public static final ServerFeatures INSTANCE = new ServerFeatures();

    private static boolean initialized;

    private ServerFeatures() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        FMLCommonHandler.instance().bus().register(ServerMetrics.INSTANCE);
        FMLCommonHandler.instance().bus().register(INSTANCE);
        if (ServerFeatureConfig.configFile() != null) {
            MiniMotdConfig.initialize(ServerFeatureConfig.configFile().getParentFile());
        }
        MyMod.LOG.info(
                "服务端功能初始化：side={}, ServerUtilities={}",
                FMLCommonHandler.instance().getSide() == Side.CLIENT ? "CLIENT" : "SERVER",
                Boolean.valueOf(ServerTabService.available()));
    }

    public static void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new MugCommand());
        ServerTabService.forceUpdate();
    }

    public static boolean reload() {
        if (ServerFeatureConfig.configFile() == null) {
            return false;
        }
        try {
            ConfigManager manager = ConfigManager.bootstrap(
                    ServerFeatureConfig.configFile(),
                    ModernUiConfigSchema.create());
            ServerFeatureConfig.apply(manager.authority());
            ServerMotdService.clearEnabledOverride();
            ServerMotdService.invalidate();
            ServerTabService.forceUpdate();
            return true;
        } catch (Throwable t) {
            MyMod.LOG.warn("重新加载服务端配置失败", t);
            return false;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerTabService.tick();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ServerTabService.onPlayerLogin((EntityPlayerMP) event.player);
        }
    }
}
