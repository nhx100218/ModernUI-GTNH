package blocamlimb.nhx100218.modernui.config.ui.theme;

import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.render.UiBackdrop;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormThemes;
import blocamlimb.nhx100218.modernui.uilib.ui.render.UiGlassMaterial;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.paint.SceneChromeTokens;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneSurfaceStyle;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneTheme;

/**
 * 配置页 UI 主题的「配置包内消费入口」：双路径，与 {@code FormTheme}/{@code FormThemes}
 * 的 G11 双路径同构。
 *
 * <p><b>默认路径</b>（全库液态玻璃外观）：{@link #asFormTheme(SceneRuntime)} 经
 * {@link FormThemes} 既有桥把当前来源主题（页面 {@code SceneThemes.withTheme} 局部主题 &gt;
 * runtime 默认主题 &gt; 库默认液态玻璃档）派生为 {@link FormTheme}，主题切换自动重派生；
 * 页壳语义色同样直接经 {@link blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneThemes} 的冻结便捷入口消费。
 * 本类<b>不是</b>颜色权威：不复制、不改写任何色板，只做配置包内的消费收口。</p>
 *
 * <p><b>显式路径</b>（旧语义，完整保留）：本类静态常量与无参 {@link #asFormTheme()} 保持
 * 「配置包内共享的显式旧主题」语义——固定值、不感知主题切换，供尚未迁移的调用点继续使用
 * （契约 §4.2：{@code FormTheme}/{@code ConfigTheme} 保留旧显式主题语义；新增默认解析走主题）。
 * {@code ConfigScreen} 与字段渲染器按 G15 序列在后续实例逐个转入默认路径。</p>
 *
 * <p><b>方向纪律</b>：配置包只消费 UILib 主题/模板，不反向成为主题中心——本类不得被
 * {@code ui.scene.theme}/{@code ui.scene.form} 引用，桥接逻辑一律复用既有
 * {@link FormThemes}，不新建第二份解析。</p>
 *
 * <p><b>访问说明</b>：概念上仅 config.ui 内部使用；因 Java 跨包访问限制
 * （{@code ConfigScreen} 在 {@code config.ui}，{@code FieldRenderer} 实现在
 * {@code config.ui.field}），常量需跨包可见，故类与常量设为 public。
 * 不属于对外公开 API。</p>
 */
public final class ConfigTheme {

    /** 字段卡片间距 */
    public static final int FIELD_GAP = SceneChromeTokens.GAP_MD;
    /** 输入框行高 */
    public static final int INPUT_HEIGHT = SceneChromeTokens.INPUT_HEIGHT;
    /** 按钮高度 */
    public static final int BUTTON_HEIGHT = SceneChromeTokens.BUTTON_HEIGHT;
    /** 按钮宽度 */
    public static final int BUTTON_WIDTH = 108;

    /** 全屏 Material 暗色遮罩（80% 不透明），世界画面可在其后连续透出。 */
    public static final int ROOT_BG = 0xCC1A110F;
    /** Material dark surface container。 */
    public static final int VIEWPORT_BG = 0xFF1A110F;
    /** 导航与底部操作区的 tonal surface。 */
    public static final int SURFACE_CONTAINER = 0xFF231917;
    /** 字段与状态读数的高一级 tonal surface。 */
    public static final int SURFACE_CONTAINER_HIGH = 0xFF322825;

    /** 标题文本色 */
    public static final int TITLE_COLOR = 0xFFF1DFDA;
    /** 正文文本色 */
    public static final int TEXT_COLOR = 0xFFF1DFDA;
    /** 次要文本色（helper/副标题） */
    public static final int MUTED_COLOR = 0xFFD8C2BC;
    /** 错误文本色 */
    public static final int ERROR_COLOR = 0xFFFFB4AB;
    /** 正常态文本色 */
    public static final int OK_COLOR = 0xFFA8DAB5;
    /** 脏态文本色（primary） */
    public static final int DIRTY_COLOR = 0xFFFFB5A1;
    /** 徽标底色 */
    public static final int READOUT_BG = SURFACE_CONTAINER_HIGH;

