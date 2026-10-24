package blocamlimb.nhx100218.modernui.uilib.mixin.late;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import cpw.mods.fml.relauncher.Side;

/**
 * Late compatibility mixins are only loaded on the client and only for present mods.
 */
public class LateMixinsTest {

    @Test
    public void clientWithAeLoadsMonitorCompatibility() {
        assertEquals(Collections.singletonList("uilib.mixin.late.ae2.MixinAbstractPartMonitor"),
                LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("appliedenergistics2")));
        assertEquals("mixins.modernuigtnh.late.json", new LateMixins().getMixinConfig());
    }

    @Test
    public void clientWithMergedOptionalModsLoadsEnhancementMixins() {
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("Waila"))
                .contains("enhance.mixin.late.waila.MixinWailaOverlayRenderer"));
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("NotEnoughItems"))
                .contains("enhance.mixin.late.nei.MixinRecipeTooltipLineHandler"));
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("betterquesting"))
                .contains("enhance.mixin.late.bq.MixinBqTooltip"));
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("bogosorter"))
                .contains("enhance.mixin.late.bogo.MixinBogoHotbarSwap"));
    }

    @Test
    public void clientWithoutAeDoesNotLoadMonitorCompatibility() {
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.emptySet()).isEmpty());
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, Collections.singleton("ae2")).isEmpty());
        assertTrue(LateMixins.buildMixinsForSide(Side.CLIENT, null).isEmpty());
    }

    @Test
    public void dedicatedServerNeverLoadsClientCompatibilityEvenWithAe() {
        assertTrue(LateMixins.buildMixinsForSide(Side.SERVER, Collections.singleton("appliedenergistics2")).isEmpty());
        assertTrue(LateMixins.buildMixinsForSide(Side.SERVER, Collections.emptySet()).isEmpty());
        assertTrue(LateMixins.buildMixinsForSide(Side.SERVER, null).isEmpty());
    }

    @Test
    public void unknownSideDoesNotLoadClientCompatibility() {
        assertTrue(LateMixins.buildMixinsForSide(null, Collections.singleton("appliedenergistics2")).isEmpty());
    }
}
