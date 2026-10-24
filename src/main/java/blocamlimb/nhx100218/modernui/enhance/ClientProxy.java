package blocamlimb.nhx100218.modernui.enhance;

import blocamlimb.nhx100218.modernui.enhance.tooltip.ModularUiTooltipHandler;
import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipForgeHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // ModernUI Config 模块加载（scoped 客户端：配置只影响客户端渲染/交互）。
        // 接管 NEI/GTNHLib 的 tooltip 渲染。
        TooltipForgeHandler.install();
        // 放宽 ModularUI2（格雷）tooltip 折行宽度。
        ModularUiTooltipHandler.install();
        // Ctrl+K 打开配置页
        FMLCommonHandler.instance().bus().register(new blocamlimb.nhx100218.modernui.enhance.client.ConfigKeyHandler());
        // 逐帧驱动 HUD 缓存重建（Angelica HUD 缓存下让聊天/快捷栏缓动可见）
        blocamlimb.nhx100218.modernui.enhance.client.SmoothScrollHudTicker ticker = new blocamlimb.nhx100218.modernui.enhance.client.SmoothScrollHudTicker();
        FMLCommonHandler.instance().bus().register(ticker);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ticker);
        // 启动预热（字体字形/着色器）：渲染 tick 驱动，加载界面期间即可触发。
        FMLCommonHandler.instance().bus().register(new blocamlimb.nhx100218.modernui.enhance.client.PreloadHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        // 本模组加载完成后布防预热（在加载界面执行一次全加载）。
        blocamlimb.nhx100218.modernui.enhance.client.PreloadService.arm();
    }
}
