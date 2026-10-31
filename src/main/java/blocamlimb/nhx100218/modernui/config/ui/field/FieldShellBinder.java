package blocamlimb.nhx100218.modernui.config.ui.field;

import java.util.function.Supplier;

import blocamlimb.nhx100218.modernui.config.schema.FieldSpec;
import blocamlimb.nhx100218.modernui.config.ui.DraftSignalAdapter;
import blocamlimb.nhx100218.modernui.config.ui.theme.ConfigTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Effect;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormFieldShell;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormThemes;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.AnchorRect;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.CrossAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.LogicalBox;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.MainAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.SceneGeometry;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.Transform;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneTooltip;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/** Wraps FieldSpec/adapter signals into a field shell. */
public final class FieldShellBinder {

    private static boolean sRowMode;

    private FieldShellBinder() {
    }

    /** Config screen row mode: label/helper left, control right, no per-field card. */
    public static void setRowMode(boolean rowMode) {
        sRowMode = rowMode;
    }

    public static SceneNode build(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter,
                                  Supplier<SceneNode> controlFn) {
        if (sRowMode) {
            return buildRow(rt, spec, adapter, controlFn, -1);
        }
        String path = spec.path();
        return FormFieldShell.build(rt,
                FieldRenderSupport.labelOf(spec), spec.helper(),
                adapter.errorSignal(path), adapter.dirtySignal(path),
                controlFn);
    }

    public static SceneNode build(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter,
                                  Supplier<SceneNode> controlFn, int controlHeight) {
        if (sRowMode) {
            return buildRow(rt, spec, adapter, controlFn, controlHeight);
        }
        String path = spec.path();
        return FormFieldShell.build(rt,
                FieldRenderSupport.labelOf(spec), spec.helper(),
                adapter.errorSignal(path), adapter.dirtySignal(path),
                controlFn, controlHeight);
    }

    private static SceneNode buildRow(SceneRuntime rt, FieldSpec spec, DraftSignalAdapter adapter,
                                      Supplier<SceneNode> controlFn, int controlHeight) {
        String path = spec.path();
        ReadableSignal<FormTheme> themeSignal = FormThemes.resolve(rt);
        FormTheme theme = themeSignal.get();

        SceneNode row = SceneNode.row();
        row.setFillParentWidth(true);
        row.setPadding(theme.cardPad());
        row.setGap(theme.cardPad());
        row.setCrossAxisAlign(CrossAxisAlign.CENTER);

        SceneNode left = SceneNode.column();
        left.setFlexGrow(1);
        left.setGap(ConfigTheme.dp(2));
        left.setMainAxisAlign(MainAxisAlign.CENTER);

        SceneNode label = text(FieldRenderSupport.labelOf(spec), theme.textColor(), theme.fontLabel());
        label.setMaxTextWidth(ConfigTheme.dp(520));
        label.setMaxLines(1);
        label.setEllipsis(true);
        label.setHitTestable(true);
        rt.bindComputed(() -> Integer.valueOf(themeSignal.get().textColor()), label::setTextColor);
        left.appendChild(label);

        if (spec.helper() != null && !spec.helper().isEmpty()) {
            SceneTooltip.attach(rt, new SceneTooltip.Props(label,
                    Signal.create(spec.helper()), null, 350,
                    ConfigTheme.dp(420), 0, true));
        }

        SceneNode error = text("", theme.errorColor(), theme.fontError());
        error.setMaxTextWidth(ConfigTheme.dp(520));
        error.setMaxLines(2);
        error.setEllipsis(true);
        rt.bind(adapter.errorSignal(path), error::setText);
        rt.bindComputed(() -> Integer.valueOf(themeSignal.get().errorColor()), error::setTextColor);
        left.appendChild(error);

        SceneNode right = SceneNode.column();
        right.setPreferredWidth(ConfigTheme.dp(400));
        right.setMainAxisAlign(MainAxisAlign.CENTER);
        right.setCrossAxisAlign(CrossAxisAlign.END);
        SceneNode control = controlFn.get();
        if (control != null) {
            if (controlHeight > 0) {
                control.setPreferredHeight(controlHeight);
            }
            right.appendChild(control);
        }

        row.appendChild(left);
        row.appendChild(right);
        rt.bind(rt.layoutDoneSignal(), epoch -> Effect.untrack(() -> {
            LogicalBox viewport = rt.logicalBox().get();
            if (!viewport.isPresent()) {
                right.setTransform(Transform.translate(0.0f, 0.0f));
                return;
            }
            AnchorRect box = SceneGeometry.absoluteBox(control, 0, 0);
            int maxRight = maxRight(control, 0, 0);
            int overflow = maxRight - (viewport.widthPx() - ConfigTheme.dp(8));
            int shift = Math.min(Math.max(0, overflow),
                    Math.max(0, box.getX() - ConfigTheme.dp(8)));
            right.setTransform(Transform.translate(-shift, 0.0f));
        }));
        return row;
    }

    private static int maxRight(SceneNode node, int rootX, int rootY) {
        AnchorRect box = SceneGeometry.absoluteBox(node, rootX, rootY);
        int right = box.getX() + box.getWidth();
        for (SceneNode child : node.__getChildren()) {
            right = Math.max(right, maxRight(child, rootX, rootY));
        }
        return right;
    }
    private static SceneNode text(String value, int color, int fontSize) {
        SceneNode node = new SceneNode();
        node.setText(value);
        node.setFontSize(fontSize);
        node.setTextColor(color);
        node.setHitTestable(false);
        return node;
    }
}