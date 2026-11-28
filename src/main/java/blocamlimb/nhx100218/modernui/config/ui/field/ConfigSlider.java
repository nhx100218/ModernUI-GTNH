package blocamlimb.nhx100218.modernui.config.ui.field;

import blocamlimb.nhx100218.modernui.config.ui.theme.ConfigTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneSliderPrimitive;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneCursor;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneInteractionState;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.LayoutBox;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.Transform;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * Material 3 / ModernUI style slider used by the configuration rows.
 *
 * <p>The shared {@link blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneSlider}
 * keeps the generic theme surface. The configuration page instead draws the
 * ModernUI seek bar look: a thin outlined groove, a pill-shaped filled track,
 * and a tall vertical capsule thumb. Geometry is expressed in config density
 * units so the 2420x1668 reference layout stays unchanged while smaller or
 * larger windows scale proportionally.</p>
 */
public final class ConfigSlider {

    /** Inactive groove. */
    private static final int TRACK = 0xFF4C3D38;
    /** Hovered groove. */
    private static final int TRACK_HOVER = 0xFF574640;
    /** Pressed groove. */
    private static final int TRACK_PRESSED = 0xFF604D46;
    /** Black-ish groove outline. */
    private static final int TRACK_EDGE = 0xFF120C0A;
    /** Active track and thumb. */
    private static final int ACCENT_FILL = 0xFFF3C8BE;
    /** Thumb outline keeps the capsule readable on the groove. */
    private static final int THUMB_EDGE = 0xFF2B1D19;
    /** Disabled active track. */
    private static final int DISABLED_FILL = 0xFF6A5A55;

    private ConfigSlider() {
    }

    /**
     * Builds a styled horizontal slider.
     *
     * @param rt       scene runtime
     * @param value    controlled value signal
     * @param enabled  enabled signal
     * @param min      minimum value
     * @param max      maximum value
     * @param step     quantization step, {@code <= 0} for continuous
     * @param onChange value change callback
     * @return slider root node
     */
    public static SceneNode create(SceneRuntime rt, ReadableSignal<Double> value,
                                   ReadableSignal<Boolean> enabled, double min, double max,
                                   double step, SceneSliderPrimitive.SliderChange onChange) {
        SceneSliderPrimitive.Result result = SceneSliderPrimitive.create(rt,
                new SceneSliderPrimitive.Props(value, enabled, min, max, step, onChange));

        SceneNode root = result.root();
        root.setPreferredWidth(ConfigTheme.dp(200));
        root.setPreferredHeight(ConfigTheme.dp(30));
        root.setClipChildren(false);

        int trackHeight = ConfigTheme.dp(8);
        int fillHeight = ConfigTheme.dp(5);
        int thumbWidth = ConfigTheme.dp(4);
        int thumbHeight = ConfigTheme.dp(28);

        SceneNode track = result.track();
        track.setFillParentWidth(true);
        track.setPreferredHeight(trackHeight);
        track.setCornerRadius(Math.max(1, trackHeight / 2));
        track.setBorderWidth(1);
        track.setBorderColor(TRACK_EDGE);
        track.setBackgroundColor(TRACK);
        rt.bind(enabled, on -> track.setCursor(Boolean.TRUE.equals(on)
                ? SceneCursor.POINTER : SceneCursor.NOT_ALLOWED));

        SceneInteractionState interaction = result.interaction();
        interaction.hovered();
        interaction.focused();
        rt.bindComputed(() -> {
            if (!Boolean.TRUE.equals(enabled.get())) {
                return Integer.valueOf(TRACK);
            }
            if (Boolean.TRUE.equals(interaction.pressed().get())) {
                return Integer.valueOf(TRACK_PRESSED);
            }
            if (Boolean.TRUE.equals(interaction.hovered().get())) {
                return Integer.valueOf(TRACK_HOVER);
            }
            return Integer.valueOf(TRACK);
        }, track::setBackgroundColor);

        SceneNode fill = result.fillBox();
        fill.setPreferredHeight(fillHeight);
        fill.setCornerRadius(Math.max(1, fillHeight / 2));
        rt.bindComputed(() -> Boolean.TRUE.equals(enabled.get())
                ? Integer.valueOf(ACCENT_FILL) : Integer.valueOf(DISABLED_FILL),
                fill::setBackgroundColor);

        // Move the thumb out of the groove so the track can stay thin while
        // the capsule keeps Material 3's 28dp height (the reference look).
        SceneNode thumb = result.thumb();
        track.removeChild(thumb);
        root.appendChild(thumb);
        thumb.setPreferredWidth(thumbWidth);
        thumb.setPreferredHeight(thumbHeight);
        thumb.setCornerRadius(Math.max(1, thumbWidth / 2));
        thumb.setBackgroundColor(ACCENT_FILL);
        thumb.setBorderWidth(1);
        thumb.setBorderColor(THUMB_EDGE);
        thumb.setHitTestable(false);
        rt.bindComputed(() -> {
            rt.layoutDoneSignal().get();
            Object trackBox = track.getCachedLayout();
            Object rootBox = root.getCachedLayout();
            int trackWidth = trackBox instanceof LayoutBox ? ((LayoutBox) trackBox).getWidth() : 0;
            int rootWidth = rootBox instanceof LayoutBox ? ((LayoutBox) rootBox).getWidth() : trackWidth;
            if (trackWidth <= 0 || rootWidth <= 0) {
                return Transform.translate(0.0F, 0.0F);
            }
            double progress = Math.max(0.0D, Math.min(1.0D, result.progress().get()));
            int travel = Math.max(0, rootWidth - thumbWidth);
            int x = (int) Math.round(travel * progress);
            return Transform.translate(-trackWidth + x, 0.0F);
        }, thumb::setTransform);

        // The track fills the row, so the filled segment has to be measured
        // from the real laid-out width rather than a fixed 200px constant.
        rt.bindComputed(() -> {
            rt.layoutDoneSignal().get();
            Object cached = track.getCachedLayout();
            if (!(cached instanceof LayoutBox)) {
                return Integer.valueOf(1);
            }
            int width = ((LayoutBox) cached).getWidth();
            if (width <= 0) {
                return Integer.valueOf(1);
            }
            int travel = Math.max(0, width - thumbWidth);
            int raw = (int) Math.round(travel * result.progress().get()) + thumbWidth / 2;
            return Integer.valueOf(Math.max(1, Math.min(width, raw)));
        }, fill::setPreferredWidth);

        return root;
    }
}
