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
            mixins.add("uilib.mixin.late.ae2.MixinAbstractPartMonitor");
        }
        if (has.test("Waila")) {
            mixins.add("enhance.mixin.late.waila.MixinWailaOverlayRenderer");
        }
        if (has.test("CodeChickenCore")) {
            mixins.add("enhance.mixin.late.codechicken.MixinGuiDrawTooltip");
        }
        if (has.test("modularui2")) {
            mixins.add("enhance.mixin.late.modularui.MixinModularUiTooltip");
            mixins.add("enhance.mixin.late.modularui.MixinModularScreenTransition");
            mixins.add("enhance.mixin.late.modularui.MixinTextRendererNoWrap");
            mixins.add("enhance.mixin.late.modularui.MixinRichTextCompilerNoWrap");
        }
        if (has.test("chromatictooltips")) {
            mixins.add("enhance.mixin.late.ct.MixinCtTooltipDecorator");
            mixins.add("enhance.mixin.late.ct.MixinCtTextComponent");
        }
        if (has.test("chromatictooltipscompat")) {
            mixins.add("enhance.mixin.late.ct.MixinNeiTooltipComponentCompat");
        }
        if (has.test("betterquesting")) {
            mixins.add("enhance.mixin.late.bq.MixinBqTooltip");
            mixins.add("enhance.mixin.late.bq.MixinBqSceneTransition");
            mixins.add("enhance.mixin.late.bq.MixinBqPanelTextField");
            mixins.add("enhance.mixin.late.bq.MixinBqRenderUtils");
            mixins.add("enhance.mixin.late.bq.MixinBqScreenCanvas");
        }
        if (has.test("NotEnoughItems")) {
            mixins.add("enhance.mixin.late.nei.MixinFormattedTextFieldSelection");
            mixins.add("enhance.mixin.late.nei.MixinRecipeTooltipLineHandler");
        }
        if (has.test("TConstruct")) {
            mixins.add("enhance.mixin.late.tconstruct.MixinSmelteryGui");
        }
        if (has.test("bogosorter")) {
            mixins.add("enhance.mixin.late.bogo.MixinBogoHotbarSwap");
        }
        return mixins;
    }
}