    /** 页面内容最大宽度。 */
    public static final int PAGE_MAX_WIDTH = 1120;
    /** 单列内容最大宽度。 */
    public static final int CONTENT_MAX_WIDTH = 860;
    /** 固定左侧 section navigation 宽度。 */
    public static final int NAV_PANE_WIDTH = 196;

    /** 标题条固定高度（压缩后） */
    public static final int TITLE_BAR_HEIGHT = 44;
    /** 状态摘要条固定高度（压缩后） */
    public static final int STATUS_HEIGHT = 28;
    /** 操作条固定高度（压缩后） */
    public static final int ACTION_BAR_HEIGHT = 46;
    /** save 反馈独立行固定高度（与 STATUS_HEIGHT 同档，守 grow 求解器不早退） */
    public static final int SAVE_FEEDBACK_HEIGHT = 24;
    /** 横向 Tab 导航段内边距（与 {@code SceneSegmented.SEGMENT_PADDING} 对齐，PAD_LG=10） */
    public static final int NAV_TAB_PADDING = SceneChromeTokens.PAD_LG;
    /** 横向 Tab 段标签字号（与 {@code SceneSegmented.SEG_LABEL_FONT_SIZE} 对齐） */
    public static final int NAV_TAB_FONT_SIZE = 16;
    /** 根容器内边距（压缩后，原 20） */
    public static final int ROOT_PADDING = 16;
    /** 根容器子节点间距（压缩后，原 12） */
    public static final int ROOT_GAP = 10;
    /** 滚动容器内 viewport 与 scrollbar 列间距（M3，原 0） */
    public static final int SCROLL_GAP = 3;

    /** Material fast Motion。 */
    public static final int MOTION_FAST_MS = SceneChromeTokens.MOTION_FAST_MS;
    /** Material standard Motion。 */
    public static final int MOTION_STANDARD_MS = SceneChromeTokens.MOTION_STANDARD_MS;
    /** Material emphasized Motion。 */
    public static final int MOTION_EMPHASIZED_MS = SceneChromeTokens.MOTION_EMPHASIZED_MS;

    // ===== 字号梯度 token（S1，UI 像素）=====
    /** 页标题字号（titleBar 主标题） */
    public static final int FONT_TITLE = 24;
    /** section 标题/导航字号 */
    public static final int FONT_SECTION = 18;
    /** 字段 label / 按钮文案字号 */
    public static final int FONT_LABEL = 16;
    /** helper text 字号 */
    public static final int FONT_HELPER = 13;
    /** error text 字号 */
    public static final int FONT_ERROR = 13;
    /** 按钮文案字号 */
    public static final int FONT_BUTTON = 16;
    /** 徽标字号 */
    public static final int FONT_BADGE = 12;
    /** titleBar 副标题（modId）字号 */
    public static final int FONT_SUBTITLE = 12;
    /** slider 读数字号 */
    public static final int FONT_READOUT = 14;

    /** 桥接 FormTheme 缓存实例：字段卡片相关通用 token 与 {@link FormTheme#defaultDark()} 对齐 */
    private static final FormTheme FORM_THEME = FormTheme.defaultDark();

    /**
     * 桥接获取 uilib.form 通用主题 token，供 4 个 FieldRenderer 调
     * {@link blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormFieldShell#build} 时传入。
     *
     * <p>config.ui 是 uilib.form 的适配层，主题 token 仍由本类收口，经此方法转为
     * uilib.form 的 {@link FormTheme} 形态下沉给字段外壳。</p>
     *
     * <p><b>显式旧主题</b>：返回缓存的 {@link FormTheme#defaultDark()} 固定值，
     * <b>不感知主题切换</b>（语义自 G11 前保持原样，未随默认路径迁移而改写）；
     * 需要跟随主题的默认外观请用 {@link #asFormTheme(SceneRuntime)}。</p>
     *
     * @return 深色档 FormTheme 实例（缓存，恒同一对象）
     */
    public static FormTheme asFormTheme() {
        return FORM_THEME;
    }

