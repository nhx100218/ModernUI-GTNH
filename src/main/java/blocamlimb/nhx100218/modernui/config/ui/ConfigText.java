package blocamlimb.nhx100218.modernui.config.ui;

import java.util.function.Function;

/** Translation hook for the headless config UI. */
public final class ConfigText {

    private static Function<String, String> sTranslator = key -> key;

    private ConfigText() {
    }

    public static void setTranslator(Function<String, String> translator) {
        sTranslator = translator == null ? key -> key : translator;
    }

    public static String tr(String key) {
        return sTranslator.apply(key);
    }
}