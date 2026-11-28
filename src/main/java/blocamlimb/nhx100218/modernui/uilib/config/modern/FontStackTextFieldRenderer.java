package blocamlimb.nhx100218.modernui.uilib.config.modern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import blocamlimb.nhx100218.modernui.config.schema.FieldSpec;
import blocamlimb.nhx100218.modernui.config.ui.DraftSignalAdapter;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderer;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldShellBinder;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneInputType;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneTextInput;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.CrossAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * Two plain text fields for the font family stack.
 *
 * <p>The player types a family name or font-file path directly; there is no
 * discovered-font list, add button or drag sorting. Row 1 is the primary
 * family and row 2 is the fallback family.</p>
 */
public final class FontStackTextFieldRenderer implements FieldRenderer {

    private static final List<String> DEFAULTS = Arrays.asList("Inter", "思源黑体");

    @Override
    public SceneNode render(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter) {
        final String path = spec.path();
        final ReadableSignal<Object> draft = adapter.draftSignal(path);
        List<String> initial = readValues(draft.get());
        final Signal<String> primary = Signal.create(initial.get(0));
        final Signal<String> fallback = Signal.create(initial.get(1));

        rt.bind(draft, value -> {
            List<String> next = readValues(value);
            if (!next.get(0).equals(primary.get())) {
                primary.set(next.get(0));
            }
            if (!next.get(1).equals(fallback.get())) {
                fallback.set(next.get(1));
            }
        });

        return FieldShellBinder.build(rt, spec, adapter, () -> {
            SceneNode column = SceneNode.column();
            column.setFillParentWidth(true);
            column.setGap(8);
            column.appendChild(row(rt,
                    blocamlimb.nhx100218.modernui.config.ui.ConfigText.tr("modernui.config.font.primary"),
                    primary, adapter, path, primary, fallback));
            column.appendChild(row(rt,
                    blocamlimb.nhx100218.modernui.config.ui.ConfigText.tr("modernui.config.font.fallback"),
                    fallback, adapter, path, primary, fallback));
            return column;
        });
    }

    private static SceneNode row(SceneRuntime rt, String label, Signal<String> value,
            DraftSignalAdapter adapter, String path, Signal<String> primary, Signal<String> fallback) {
        SceneNode row = SceneNode.row();
        row.setFillParentWidth(true);
        row.setCrossAxisAlign(CrossAxisAlign.CENTER);
        row.setGap(8);

        SceneNode labelNode = new SceneNode();
        labelNode.setText(label);
        labelNode.setFontSize(14);
        labelNode.setPreferredWidth(88);
        labelNode.setHitTestable(false);
        row.appendChild(labelNode);

        SceneTextInput.Props props = new SceneTextInput.Props(
                value,
                Signal.create(Boolean.TRUE),
                Signal.create(Boolean.FALSE),
                "字体家族名或字体文件路径",
                512,
                SceneInputType.TEXT,
                next -> {
                    value.set(next);
                    adapter.onFieldEdit(path, new ArrayList<String>(Arrays.asList(primary.get(), fallback.get())));
                });
        SceneNode input = SceneTextInput.create(rt, props).get();
        input.setFlexGrow(1);
        row.appendChild(input);
        return row;
    }

    private static List<String> readValues(Object value) {
        List<String> result = new ArrayList<String>(2);
        if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                result.add(entry == null ? "" : String.valueOf(entry));
            }
        }
        while (result.size() < 2) {
            result.add(DEFAULTS.get(result.size()));
        }
        return result;
    }
}
