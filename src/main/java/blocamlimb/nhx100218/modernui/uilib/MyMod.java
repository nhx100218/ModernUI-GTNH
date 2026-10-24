package blocamlimb.nhx100218.modernui.uilib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = MyMod.MODID,
        version = Tags.VERSION,
        name = "ModernUI-GTNH",
        acceptedMinecraftVersions = "[1.7.10]",
        // 远端版本协商（契约见 FmlRemoteVersionCompatibilityContractTest）：区间必须包含当前
        // Tags.VERSION，否则 FML 判定「mod 拒绝自身版本」——集成服务器在进入世界的握手阶段
        // Rejecting connection CLIENT 后卸载全部维度（症状：无法进入世界）。
        // 注意「显式空串」不是开发期形态：它构成空区间并拒绝一切（含自身）；精确版本相等只属于
        // 整条属性不写的形态。区间不跨 major：下界为 4.11.0 定档时收紧的已承诺下界，上界取当前
        // 制品 4.11.0 的下一 minor 边界 4.12.0（覆盖 4.11.x；与 4.10.x 及更早不承诺混用）。
        acceptableRemoteVersions = "[4.11.0,4.12.0)",
        guiFactory = MyMod.GUI_FACTORY)
public class MyMod {

    public static final String MODID = "modernuigtnh";
    public static final String MOD_NAME = "ModernUI-GTNH";
    public static final String CLIENT_PROXY = "blocamlimb.nhx100218.modernui.uilib.ClientProxy";
    public static final String COMMON_PROXY = "blocamlimb.nhx100218.modernui.uilib.CommonProxy";
    public static final String GUI_FACTORY = "blocamlimb.nhx100218.modernui.uilib.config.ModGuiFactory";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy;

    /** 原附属增强模组的代理（合并后仍在此统一初始化）。 */
    @SidedProxy(
            clientSide = "blocamlimb.nhx100218.modernui.enhance.ClientProxy",
            serverSide = "blocamlimb.nhx100218.modernui.enhance.CommonProxy")
    public static blocamlimb.nhx100218.modernui.enhance.CommonProxy enhanceProxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        enhanceProxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        enhanceProxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        enhanceProxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
