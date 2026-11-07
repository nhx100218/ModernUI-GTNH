package blocamlimb.nhx100218.modernui.uilib.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import blocamlimb.nhx100218.modernui.uilib.mixin.early.EarlyMixins;
import blocamlimb.nhx100218.modernui.uilib.mixin.late.LateMixins;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Supplies the per-side and per-mod mixin lists to the early and late configs.
 *
 * <p>Keeping the selection here means the JSON configs stay static while the
 * runtime still decides which optional compatibility mixins are safe to apply.</p>
 */
public final class ModernUiMixinConfigPlugin implements IMixinConfigPlugin {

    private static final Logger LOG = LogManager.getLogger("ModernUI-GTNH/Mixin");

    private List<String> mixins = Collections.emptyList();

    @Override
    public void onLoad(String mixinPackage) {
        ensureMixinExtras();
        Side side = currentSide();
        if (mixinPackage != null && mixinPackage.endsWith(".late")) {
            mixins = LateMixins.allForSide(side);
        } else {
            ClassLoader loader = ModernUiMixinConfigPlugin.class.getClassLoader();
            mixins = EarlyMixins.buildMixinsForSide(side, EarlyMixins.isAngelicaPresent(loader));
        }
        LOG.info("Loaded Mixin config {} with {} mixin(s)", mixinPackage, mixins.size());
    }

    /**
     * Registers MixinExtras' injectors and extensions. UniMixins normally does
     * this, but the bundled runtime has to be self-sufficient; the call is
     * idempotent and runs once Mixin's transformer is active.
     */
    private static void ensureMixinExtras() {
        try {
            MixinExtrasBootstrap.init();
        } catch (Throwable t) {
            LOG.warn("ModernUI-GTNH could not initialise MixinExtras", t);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // Nothing to do.
    }

    @Override
    public List<String> getMixins() {
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Nothing to do.
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Nothing to do.
    }

    private static Side currentSide() {
        try {
            Side side = FMLLaunchHandler.side();
            if (side != null) {
                return side;
            }
        } catch (Throwable ignored) {
            // FML has not set the side yet; Mixin can still detect it.
        }
        try {
            MixinEnvironment.Side side = MixinEnvironment.getDefaultEnvironment().getSide();
            if (side == MixinEnvironment.Side.CLIENT) {
                return Side.CLIENT;
            }
            if (side == MixinEnvironment.Side.SERVER) {
                return Side.SERVER;
            }
        } catch (Throwable ignored) {
            // Leave the side unknown rather than guessing wrong.
        }
        return null;
    }
}
