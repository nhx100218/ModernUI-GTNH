package blocamlimb.nhx100218.modernui.enhance;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        if (Loader.isModLoaded("chromatictooltips")) {
            MyMod.LOG.warn("检测到 ChromaticTooltips：它同样会接管 tooltip 渲染，可能覆盖本附属的样式。建议移除 chromatictooltips 与 chromatictooltipscompat。");
        }
    }
}
