package blocamlimb.nhx100218.modernui.uilib.mixin.early;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.Side;

/**
 * Early mixin loader for vanilla classes. Loads both the library and the merged
 * enhancement mixins through a single coremod and a single mixin config.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixins implements IEarlyMixinLoader, IFMLLoadingPlugin {

    private static final String ANGELICA_TWEAKER = "com.gtnewhorizons.angelica.loading.AngelicaTweaker";

    @Override
    public String getMixinConfig() {
        return "mixins.modernuigtnh.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return buildMixinsForSide(FMLLaunchHandler.side(), loadedCoreMods);
    }

    static List<String> buildMixinsForSide(Side side) {
        return buildMixinsForSide(side, Collections.<String>emptySet());
    }

    static List<String> buildMixinsForSide(Side side, Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<String>();
        if (side == Side.CLIENT) {
            // Library
            mixins.add("MixinFontRenderer");
            mixins.add("MixinMinecraftWorldLoadPump");
            mixins.add("MixinGuiScreenKeyboardIsolation");
            mixins.add("network.MixinNetHandlerPlayClient");
            mixins.add("nametag.MixinEntityRendererPlayerNameTagPass");
            mixins.add("nametag.MixinRendererLivingEntityPlayerNameTag");
            mixins.add("nametag.MixinRenderPlayerScoreboardNameTag");
            if (loadedCoreMods != null && loadedCoreMods.contains(ANGELICA_TWEAKER)) {
                mixins.add("nametag.MixinAngelicaPlayerNameTagReplay");
                mixins.add("tesr.MixinAngelicaTesrBatchReplay");
            }
            // Merged enhancement
            mixins.add("enhance.MixinGuiScreenTooltip");
            mixins.add("enhance.MixinGuiSlotSmoothScroll");
            mixins.add("enhance.MixinEntityRendererScreenTransition");
            mixins.add("enhance.MixinEntityRendererZoom");
            mixins.add("enhance.MixinGuiNewChat");
            mixins.add("enhance.MixinMinecraftUnfocused");
            mixins.add("enhance.MixinGameSettingsUnfocused");
            mixins.add("enhance.MixinGuiTextFieldUndo");
            mixins.add("enhance.MixinGuiContainerPause");
            mixins.add("enhance.MixinGuiContainerCreativeSmoothScroll");
            mixins.add("enhance.MixinGuiTextFieldSmoothScroll");
            mixins.add("enhance.MixinGuiTextFieldSelection");
            mixins.add("enhance.MixinMinecraftScreenPoke");
            mixins.add("enhance.MixinGuiIngameHotbarScroll");
            mixins.add("enhance.MixinGuiHotbarTexture");
            mixins.add("enhance.MixinInventoryPlayerHotbar");
            mixins.add("enhance.MixinGuiNewChatScroll");
        }
        mixins.add("network.MixinNetHandlerPlayServer");
        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Object location = data.get("mcLocation");
        if (location instanceof File) {
            blocamlimb.nhx100218.modernui.enhance.vanilla.StartupConfigFixer
                    .fix(new File((File) location, "config"));
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
