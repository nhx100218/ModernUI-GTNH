package blocamlimb.nhx100218.modernui.uilib.mixin.early;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import cpw.mods.fml.relauncher.Side;

/**
 * Contract tests for the merged early mixin registration list.
 */
public class EarlyMixinsTest {

    private static final String ANGELICA_TWEAKER = "com.gtnewhorizons.angelica.loading.AngelicaTweaker";

    @Test
    public void shouldNotReturnClientMixinsOnDedicatedServerSide() {
        List<String> mixins = EarlyMixins.buildMixinsForSide(Side.SERVER);

        Assert.assertTrue(mixins.contains("uilib.mixin.early.network.MixinNetHandlerPlayServer"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.network.MixinNetHandlerPlayClient"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.MixinFontRenderer"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.MixinMinecraftWorldLoadPump"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.MixinGuiScreenKeyboardIsolation"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.tesr.MixinAngelicaTesrBatchReplay"));
        Assert.assertFalse(mixins.contains("enhance.mixin.early.MixinGuiNewChat"));
        assertNoNameTagMixins(mixins);
    }

    @Test
    public void shouldReturnClientAndServerNetworkMixinsOnClientSide() {
        List<String> mixins = EarlyMixins.buildMixinsForSide(Side.CLIENT);

        Assert.assertTrue(mixins.contains("uilib.mixin.early.network.MixinNetHandlerPlayClient"));
        Assert.assertTrue(mixins.contains("uilib.mixin.early.network.MixinNetHandlerPlayServer"));
        Assert.assertTrue(mixins.contains("uilib.mixin.early.MixinMinecraftWorldLoadPump"));
        Assert.assertTrue(mixins.contains("uilib.mixin.early.MixinGuiScreenKeyboardIsolation"));
        assertGenericNameTagMixins(mixins);
        Assert.assertFalse(mixins.contains("uilib.mixin.early.nametag.MixinAngelicaPlayerNameTagReplay"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.tesr.MixinAngelicaTesrBatchReplay"));
        // The merged enhancement early mixins are loaded from the same config.
        Assert.assertTrue(mixins.contains("enhance.mixin.early.MixinGuiNewChat"));
        Assert.assertTrue(mixins.contains("enhance.mixin.early.MixinGuiSlotSmoothScroll"));
    }

    @Test
    public void shouldAddAngelicaGuardOnlyForClientWithAngelicaTweaker() {
        Set<String> loadedCoreMods = Collections.singleton(ANGELICA_TWEAKER);

        List<String> clientMixins = EarlyMixins.buildMixinsForSide(Side.CLIENT, loadedCoreMods);
        assertGenericNameTagMixins(clientMixins);
        Assert.assertTrue(clientMixins.contains("uilib.mixin.early.nametag.MixinAngelicaPlayerNameTagReplay"));
        Assert.assertTrue("Angelica must install the TESR replay guard",
                clientMixins.contains("uilib.mixin.early.tesr.MixinAngelicaTesrBatchReplay"));

        List<String> serverMixins = EarlyMixins.buildMixinsForSide(Side.SERVER, loadedCoreMods);
        assertNoNameTagMixins(serverMixins);
    }

    private static void assertGenericNameTagMixins(List<String> mixins) {
        Assert.assertTrue(mixins.contains("uilib.mixin.early.nametag.MixinEntityRendererPlayerNameTagPass"));
        Assert.assertTrue(mixins.contains("uilib.mixin.early.nametag.MixinRendererLivingEntityPlayerNameTag"));
        Assert.assertTrue(mixins.contains("uilib.mixin.early.nametag.MixinRenderPlayerScoreboardNameTag"));
    }

    private static void assertNoNameTagMixins(List<String> mixins) {
        Assert.assertFalse(mixins.contains("uilib.mixin.early.nametag.MixinEntityRendererPlayerNameTagPass"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.nametag.MixinRendererLivingEntityPlayerNameTag"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.nametag.MixinRenderPlayerScoreboardNameTag"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.nametag.MixinAngelicaPlayerNameTagReplay"));
        Assert.assertFalse(mixins.contains("uilib.mixin.early.tesr.MixinAngelicaTesrBatchReplay"));
    }
}
