package blocamlimb.nhx100218.modernui.config.ui.theme;

import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneTheme;

/** Static config-screen theme signal (solid Material 3). */
public final class ConfigThemePreference {

    private static final SceneTheme FLAT = ConfigTheme.modernDark();
    private static final Signal<SceneTheme> THEME = Signal.create(FLAT);

    private ConfigThemePreference() {
    }

    public static ReadableSignal<SceneTheme> signal() {
        return THEME;
    }
}