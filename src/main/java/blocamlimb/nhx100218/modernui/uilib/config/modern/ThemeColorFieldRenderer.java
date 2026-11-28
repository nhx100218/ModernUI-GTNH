package blocamlimb.nhx100218.modernui.uilib.config.modern;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import blocamlimb.nhx100218.modernui.config.schema.FieldSpec;
import blocamlimb.nhx100218.modernui.config.ui.DraftSignalAdapter;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderer;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldShellBinder;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneInputType;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneTextInput;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneCursor;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneEventType;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.CrossAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * Temporary theme-colour editor: four preset swatches plus a hex text box,
 * matching the colour-row treatment used by ModernUI's border-style section.
 */
public final class ThemeColorFieldRenderer implements FieldRenderer {

    private static final List<String> PRESETS = Arrays.asList(
            "#C2D0D6", "#E7DAE5", "#C8B9AC", "#CCDAC8");

    @Override
    public SceneNode render(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter) {
        final String path = spec.path();
        final ReadableSignal<Object> draft = adapter.draftSignal(path);
        final Signal<String> editText = Signal.create(textOf(draft.get()));

        rt.bind(draft, value -> {
            String next = textOf(value);
            if (!next.equals(editText.get())) {
                editText.set(next);
            }
        });

        return FieldShellBinder.build(rt, spec, adapter, () -> {
            SceneNode column = SceneNode.column();
            column.setFillParentWidth(true);
            column.setGap(8);

            SceneNode swatches = SceneNode.row();
            swatches.setGap(8);
            swatches.setCrossAxisAlign(CrossAxisAlign.CENTER);
            for (String preset : PRESETS) {
                SceneNode swatch = new SceneNode();
                swatch.setPreferredWidth(42);
                swatch.setPreferredHeight(24);
                swatch.setCornerRadius(6);
                swatch.setBorderWidth(1);
                swatch.setBorderColor(0xFFFFFFFF);
                swatch.setBackgroundColor(parse(preset));
                swatch.setCursor(SceneCursor.POINTER);
                rt.on(swatch, SceneEventType.CLICK, (event, context) -> {
                    editText.set(preset);
                    adapter.onFieldEdit(path, preset);
                    context.stopPropagation();
                });
                swatches.appendChild(swatch);
            }
            column.appendChild(swatches);

            SceneTextInput.Props props = new SceneTextInput.Props(
                    editText,
                    Signal.create(Boolean.TRUE),
                    Signal.create(Boolean.FALSE),
                    "#RRGGBB",
                    16,
                    SceneInputType.TEXT,
                    next -> {
                        editText.set(next);
                        adapter.onFieldEdit(path, next);
                    });
            SceneNode input = SceneTextInput.create(rt, props).get();
            input.setFillParentWidth(true);
            column.appendChild(input);
            return column;
        });
    }

    private static String textOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int parse(String value) {
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() == 8) {
                hex = hex.substring(2);
            }
            return 0xFF000000 | Integer.parseInt(hex.toLowerCase(Locale.ROOT), 16);
        } catch (RuntimeException ignored) {
            return 0xFF322825;
        }
    }
}
