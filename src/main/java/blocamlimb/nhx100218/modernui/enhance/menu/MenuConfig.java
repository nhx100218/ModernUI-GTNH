package blocamlimb.nhx100218.modernui.enhance.menu;

/**
 * Menu and loading-screen switches.
 *
 * <p>These flags are internal for now. They stay separate from the YAML schema so a
 * failed experiment can be disabled without a config migration.</p>
 */
public final class MenuConfig {

    /** Draw ModernUI's loading screen instead of the vanilla or EFR one. */
    public static volatile boolean modernLoadingScreen = true;
    /** Draw the shared panorama behind menu screens. */
    public static volatile boolean globalPanorama = true;
    /** Use nether/end portal backgrounds while entering those dimensions. */
    public static volatile boolean dimensionPortalBackground = true;
    /** Show the join arrow on the world-list icon. */
    public static volatile boolean worldQuickJoin = true;
    /** Show join and reorder arrows on server-list icons. */
    public static volatile boolean serverListArrows = true;

    private MenuConfig() {
    }
}
