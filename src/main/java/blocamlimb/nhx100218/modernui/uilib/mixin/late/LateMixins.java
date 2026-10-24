package blocamlimb.nhx100218.modernui.uilib.mixin.late;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Late compatibility mixins for optional mods. The library and the merged
 * enhancement mixins share this single loader and a single mixin config.
 */
@LateMixin
public class LateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.modernuigtnh.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return buildMixinsForSide(FMLLaunchHandler.side(), loadedMods);
    }

    static List<String> buildMixinsForSide(Side side, Set<String> loadedMods) {
        if (side != Side.CLIENT || loadedMods == null) {
            return Collections.emptyList();
        }
        final Set<String> mods = loadedMods;
        java.util.function.Predicate<String> has = id -> {
            for (String s : mods) {
                if (s != null && s.equalsIgnoreCase(id)) {
                    return true;
                }
            }
            return false;
        };

        List<String> mixins = new ArrayList<String>();
        if (has.test("appliedenergistics2")) {
            mixins.add("ae2.MixinAbstractPartMonitor");
        }
        if (has.test("Waila")) {
            mixins.add("enhance.waila.MixinWailaOverlayRenderer");
        }
        if (has.test("CodeChickenCore")) {
            mixins.add("enhance.codechicken.MixinGuiDrawTooltip");
        }
        if (has.test("modularui2")) {
            mixins.add("enhance.modularui.MixinModularUiTooltip");
            mixins.add("enhance.modularui.MixinModularScreenTransition");
            mixins.add("enhance.modularui.MixinTextRendererNoWrap");
            mixins.add("enhance.modularui.MixinRichTextCompilerNoWrap");
        }
        if (has.test("chromatictooltips")) {
            mixins.add("enhance.ct.MixinCtTooltipDecorator");
            mixins.add("enhance.ct.MixinCtTextComponent");
        }
        if (has.test("chromatictooltipscompat")) {
            mixins.add("enhance.ct.MixinNeiTooltipComponentCompat");
        }
        if (has.test("betterquesting")) {
            mixins.add("enhance.bq.MixinBqTooltip");
            mixins.add("enhance.bq.MixinBqSceneTransition");
            mixins.add("enhance.bq.MixinBqPanelTextField");
            mixins.add("enhance.bq.MixinBqRenderUtils");
            mixins.add("enhance.bq.MixinBqScreenCanvas");
        }
        if (has.test("NotEnoughItems")) {
            mixins.add("enhance.nei.MixinFormattedTextFieldSelection");
            mixins.add("enhance.nei.MixinRecipeTooltipLineHandler");
        }
        if (has.test("TConstruct")) {
            mixins.add("enhance.tconstruct.MixinSmelteryGui");
        }
        if (has.test("bogosorter")) {
            mixins.add("enhance.bogo.MixinBogoHotbarSwap");
        }
        return mixins;
    }
}
