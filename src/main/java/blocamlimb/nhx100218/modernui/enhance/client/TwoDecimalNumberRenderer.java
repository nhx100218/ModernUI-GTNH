package blocamlimb.nhx100218.modernui.enhance.client;

import java.util.Locale;
import java.util.function.Supplier;

import blocamlimb.nhx100218.modernui.config.schema.FieldSpec;
import blocamlimb.nhx100218.modernui.config.ui.DraftSignalAdapter;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderer;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderSupport;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldShellBinder;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Computed;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneInputType;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneTextInput;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * NUMBER 字段文本输入渲染器，显示固定 2 位小数（如 {@code -1.50}）。
 *
 * <p>库默认 {@code NumberFieldRenderer} 用 {@code Double.toString} 去尾零（{@code -1.5}）。
 * 这里只替换「规范显示写法」为 {@code %.2f}，编辑期原文判据仍复用
 * {@link FieldRenderSupport#isUnfinishedNumberText}，写回/校验/失焦归位与默认实现同口径。</p>
 */
public final class TwoDecimalNumberRenderer implements FieldRenderer {

    @Override
    public SceneNode render(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter) {
        final String path = spec.path();
        final ReadableSignal<Object> draftSig = adapter.draftSignal(path);

        final String initialText = format2(draftSig.get());
        final Signal<String> editText = Signal.create(initialText);
        final ReadableSignal<String> displayText = Computed.create(initialText, () -> {
            Object value = draftSig.get();
            String raw = editText.get();
            return FieldRenderSupport.isUnfinishedNumberText(raw, value) ? raw : format2(value);
        });

        SceneTextInput.Props props = new SceneTextInput.Props(
                displayText,
                Signal.create(Boolean.TRUE),
                Signal.create(Boolean.FALSE),
                "",
                Integer.MAX_VALUE,
                SceneInputType.NUMBER,
                next -> {
                    editText.set(next);
                    try {
                        adapter.onFieldEdit(path, Double.valueOf(Double.parseDouble(next)));
                    } catch (NumberFormatException e) {
                        adapter.onFieldEdit(path, next);
                    }
                });

        final Supplier<SceneNode> control = () -> {
            SceneNode input = SceneTextInput.create(rt, props).get();
            rt.bind(rt.interactionState(input).focused(), focused -> {
                if (!Boolean.TRUE.equals(focused)) {
                    editText.set(format2(draftSig.get()));
                }
            });
            return input;
        };

        return FieldShellBinder.build(rt, spec, adapter, control);
    }

    private static String format2(Object value) {
        if (value instanceof Number) {
            return String.format(Locale.ROOT, "%.2f", Double.valueOf(((Number) value).doubleValue()));
        }
        return value == null ? "" : String.valueOf(value);
    }
}
