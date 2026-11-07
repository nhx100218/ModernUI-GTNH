package blocamlimb.nhx100218.modernui.uilib.mixin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Registers the late Mixin config and provides a fallback Mixin start.
 *
 * <p>The early config is listed in the jar's {@code MixinConfigs} manifest
 * attribute, so Mixin discovers it on its own before game classes load. This
 * class is loaded from a mixin and therefore assumes Mixin is already running
 * by the time {@link #registerLate()} is called.</p>
 */
public final class MixinRegistration {

    private static final Logger LOG = LogManager.getLogger("ModernUI-GTNH/Mixin");

    public static final String LATE_CONFIG = "mixins.modernuigtnh.late.json";

    private static final String MIXIN_BOOTSTRAP = "org.spongepowered.asm.launch.MixinBootstrap";
    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";

    public static final String EARLY_CONFIG = "mixins.modernuigtnh.early.json";

    private static boolean earlyRegistered;
    private static boolean lateRegistered;

    private MixinRegistration() {}

    /**
     * Starts Mixin when no other mod supplied the standard tweaker. Safe to
     * call after {@code MixinBootstrap} has already been initialised.
     */
    public static void startIfNeeded() {
        try {
            if (!isMixinInitialised()) {
                startMixinTweaker();
            }
        } catch (Throwable t) {
            LOG.error("ModernUI-GTNH could not start the Mixin subsystem", t);
        }
    }

    /**
     * Starts Mixin when no other mod supplied the standard tweaker and registers
     * the early config. Called from the coremod during tweaker installation.
     */
    public static void startAndRegisterEarly() {
        startIfNeeded();
        registerEarly();
        registerLate();
    }

    /** Registers the early config while the Mixin subsystem is running. */
    public static void registerEarly() {
        if (earlyRegistered) {
            return;
        }
        earlyRegistered = true;
        Mixins.addConfiguration(EARLY_CONFIG);
        LOG.info("Registered early Mixin config {}", EARLY_CONFIG);
    }

    /** Registers the late compatibility config during FML's CONSTRUCTING state. */
    public static void registerLate() {
        if (lateRegistered) {
            return;
        }
        lateRegistered = true;
        Mixins.addConfiguration(LATE_CONFIG);
        LOG.info("Registered late Mixin config {}", LATE_CONFIG);
    }

    /**
     * Reads the private flag directly. Loading {@code MixinBootstrap} through
     * {@code Class.forName} is safe here: the runtime is installed already.
     */
    private static boolean isMixinInitialised() {
        try {
            Class<?> bootstrap = Class.forName(MIXIN_BOOTSTRAP, true, MixinRegistration.class.getClassLoader());
            Field initialised = bootstrap.getDeclaredField("initialised");
            initialised.setAccessible(true);
            return initialised.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Drives a real {@code MixinTweaker} when the launch chain has none. The
     * tweaker's constructor starts Mixin, {@code acceptOptions} prepares the
     * platform, and {@code injectIntoClassLoader} installs the platform agents.
     * Its {@code EnvironmentStateTweaker} is appended to the tweak list and
     * processed by launchwrapper's next pass, which also advances the phase.
     */
    private static void startMixinTweaker() throws ReflectiveOperationException {
        ClassLoader classLoader = MixinRegistration.class.getClassLoader();
        Class<?> tweakerClass = Class.forName(MIXIN_TWEAKER, true, classLoader);
        Object tweaker = tweakerClass.getDeclaredConstructor().newInstance();

        Method acceptOptions =
                tweakerClass.getMethod("acceptOptions", List.class, File.class, File.class, String.class);
        acceptOptions.invoke(tweaker, Collections.emptyList(), null, null, null);

        Method injectIntoClassLoader = tweakerClass.getMethod("injectIntoClassLoader", LaunchClassLoader.class);
        injectIntoClassLoader.invoke(tweaker, Launch.classLoader);

        LOG.info("ModernUI-GTNH started the Mixin subsystem because no tweaker provided one");
    }
}