    /**
     * 默认路径：把当前来源 {@code SceneTheme} 经 {@link FormThemes} 既有桥派生为
     * 配置页消费的 {@link FormTheme} 信号（G15/Theme 新增，全库默认液态玻璃外观）。
     *
     * <p>解析优先级与语义色/表面映射口径全部由 {@link FormThemes#resolve(SceneRuntime)}
     * 单点定义（页面局部主题 &gt; runtime 默认 &gt; 库默认液态玻璃档）；本方法零色板、
     * 零第二套解析，只是配置包内的消费入口收口。主题切换后信号自动重派生，不重建节点。</p>
     *
     * <p><b>构造期调用</b>：与 {@link FormThemes#resolve(SceneRuntime)} 同一纪律——必须在构建期
     * （{@code mount/show/forEach/portal} 的 builder 内）调用，此时 {@code Owner.current()}
     * 是来源作用域，局部/页面主题才能被捕获；无 Owner 时经 runtime 根作用域解析并安全回落库默认。
     * 与无参 {@link #asFormTheme()} 的显式旧路径并存、互不感知。</p>
     *
     * @param rt 目标 runtime，不可为 null（null 抛 {@link NullPointerException}，不静默降级）
     * @return 表单主题只读信号（恒非 null，其值恒非 null）
     */
    public static ReadableSignal<FormTheme> asFormTheme(SceneRuntime rt) {
        return FormThemes.resolve(rt);
    }

    /** 纯常量类，禁止实例化 */
    // ModernUI default Material 3 dark palette (SystemTheme subclass 0).
    private static final int SURFACE = 0xFF1A110F;
    private static final int SURFACE_LOWEST = 0xFF140C0A;
    private static final int SURFACE_LOW = 0xFF231917;
    private static final int SURFACE_CONTAINER_TONE = 0xFF271D1B;
    private static final int SURFACE_HIGH = 0xFF322825;
    private static final int SURFACE_HIGHEST = 0xFF3D3230;
    private static final int ON_SURFACE = 0xFFF1DFDA;
    private static final int ON_SURFACE_VARIANT = 0xFFD8C2BC;
    private static final int OUTLINE = 0xFFA08C87;
    private static final int OUTLINE_VARIANT = 0xFF53433F;
    private static final int PRIMARY = 0xFFFFB5A1;
    private static final int ON_PRIMARY = 0xFF561F10;
    private static final int PRIMARY_CONTAINER = 0xFF723523;
    private static final int ON_PRIMARY_CONTAINER = 0xFFFFDBD1;
    private static final int PRIMARY_INVERSE = 0xFF8F4B39;
    private static final int SECONDARY = 0xFFE7BDB2;
    private static final int SECONDARY_CONTAINER = 0xFF5D4038;
    private static final int TERTIARY = 0xFFD9C58D;
    private static final int ERROR = 0xFFFFB4AB;
    private static final int ON_ERROR = 0xFF690005;
    private static final int ERROR_CONTAINER = 0xFF93000A;

    /** Solid ModernUI/Material 3 dark theme for the flat config page. */
    public static SceneTheme modernDark() {
        return modernTheme(false);
    }

    /** Translucent ModernUI/Material 3 dark theme for the glass config page. */
    public static SceneTheme modernGlass() {
        return modernTheme(true);
    }

