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
            mixins.add("uilib.mixin.early.MixinFontRenderer");
            mixins.add("uilib.mixin.early.MixinMinecraftWorldLoadPump");
            mixins.add("uilib.mixin.early.MixinGuiScreenKeyboardIsolation");
            mixins.add("uilib.mixin.early.network.MixinNetHandlerPlayClient");
            mixins.add("uilib.mixin.early.nametag.MixinEntityRendererPlayerNameTagPass");
            mixins.add("uilib.mixin.early.nametag.MixinRendererLivingEntityPlayerNameTag");
            mixins.add("uilib.mixin.early.nametag.MixinRenderPlayerScoreboardNameTag");
            if (loadedCoreMods != null && loadedCoreMods.contains(ANGELICA_TWEAKER)) {
                mixins.add("uilib.mixin.early.nametag.MixinAngelicaPlayerNameTagReplay");
                mixins.add("uilib.mixin.early.tesr.MixinAngelicaTesrBatchReplay");
            }
            // Merged enhancement
            mixins.add("enhance.mixin.early.MixinGuiScreenTooltip");
            mixins.add("enhance.mixin.early.MixinGuiSlotSmoothScroll");
            mixins.add("enhance.mixin.early.MixinEntityRendererScreenTransition");
            mixins.add("enhance.mixin.early.MixinEntityRendererZoom");
            mixins.add("enhance.mixin.early.MixinGuiNewChat");
            mixins.add("enhance.mixin.early.MixinMinecraftUnfocused");
            mixins.add("enhance.mixin.early.MixinGameSettingsUnfocused");
            mixins.add("enhance.mixin.early.MixinGuiTextFieldUndo");
            mixins.add("enhance.mixin.early.MixinGuiContainerPause");
            mixins.add("enhance.mixin.early.MixinGuiContainerCreativeSmoothScroll");
            mixins.add("enhance.mixin.early.MixinGuiTextFieldSmoothScroll");
            mixins.add("enhance.mixin.early.MixinGuiTextFieldSelection");
            mixins.add("enhance.mixin.early.MixinMinecraftScreenPoke");
            mixins.add("enhance.mixin.early.MixinGuiIngameHotbarScroll");
            mixins.add("enhance.mixin.early.MixinGuiHotbarTexture");
            mixins.add("enhance.mixin.early.MixinInventoryPlayerHotbar");
            mixins.add("enhance.mixin.early.MixinGuiNewChatScroll");
        }
        mixins.add("uilib.mixin.early.network.MixinNetHandlerPlayServer");
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
