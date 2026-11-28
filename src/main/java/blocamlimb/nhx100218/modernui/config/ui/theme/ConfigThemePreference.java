package blocamlimb.nhx100218.modernui.config.ui.theme;

import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneTheme;

/** Static config-screen theme signal (solid Material 3). */
public final class ConfigThemePreference {

    private static final Signal<SceneTheme> THEME = Signal.create(ConfigTheme.modernDark());

    private ConfigThemePreference() {
    }

    /**
     * Returns the config theme, rebuilt against the current density so scaled
     * corner radii follow the window resolution and GUI scale.
     */
    public static ReadableSignal<SceneTheme> signal() {
        THEME.set(ConfigTheme.modernDark());
        return THEME;
    }
}