    private static SceneTheme modernTheme(boolean glass) {
        SceneTheme.Builder builder = SceneTheme.builder()
                .foreground(ON_SURFACE)
                .mutedForeground(ON_SURFACE_VARIANT)
                .disabledForeground(OUTLINE)
                .onAccentForeground(ON_PRIMARY)
                .accent(PRIMARY)
                .accentHover(PRIMARY_CONTAINER)
                .accentPressed(PRIMARY_INVERSE)
                .selectionBackground(PRIMARY_CONTAINER)
                .selectionForeground(ON_PRIMARY_CONTAINER)
                .borderDefault(OUTLINE)
                .borderFocus(PRIMARY)
                .borderDisabled(OUTLINE_VARIANT)
                .danger(ERROR)
                .errorText(ERROR)
                .warningText(TERTIARY)
                .warningSubtle(0x22D9C58D);
        for (SceneTheme.Role role : SceneTheme.Role.values()) {
            builder.surface(role, glass ? glassSurface(role) : solidSurface(role));
        }
        return builder.build();
    }

    private static SceneSurfaceStyle solidSurface(SceneTheme.Role role) {
        switch (role) {
            case PANEL:
                return solid(SURFACE, SURFACE_LOW, SURFACE_LOWEST, OUTLINE_VARIANT, 12);
            case TOOLBAR:
                return solid(SURFACE_LOW, SURFACE_CONTAINER_TONE, SURFACE, OUTLINE_VARIANT, 12);
            case INPUT:
                return solid(SURFACE_LOWEST, SURFACE_LOW, SURFACE, OUTLINE, 8);
            case GROUP:
                return solid(SURFACE_CONTAINER_TONE, SURFACE_HIGH, SURFACE_LOW, OUTLINE_VARIANT, 12);
            case OVERLAY:
                return solid(SURFACE_HIGH, SURFACE_HIGHEST, SURFACE_CONTAINER_TONE, OUTLINE, 16);
            case INDICATOR:
                return indicatorSolid();
            case BUTTON_STANDARD:
                return solid(SURFACE_HIGH, SURFACE_HIGHEST, SURFACE_CONTAINER_TONE, OUTLINE_VARIANT, 20);
            case BUTTON_PRIMARY:
                return solid(PRIMARY, PRIMARY_CONTAINER, PRIMARY_INVERSE, PRIMARY, 20);
            case BUTTON_DANGER:
                return solid(ERROR, 0xFFFFDAD6, ERROR_CONTAINER, ERROR, 20);
            default:
                return solid(SURFACE_CONTAINER_TONE, SURFACE_HIGH, SURFACE_LOW, OUTLINE_VARIANT, 12);
        }
    }

