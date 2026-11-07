package blocamlimb.nhx100218.modernui.uilib.mixin.early;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.Side;

import blocamlimb.nhx100218.modernui.uilib.mixin.BundledMixinRuntime;

/**
 * Early mixin loader for vanilla classes. Loads both the library and the merged
 * enhancement mixins through a single coremod and a single mixin config.
 *
 * <p>The jar is a pure coremod: FML instantiates this class while it discovers
 * coremods, which is before launchwrapper constructs the Mixin tweaker. The
 * embedded runtime is installed here when no provider exists in {@code mods}.
 * The early config itself is picked up from the {@code MixinConfigs} manifest
 * attribute, so no code has to register it.</p>
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixins implements IFMLLoadingPlugin {

    private static final Logger LOG = LogManager.getLogger("ModernUI-GTNH/Mixin");

    private static final String ANGELICA_TWEAKER = "com.gtnewhorizons.angelica.loading.AngelicaTweaker";

    static {
        try {
            BundledMixinRuntime.ensureAvailable();
        } catch (Throwable t) {
            LOG.error("ModernUI-GTNH could not install its Mixin runtime", t);
        }
    }

    static List<String> buildMixinsForSide(Side side) {
        return buildMixinsForSide(side, false);
    }

    public static List<String> buildMixinsForSide(Side side, boolean angelicaPresent) {
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
            if (angelicaPresent) {
                mixins.add("nametag.MixinAngelicaPlayerNameTagReplay");
                mixins.add("tesr.MixinAngelicaTesrBatchReplay");
                mixins.add("debug.MixinAngelicaDebugTextBatched");
                mixins.add("debug.MixinAngelicaBatchingFontRenderer");
                mixins.add("debug.MixinAngelicaDebugScreenOverlay");
            }
            // Merged enhancement
            mixins.add("enhance.MixinGuiScreenTooltip");
            mixins.add("enhance.MixinGuiSlotSmoothScroll");
            mixins.add("enhance.MixinEntityRendererScreenTransition");
            mixins.add("enhance.MixinEntityRendererWorldSkip");
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
            mixins.add("motd.client.MixinServerListEntryNormal");
        }
        mixins.add("network.MixinNetHandlerPlayServer");
        mixins.add("motd.MixinNetHandlerStatusServer");
        return mixins;
    }

    /** Whether the early list contains the Angelica-only compatibility mixins. */
    public static boolean isAngelicaPresent(ClassLoader classLoader) {
        try {
            return classLoader.getResource(ANGELICA_TWEAKER.replace('.', '/') + ".class") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        // Registered directly from the static initialiser so it is active
        // before any other coremod or Mixin starts loading FML classes.
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
        // Normally UniMixins, Angelica or GTNHLib schedules a Mixin tweaker. If
        // none is present, start Mixin ourselves before the game launches.
        try {
            Class<?> registration = Class.forName(
                    "blocamlimb.nhx100218.modernui.uilib.mixin.MixinRegistration",
                    true,
                    EarlyMixins.class.getClassLoader());
            registration.getMethod("startAndRegisterEarly").invoke(null);
        } catch (Throwable t) {
            LOG.error("ModernUI-GTNH could not ensure the Mixin subsystem is running", t);
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
