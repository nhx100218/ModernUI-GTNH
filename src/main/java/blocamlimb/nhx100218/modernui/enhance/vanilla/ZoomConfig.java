package blocamlimb.nhx100218.modernui.enhance.vanilla;

/** Zoom（Zoomify 风格）配置。 */
public final class ZoomConfig {

    private ZoomConfig() {}

    public static volatile boolean enabled = true;
    public static volatile float factor = 4.0F;
    public static volatile boolean smooth = true;
    public static volatile float speed = 0.25F;
    /** 视角灵敏度额外减速系数（>=1，越大越慢）。 */
    public static volatile float sensitivity = 2.0F;
    /** LWJGL 键码，默认 C(46)。 */
    public static volatile int keyCode = 46;
}