    private static SceneSurfaceStyle indicatorSolid() {
        return SceneSurfaceStyle.builder()
                .backdrop(null)
                .cornerRadius(SceneTheme.PILL_RADIUS)
                .borderWidth(0)
                .focusEdge(PRIMARY)
                .idle(new SceneSurfaceStyle.StateStyle(0x005D4038, 0, 0.0F, 0.0F))
                .hovered(new SceneSurfaceStyle.StateStyle(0x335D4038, 0, 0.0F, 0.0F))
                .pressed(new SceneSurfaceStyle.StateStyle(0x225D4038, 0, 0.0F, 0.0F))
                .disabled(new SceneSurfaceStyle.StateStyle(0x005D4038, 0, 0.0F, 0.0F))
                .build();
    }
    private static SceneSurfaceStyle glassSurface(SceneTheme.Role role) {
        if (role == SceneTheme.Role.INDICATOR) {
            return SceneSurfaceStyle.builder()
                    .backdrop(UiBackdrop.liquidGlass(UiGlassMaterial.DARK_THIN, 8, 0.6F))
                    .cornerRadius(SceneTheme.PILL_RADIUS)
                    .borderWidth(0)
                    .focusEdge(PRIMARY)
                    .idle(new SceneSurfaceStyle.StateStyle(0x005D4038, 0x00E7BDB2, 0.5F, 0.85F))
                    .hovered(new SceneSurfaceStyle.StateStyle(0x335D4038, 0x66E7BDB2, 1.0F, 1.0F))
                    .pressed(new SceneSurfaceStyle.StateStyle(0x225D4038, 0x44E7BDB2, 0.0F, 0.60F))
                    .disabled(new SceneSurfaceStyle.StateStyle(0x005D4038, 0x00E7BDB2, 0.35F, 0.65F))
                    .build();
        }
        int radius = role == SceneTheme.Role.INDICATOR ? SceneTheme.PILL_RADIUS
                : role == SceneTheme.Role.OVERLAY ? 16
                : role == SceneTheme.Role.BUTTON_STANDARD || role == SceneTheme.Role.BUTTON_PRIMARY
                        || role == SceneTheme.Role.BUTTON_DANGER ? 20 : 12;
        int idle;
        int edge;
        switch (role) {
            case PANEL:
                idle = SURFACE;
                edge = OUTLINE_VARIANT;
                break;
            case TOOLBAR:
                idle = SURFACE_LOW;
                edge = OUTLINE_VARIANT;
                break;
            case INPUT:
                idle = SURFACE_LOWEST;
                edge = OUTLINE;
                break;
            case GROUP:
                idle = SURFACE_CONTAINER_TONE;
                edge = OUTLINE_VARIANT;
                break;
            case OVERLAY:
                idle = SURFACE_HIGH;
                edge = OUTLINE;
                break;
            case INDICATOR:
                idle = SECONDARY_CONTAINER;
                edge = SECONDARY;
                break;
            case BUTTON_PRIMARY:
                idle = PRIMARY;
                edge = PRIMARY;
                break;
            case BUTTON_DANGER:
                idle = ERROR;
                edge = ERROR;
                break;
            case BUTTON_STANDARD:
            default:
                idle = SURFACE_HIGH;
                edge = OUTLINE_VARIANT;
                break;
        }
        return SceneSurfaceStyle.builder()
                .backdrop(UiBackdrop.liquidGlass(UiGlassMaterial.DARK_THIN, 8, 0.6F))
                .cornerRadius(radius)
                .borderWidth(1)
                .focusEdge(PRIMARY)
                .idle(new SceneSurfaceStyle.StateStyle(withAlpha(idle, 0xCC), withAlpha(edge, 0x66), 0.5F, 0.85F))
                .hovered(new SceneSurfaceStyle.StateStyle(withAlpha(idle, 0xEE), withAlpha(edge, 0x99), 1.0F, 1.0F))
                .pressed(new SceneSurfaceStyle.StateStyle(withAlpha(idle, 0xAA), withAlpha(edge, 0x55), 0.0F, 0.60F))
                .disabled(new SceneSurfaceStyle.StateStyle(withAlpha(idle, 0x66), withAlpha(edge, 0x33), 0.35F, 0.65F))
                .build();
    }

    private static SceneSurfaceStyle solid(int idle, int hover, int pressed, int edge, int radius) {
        return SceneSurfaceStyle.builder()
                .backdrop(null)
                .cornerRadius(radius)
                .borderWidth(1)
                .focusEdge(PRIMARY)
                .idle(new SceneSurfaceStyle.StateStyle(idle, edge, 0.0F, 0.0F))
                .hovered(new SceneSurfaceStyle.StateStyle(hover, edge, 0.0F, 0.0F))
                .pressed(new SceneSurfaceStyle.StateStyle(pressed, edge, 0.0F, 0.0F))
                .disabled(new SceneSurfaceStyle.StateStyle(SURFACE_LOWEST, OUTLINE_VARIANT, 0.0F, 0.0F))
                .build();
    }

    private static int withAlpha(int argb, int alpha) {
        return ((Math.max(0, Math.min(0xFF, alpha)) << 24) | (argb & 0x00FFFFFF));
    }
    private ConfigTheme() {
    }
}
