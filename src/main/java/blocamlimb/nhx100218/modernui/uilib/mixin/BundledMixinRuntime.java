package blocamlimb.nhx100218.modernui.uilib.mixin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Makes a Mixin runtime available while FML is still discovering coremods.
 *
 * <p>Other mods in {@code mods} declare the standard Mixin tweaker, but the
 * launch class loader only gets their jars after FML has read their manifests,
 * and directory order is not guaranteed. This class runs from the coremod
 * static initialiser, so it makes a runtime visible to both the launch class
 * loader and its parent before any Mixin class is touched. A runtime that
 * already exists in {@code mods} is preferred; the embedded jars are only used
 * when none is installed.</p>
 *
 * <p>No class-loader exclusion is added for {@code org.spongepowered.asm.*}.
 * Mixin generates synthetic classes (for example {@code Args$N}) at runtime and
 * registers them with the launch class loader; excluding the namespace would
 * send those lookups to the parent, where the generated classes do not exist.</p>
 */
public final class BundledMixinRuntime {

    private static final Logger LOG = LogManager.getLogger("ModernUI-GTNH/Mixin");

    private static final String MIXIN_BOOTSTRAP_RESOURCE =
            "org/spongepowered/asm/launch/MixinBootstrap.class";
    private static final String MIXIN_EXTRAS_RESOURCE =
            "com/llamalad7/mixinextras/MixinExtrasBootstrap.class";

    private static final String BUNDLED_RUNTIME = "/META-INF/modernui/mixin-runtime.jar";
    private static final String BUNDLED_EXTRAS = "/META-INF/modernui/mixin-extras.jar";

    private static boolean installed;

    private BundledMixinRuntime() {}

    /** Makes a runtime visible on both class loaders before Mixin is used. */
    public static synchronized void ensureAvailable() {
        if (installed) {
            return;
        }
        installed = true;

        LaunchClassLoader classLoader = Launch.classLoader;
        if (classLoader == null) {
            LOG.warn("ModernUI-GTNH could not access the launch class loader");
            return;
        }

        ClassLoader parent = classLoader.getClass().getClassLoader();
        boolean parentHasRuntime = parent != null && hasResource(parent, MIXIN_BOOTSTRAP_RESOURCE);

        if (!hasResource(classLoader, MIXIN_BOOTSTRAP_RESOURCE)) {
            File runtime = findInstalledProvider();
            if (runtime == null) {
                runtime = extractBundled(BUNDLED_RUNTIME, "mixin-runtime");
                LOG.info("Using the Mixin runtime bundled with ModernUI-GTNH");
            } else {
                LOG.info("Using the Mixin runtime provided by {}", runtime.getName());
            }
            addToLaunchClassLoader(classLoader, runtime);
            if (!parentHasRuntime && parent != null) {
                addToParent(classLoader, runtime);
            }
        } else if (!parentHasRuntime && parent != null) {
            // FML has not added the provider jar to the parent yet (directory
            // order is not guaranteed). Mirror its cascading-tweak handling.
            File runtime = findInstalledProvider();
            if (runtime == null) {
                runtime = extractBundled(BUNDLED_RUNTIME, "mixin-runtime");
            }
            addToParent(classLoader, runtime);
            LOG.info("Exposed the Mixin runtime from {} to the launch parent", runtime.getName());
        }

        if (!hasResource(classLoader, MIXIN_EXTRAS_RESOURCE)) {
            addToLaunchClassLoader(classLoader, extractBundled(BUNDLED_EXTRAS, "mixin-extras"));
            LOG.info("Using the MixinExtras runtime bundled with ModernUI-GTNH");
        }
    }

    /**
     * Checks for a class file instead of loading the class: a failed
     * {@code Class.forName} would poison launchwrapper's invalid-class cache.
     */
    private static boolean hasResource(ClassLoader classLoader, String resource) {
        try {
            return classLoader.getResource(resource) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Looks for a jar in {@code mods} that already bundles the Mixin runtime.
     */
    private static File findInstalledProvider() {
        File home = Launch.minecraftHome;
        if (home == null) {
            return null;
        }
        File provider = findProviderJar(new File(home, "mods"));
        if (provider == null) {
            provider = findProviderJar(new File(new File(home, "mods"), "1.7.10"));
        }
        return provider;
    }

    private static File findProviderJar(File directory) {
        File[] jars = directory.listFiles();
        if (jars == null) {
            return null;
        }
        for (File jar : jars) {
            if (!jar.isFile() || !jar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                continue;
            }
            try (ZipFile zip = new ZipFile(jar)) {
                if (zip.getEntry(MIXIN_BOOTSTRAP_RESOURCE) != null) {
                    return jar;
                }
            } catch (IOException ignored) {
                // A broken jar is not a usable provider; keep looking.
            }
        }
        return null;
    }

    private static void addToLaunchClassLoader(LaunchClassLoader classLoader, File file) {
        try {
            classLoader.addURL(file.toURI().toURL());
        } catch (Exception e) {
            throw new IllegalStateException("ModernUI-GTNH could not add " + file + " to the class path", e);
        }
    }

    /**
     * Adds the jar to the loader that {@link LaunchClassLoader} uses for
     * excluded packages. In RFB that is the public {@code RfbSystemClassLoader};
     * on Java 8 it is the application class loader and needs reflection.
     */
    private static void addToParent(LaunchClassLoader classLoader, File file) {
        ClassLoader parent = classLoader.getClass().getClassLoader();
        if (parent == null) {
            throw new IllegalStateException("ModernUI-GTNH has no parent class loader for " + file);
        }
        try {
            URL url = file.toURI().toURL();
            Method addUrl;
            try {
                addUrl = parent.getClass().getMethod("addURL", URL.class);
            } catch (NoSuchMethodException missing) {
                addUrl = URLClassLoaderAccess.addUrlMethod(parent);
            }
            addUrl.invoke(parent, url);
        } catch (ReflectiveOperationException | IOException e) {
            throw new IllegalStateException("ModernUI-GTNH could not add " + file + " to " + parent, e);
        }
    }

    /** Keeps the Java 8 fallback out of the normal RFB path. */
    private static final class URLClassLoaderAccess {

        private URLClassLoaderAccess() {}

        private static Method addUrlMethod(ClassLoader parent) throws ReflectiveOperationException {
            Method method = Class.forName("java.net.URLClassLoader", true, parent)
                    .getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            return method;
        }
    }

    /**
     * Extracts an embedded runtime jar to a stable temp path. The name includes
     * the content checksum, so updating the mod never reuses stale bytecode.
     */
    private static File extractBundled(String resource, String prefix) {
        try (InputStream input = BundledMixinRuntime.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("ModernUI-GTNH is missing its embedded runtime " + resource);
            }
            byte[] bytes;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                bytes = output.toByteArray();
            }

            CRC32 checksum = new CRC32();
            checksum.update(bytes);
            File directory = new File(System.getProperty("java.io.tmpdir", "."), "modernui-gtnh");
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create " + directory);
            }
            File target = new File(
                    directory, prefix + "-" + Long.toHexString(checksum.getValue()) + "-" + bytes.length + ".jar");
            if (target.isFile() && target.length() == bytes.length) {
                return target;
            }

            File temporary = new File(directory, target.getName() + ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(bytes);
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("ModernUI-GTNH could not extract " + resource, e);
        }
    }
}
