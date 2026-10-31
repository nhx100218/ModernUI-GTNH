package blocamlimb.nhx100218.modernui.config.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import blocamlimb.nhx100218.modernui.config.runtime.ConfigManager;
import blocamlimb.nhx100218.modernui.config.runtime.DraftBuffer;
import blocamlimb.nhx100218.modernui.config.runtime.SaveOutcome;
import blocamlimb.nhx100218.modernui.config.schema.ConfigSchema;
import blocamlimb.nhx100218.modernui.config.schema.FieldSpec;
import blocamlimb.nhx100218.modernui.config.schema.SectionSpec;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRenderer;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldShellBinder;
import blocamlimb.nhx100218.modernui.config.ui.field.FieldRendererRegistry;
import blocamlimb.nhx100218.modernui.config.ui.theme.ConfigTheme;
import blocamlimb.nhx100218.modernui.config.ui.theme.ConfigThemePreference;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormActionBar;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormPageShell;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.form.FormTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.env.UiEnvironment;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.host.AbstractSceneHostWidget;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.host.SceneHostAssembly;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Computed;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Effect;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Owner;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.ReadableSignal;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.MountHandle;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneButton;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneButtonVariant;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.control.SceneScrollbar;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.image.SceneImageSource;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneCursor;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneEventType;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.PlatformInputSource;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.CrossAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.LayoutBox;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.MainAxisAlign;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.layout.SceneGeometry;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneInteractionState;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.node.Transform;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.overlay.SceneOverlayHost;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneSurfaceBinder;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneSurfaceStyle;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneTheme;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.theme.SceneThemes;

/**
 * 配置页 UI 骨架，extends {@link AbstractSceneHostWidget}。
 *
 * <p>Material settings page 结构；section 使用专用 Owner 单槽，并在完整布局发布后执行
 * 标题/字段 presentation shell 的满 opacity 级联进入：</p>
 * <pre>
 * mask (root: COLUMN, fillParent, translucent bg=ROOT_BG——专用世界遮罩，只负责遮罩)
 *   └ pageRoot (FormPageShell PANEL 玻璃页壳: COLUMN, centered, fillParentHeight)
 *       ├ titleBar        schema.title() + modId（主题语义色）
 *       ├ statusSummary   dirty/error 状态（主题语义色徽标）
 *       ├ bodyRow         多 section 时固定左侧 navigation + PANEL 玻璃 viewport
 *       │   ├ navPane     SceneNavList（自身已装 TOOLBAR 底座）
 *       │   └ scrollContainer
 *       │       ├ viewport → content → active section Setting Rows
 *       │       └ scrollbarColumn
 *       ├ saveFeedbackBar (按状态懒挂载，GROUP 玻璃底座)
 *       └ actionBar       FormActionBar 默认路径（TOOLBAR 玻璃底座），底部固定
 * </pre>
 *
 * <h3>G15/Shell 液态玻璃迁移要点</h3>
 * <ul>
 *   <li>页壳/操作条走 theme-aware 无 {@code FormTheme} 参重载：表面（染色/边框/圆角/
 *       浮雕/滤镜）由 {@link SceneSurfaceBinder} 独占、按来源主题 PANEL/TOOLBAR 配方逐项绑定，
 *       主题切换只重派生、不重建节点、不丢草稿/分类/滚动。</li>
 *   <li>语义色经 {@link ConfigTheme#asFormTheme(SceneRuntime)} 默认路径信号派生
 *       （title/muted/text/error/dirty），不用 {@code .get()} 快照喂显式 build。</li>
 *   <li>{@code ROOT_BG} 专用世界遮罩与玻璃分层：遮罩是独立外层节点、静态专用语义，
 *       不装玻璃配方；绘制顺序上先于（位于）玻璃表面，不遮挡玻璃采样。</li>
 *   <li>状态徽标按契约 §7.3「明确不迁移」保留显式底色/几何，仅状态描边色随主题语义色。</li>
 * </ul>
 *
 * <h3>项2/3 布局语义</h3>
 * <ul>
 *   <li>navPane 固定不参与滚动；0/1 section 时省略。</li>
 *   <li>actionBar 在滚动容器外并固定于页面底部，save/cancel/restore 始终可见。</li>
 *   <li>标题、状态、body 与 action bar 都居中并受页面最大宽度约束。</li>
 * </ul>
 *
 * <h3>关键守不变量</h3>
 * <ul>
 *   <li>signal-first：导航目标与 saveFeedback 经 signal 驱动；Motion phase 不写事务历史</li>
 *   <li>生命周期纪律：section 单槽严格先 dispose outgoing Owner 再 mount incoming</li>
 *   <li>未激活 section 子树不参与布局/绘制</li>
 *   <li>handler 边界：导航点击 handler 只 activeSectionSignal.set，不直接改 SceneNode</li>
 * </ul>
 *
 * <h3>按钮回调</h3>
 * <ul>
 *   <li>保存 → {@code mgr.save(draft)} + {@code adapter.afterSaveSync()} + 写 saveFeedbackSignal</li>
 *   <li>取消 → {@code adapter.resetToCurrent()}</li>
 *   <li>恢复默认 → 按 {@link FieldRestorePolicy} 逐字段跳过、自定义或 {@code resetFieldToDefault(path)}</li>
 * </ul>
 *
 * <p>构造器接受可为 null 的 {@link PlatformInputSource}（headless 测试传 null）。
 * 逻辑与渲染解耦：构造器只建 signal 树和 mount，构造末尾 flush 让所有 Computed 物化。</p>
 */
public class ConfigScreen extends AbstractSceneHostWidget {

    /** bodyRow 横向间距 */
    private static final int BODY_ROW_GAP = 12;
    /** 标题与字段 presentation shell 使用 emphasized 级联进入。 */
    private static final int SECTION_REVEAL_MS = ConfigTheme.MOTION_EMPHASIZED_MS;
    /** 从上方明显落位；全程满 opacity，不制造白/透明闪帧。 */
    private static final float SECTION_REVEAL_START_OFFSET_Y = -22.0f;
    /** 相邻标题/卡片的启动间隔。 */
    private static final int SECTION_REVEAL_ITEM_DELAY_MS = 36;
    /** 长 section 的最大级联窗口，避免尾项等待数秒。 */
    private static final int SECTION_REVEAL_MAX_DELAY_MS = 252;
    /** 配置主视口滚轮平滑收敛时长。 */
    private static final int SCROLL_MOTION_MS = ConfigTheme.MOTION_STANDARD_MS;
    /** 玻璃底座恒定启用（页壳/反馈条无禁用态），供表面绑定器判定状态档。 */
    private static final ReadableSignal<Boolean> ALWAYS_ENABLED = () -> Boolean.TRUE;

    private static SceneImageSource[] sNavIcons;
    /** MD3 top-level pages: general, interface, advanced. */
    private static final String[] GROUP_TITLES = {
            "modernui.config.group.general", "modernui.config.group.interface", "modernui.config.group.advanced"
    };
    private static final String[][] GROUP_SECTION_IDS = {
            {"tooltip", "waila", "chat"},
            {"smoothscroll", "screentransition", "zoom", "ui"},
            {"general", "textselection", "emoji", "performance"}
    };

    /** 配置管理器，保存事务入口 */
    private final ConfigManager manager;
    /** 草稿 signal 适配器 */
    private final DraftSignalAdapter adapter;
    /** 字段渲染器注册表 */
    private final FieldRendererRegistry registry;
    /** 关联的 schema */
    private final ConfigSchema schema;
    /** 恢复默认字段策略，可为 null（全部走默认恢复） */
    private final FieldRestorePolicy restorePolicy;

    /** 场景树根节点：专用世界遮罩（静态 {@code ROOT_BG}，只负责遮罩，不装玻璃）。 */
    private SceneNode root;
    /** {@link FormPageShell} 玻璃页壳根节点（PANEL 配方表面）；root 的唯一子。 */
    private SceneNode pageRoot;
    /** 滚动视口节点 */
    private SceneNode viewport;
    /** 视口内容容器节点（任一时刻仅一个 live panel） */
    private SceneNode content;
    /** 纵向滚动受控源（per-section：每个 section 独立保持滚动位置，切换不丢失） */
    private Signal<Integer>[] sectionScrolls;
    /** 当前 live section 的动态滚动显示源，clamp 到当前 maxScroll 并订阅 layoutDoneSignal。 */
    private ReadableSignal<Integer> activeScroll;
    /** 滚动条/编程式滚动写入回调（clamp 后写 authority 并立即定位）。 */
    private Consumer<Integer> setScroll;
    /** 滚轮平滑滚动的 keyed Motion。 */
    private final Object scrollMotionKey = new Object();
    private final Object autosaveKey = new Object();
    private boolean autosavePending;
    /** 输入 handler 只递增 intent；同帧 flush 再应用 Motion/节点，守 signal-first。 */
    private final Signal<Integer> scrollIntentRevision = Signal.create(Integer.valueOf(0));
    /** 同帧连续输入合并后的目标。 */
    private int pendingScrollTarget;
    /** 是否有尚未 flush 的滚动意图。 */
    private boolean scrollIntentPending;
    /** pending intent 是否应走平滑滚轮轨道；false 表示 scrollbar 直接接管。 */
    private boolean pendingScrollSmooth;
    /** 单调 intent 序号，避免相同 offset 的直接接管被 Signal 相等合并。 */
    private int scrollIntentRevisionCounter;
    /** 当前是否正在把 viewport 从显示值收敛到滚轮目标。 */
    private boolean smoothScrollRunning;
    /** 当前滚轮动画最终目标，用于忽略 authority bind 对同一目标的立即回写。 */
    private int smoothScrollTarget;
    /** 标题条节点 */
    private SceneNode titleBar;
    /** 状态摘要条节点 */
    /** 操作条节点 */
    private SceneNode actionBar;
    /** 固定左侧导航根节点；0/1 section 时为 null。 */
    private SceneNode navRoot;
    /** 多 section 时的 navigation + content 双栏节点。 */
    private SceneNode bodyRow;
    /** 滚动容器（ROW：viewport + scrollbar 列），承载 viewport 并在其右侧叠加滚动条 */
    private SceneNode scrollContainer;
    /** 滚动条列节点（scrollContainer 内 viewport 右侧的独立列） */
    private SceneNode scrollbarColumn;
    /** 滚动条 thumb，滚轮 Motion 与内容使用同一进度同步位置。 */
    private SceneNode scrollbarThumb;

    /** 导航请求的 section 下标（受控源），selection indicator 立即跟随。 */
    private Signal<Integer> activeSectionSignal;
    private Signal<Boolean> dashboardSignal;
    /** schema section 快照，供单槽 panel 切换挂载。 */
    private List<SectionSpec> sections;
    /** Grouped sections for the MD3 shell. */
    private List<GroupSpec> groups;
    /** Top tab host inside the MD3 shell. */
    private SceneNode topTabsHost;
    /** 单 live section 的专用 Owner；每次切换严格先 dispose outgoing 再 mount incoming。 */
    private Owner sectionOwner;
    /** 当前单槽 panel 的 mount 句柄。 */
    private MountHandle sectionMount;
    /** 当前 live section 下标。 */
    private int displayedSectionIndex;

    /** UI 构造作用域，所有 Computed/Effect 归属此 Owner，dispose 时统一回收 */
    private final Owner uiOwner = new Owner();

    /**
     * 页壳语义色来源：{@link ConfigTheme#asFormTheme(SceneRuntime)} 默认路径信号
     * （G15/Theme 交付的桥，构建期在 uiOwner 作用域内解析一次并持有；构造期外只读该信号，
     * 主题切换自动重派生，不持任何 {@code .get()} 快照）。
     */
    private ReadableSignal<FormTheme> formTheme;

    /** 最近一次保存结果，供测试探针 */
    private SaveOutcome lastSaveOutcome;
    /** 操作请求经三跳 effect 晚于字段终态收敛执行，避免同帧 UP→Save/Cancel 读取旧 Draft。 */
    private final Signal<ActionRequest> actionRequested = Signal.create(null);
    private final Signal<ActionRequest> actionSettling = Signal.create(null);
    private final Signal<ActionRequest> actionReady = Signal.create(null);
    /** signal 合并只负责唤醒；FIFO 队列保留同帧每次 CLICK 的顺序。 */
    private final Deque<ActionRequest> actionQueue = new ArrayDeque<ActionRequest>();
    private int actionRevision;

    private static final class GroupSpec {
        private final String title;
        private final List<Integer> sectionIndices = new ArrayList<Integer>();

        private GroupSpec(String title) {
            this.title = title;
        }
    }
    private enum ActionKind {
        RESTORE,
        CANCEL,
        SAVE
    }

    private static final class ActionRequest {
        private final ActionKind kind;
        private final int revision;

        private ActionRequest(ActionKind kind, int revision) {
            this.kind = kind;
            this.revision = revision;
        }
    }

    /**
     * 创建配置页 UI 骨架。
     *
     * @param input    平台输入源，可为 null（headless 测试）
     * @param manager  配置管理器
     * @param adapter  草稿 signal 适配器
     * @param registry 字段渲染器注册表
     */
    public ConfigScreen(PlatformInputSource input, ConfigManager manager,
                        DraftSignalAdapter adapter, FieldRendererRegistry registry) {
        this(input, manager, adapter, registry, null);
    }

    /**
     * 创建配置页 UI 骨架。
     *
     * @param input         平台输入源，可为 null（headless 测试）
     * @param manager       配置管理器
     * @param adapter       草稿 signal 适配器
     * @param registry      字段渲染器注册表
     * @param restorePolicy 恢复默认字段策略，可为 null（全部走默认恢复）
     */
    public ConfigScreen(PlatformInputSource input, ConfigManager manager,
                        DraftSignalAdapter adapter, FieldRendererRegistry registry,
                        FieldRestorePolicy restorePolicy) {
        this(input, manager, adapter, registry, restorePolicy, ConfigThemePreference.signal());
    }

    /**
     * 携带 runtime 默认主题的构造入口（包内测试接缝，不扩公共 API）。
     *
     * <p>{@code runtimeTheme} 非 null 时在页壳树构建前经契约冻结入口
     * {@link SceneThemes#install(SceneRuntime, ReadableSignal)} 安装为 runtime 默认主题，
     * 使「主题切换只重派生外观、不丢草稿/分类/滚动」可被确定性验证（P-04 口径要求切换档
     * 配方值真的不同）。安装的不是第二套解析——恰是契约 §2.4 唯一安装路径；null 时不安装，
     * 回落库默认主题；公共构造器则安装配置页偏好信号（默认平面档）。</p>
     *
     * @param input        平台输入源，可为 null（headless 测试）
     * @param manager      配置管理器
     * @param adapter      草稿 signal 适配器
     * @param registry     字段渲染器注册表
     * @param restorePolicy 恢复默认字段策略，可为 null
     * @param runtimeTheme runtime 默认主题信号，可为 null（不安装）
     */
    ConfigScreen(PlatformInputSource input, ConfigManager manager,
                 DraftSignalAdapter adapter, FieldRendererRegistry registry,
                 FieldRestorePolicy restorePolicy, ReadableSignal<SceneTheme> runtimeTheme) {
        this(input, SceneHostAssembly.defaultEnvironment(), manager, adapter, registry, restorePolicy,
                runtimeTheme);
    }

    /**
     * 环境可注入构造（主题走配置页偏好信号，与公共构造同路）。
     *
     * @param input         平台输入源，可为 null（headless 测试）
     * @param environment   宿主环境端口，不可为 null
     * @param manager       配置管理器
     * @param adapter       草稿 signal 适配器
     * @param registry      字段渲染器注册表
     * @param restorePolicy 恢复默认字段策略，可为 null
     */
    ConfigScreen(PlatformInputSource input, UiEnvironment environment, ConfigManager manager,
                 DraftSignalAdapter adapter, FieldRendererRegistry registry,
                 FieldRestorePolicy restorePolicy) {
        this(input, environment, manager, adapter, registry, restorePolicy, ConfigThemePreference.signal());
    }

    /**
     * 环境可注入构造：宿主环境事实由调用方给定，不回落生产单例。
     *
     * <p>存在理由与 {@code AbstractSceneHostWidget(PlatformInputSource, UiEnvironment)} 同源——
     * 环境事实是构造依赖，任何宿主都不该把「读到的是生产进程状态」写成不可替换的事实。
     * 配置页此前只走单参 {@code super(input)}，于是 headless 出图矩阵无法注入请求级诊断域，
     * 而这一处不受 headless 包的环境门禁覆盖（门禁只扫 headless 生产包自身）。</p>
     *
     * @param input         平台输入源，可为 null（headless 测试）
     * @param environment   宿主环境端口，不可为 null；无环境事实传 {@link UiEnvironment#empty()}
     * @param manager       配置管理器
     * @param adapter       草稿 signal 适配器
     * @param registry      字段渲染器注册表
     * @param restorePolicy 恢复默认字段策略，可为 null
     * @param runtimeTheme  runtime 默认主题信号，可为 null（不安装）
     */
    ConfigScreen(PlatformInputSource input, UiEnvironment environment, ConfigManager manager,
                 DraftSignalAdapter adapter, FieldRendererRegistry registry,
                 FieldRestorePolicy restorePolicy, ReadableSignal<SceneTheme> runtimeTheme) {
        super(input, environment);
        String densityOverride = System.getProperty("modernui.config.density");
        if (densityOverride != null) {
            try {
                ConfigTheme.setDensity(Float.parseFloat(densityOverride));
            } catch (NumberFormatException ignored) {
            }
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager must not be null");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("adapter must not be null");
        }
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        DraftBuffer ownedDraft = adapter.draft();
        if (ownedDraft == null || !manager.owns(ownedDraft)) {
            throw new IllegalArgumentException(
                    "ConfigScreen requires manager.owns(adapter.draft()); "
                            + "use the same ConfigManager that opened the draft");
        }
        this.manager = manager;
        this.adapter = adapter;
        this.registry = registry;
        this.schema = adapter.draft().schema();
        this.restorePolicy = restorePolicy;
        adapter.setOnFieldEdited(this::scheduleAutosave);
        if (runtimeTheme != null) {
            SceneThemes.install(runtime, runtimeTheme);
        }
        runtime.setDensity(ConfigTheme.density());
        runtime.__enableMotion();

        // 在 uiOwner 作用域内构造，所有 Computed/Effect 归属 uiOwner，dispose 时统一回收
        uiOwner.run(() -> {
            bindActionPipeline();

            // 页壳语义色的唯一来源：构建期经 G15/Theme 默认路径桥解析一次并持有信号
            // （禁止 .get() 快照喂显式 build；见 ConfigTheme 类头双路径文档）。
            this.formTheme = ConfigTheme.asFormTheme(runtime);

            // 用 FormPageShell.build 的 theme-aware 无 FormTheme 参重载构建统一口径骨架
            // （页壳 root/viewport 表面按来源主题 PANEL 配方由 SceneSurfaceBinder 独占绑定，
            // 圆角/边框/浮雕/滤镜全部归配方，主题切换只重派生、不重建节点）。
            // attachScroll=false：shell 不建 scrollSignal/scrollbar（ConfigScreen 自建 per-section）。
            // buildTitleBar=false：跳过 shell 标题条构造（shell 的 text 不设字号，无法满足
            // FONT_TITLE/FONT_SUBTITLE 需求），ConfigScreen 自建 createTitleBar 直接挂页壳。
            // 专用世界遮罩与玻璃分层：遮罩是页壳外层的独立节点（静态 ROOT_BG，不装玻璃），
            // 绘制顺序先于玻璃 BACKDROP，只负责遮罩、不遮挡玻璃采样（G15 验证条款）。
            FormPageShell.Parts parts = FormPageShell.build(runtime,
                    schema.title(), null,
                    ConfigTheme.TITLE_BAR_HEIGHT, ConfigTheme.ROOT_PADDING, ConfigTheme.ROOT_GAP,
                    14, 14,
                    false, false);
            this.pageRoot = parts.root();
            this.viewport = parts.viewport();
            this.scrollContainer = parts.scrollContainer();
            this.root = SceneNode.column();
            root.setBackgroundColor(ConfigTheme.ROOT_BG);
            root.setFillParentWidth(true);
            root.setFillParentHeight(true);
            pageRoot.setFillParentWidth(true);
            root.appendChild(pageRoot);
            pageRoot.setCrossAxisAlign(CrossAxisAlign.CENTER);
            viewport.setCrossAxisAlign(CrossAxisAlign.CENTER);
            scrollContainer.setGap(ConfigTheme.SCROLL_GAP);

            // shell.build 在 buildTitleBar=false 时只挂了 scrollContainer 到页壳根；ConfigScreen 需特化：
            // 1) titleBar 字号 shell 不支持 → 自建 createTitleBar 挂页壳首位
            // 2) 多 section 要把 scrollContainer 挂进统一侧栏 bodyRow → 摘下重挂
            pageRoot.removeChild(scrollContainer);

            this.titleBar = createTitleBar();
            pageRoot.appendChild(titleBar);

            // 操作条固定在页面底部，保存始终位于视觉终点且不随内容滚动。


            this.sections = schema.sections();
            this.groups = createGroups(this.sections);
            int initialSection = groups.isEmpty() ? 0
                    : groups.get(0).sectionIndices.get(0).intValue();
            this.activeSectionSignal = Signal.create(Integer.valueOf(initialSection));
            this.dashboardSignal = Signal.create(Boolean.FALSE);
            this.displayedSectionIndex = initialSection;
            this.sectionOwner = uiOwner.createChild();

            if (sections.size() > 1) {
                this.topTabsHost = createTopTabs();
                titleBar.appendChild(topTabsHost);
                titleBar.setHitTestable(true);
                titleBar.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT + ConfigTheme.dp(8));
            }

            this.content = createContent();
            viewport.appendChild(content);
            renderFields(sections);
            // 导航请求立即替换单槽；新 Owner 自带 layout-ready 级联，快速重选会自动取消旧序列。
            runtime.bind(activeSectionSignal, this::requestSectionTransition);
            // 滚动容器（ROW：viewport + scrollbar 列），承载 viewport 并在其右侧叠加滚动条。
            // navPane 与底部 actionBar 都在滚动容器外；scrollContainer 仅承载 viewport + scrollbar。
            // viewport 已由 shell.build 挂入 scrollContainer，此处不再重复 appendChild。
            if (groups.size() > 1) {
                // Material settings page：section 数量不再改变导航模式，统一使用固定左侧导航。
                this.bodyRow = SceneNode.row();
                bodyRow.setFillParentHeight(true);
                bodyRow.setFillParentWidth(true);
                bodyRow.setMaxWidth(ConfigTheme.PAGE_MAX_WIDTH);
                bodyRow.setGap(ConfigTheme.dp(BODY_ROW_GAP));
                this.navRoot = createSidebarNav(bodyRow, groups);
                scrollContainer.setFlexGrow(1);
                bodyRow.appendChild(scrollContainer);
                pageRoot.appendChild(bodyRow);
                runtime.bind(dashboardSignal, active -> {
                    boolean on = Boolean.TRUE.equals(active);
                    content.setCollapsed(on);
                    titleBar.setCollapsed(on);
                });
                rt().show(viewport, dashboardSignal, this::createDashboard);
            } else {
                // 0 或 1 section：无需导航，直接挂 scrollContainer
                scrollContainer.setFillParentWidth(true);
                scrollContainer.setMaxWidth(ConfigTheme.CONTENT_MAX_WIDTH);
                pageRoot.appendChild(scrollContainer);
            }


            // ===== BUG2 修复：per-section scroll state（section 切换不丢失滚动位置）=====
            // 每个 section 独立持有一个 Signal<Integer>，切换 section 时显示源切到对应 signal，
            // 切回时恢复原滚动位置。动态显示源 clamp 到当前 maxScroll 并订阅 layoutDoneSignal，
            // 写入回调始终写当前 live section 的 signal。
            @SuppressWarnings("unchecked")
            Signal<Integer>[] scrolls = new Signal[sections.size()];
            for (int i = 0; i < sections.size(); i++) {
                scrolls[i] = Signal.create(Integer.valueOf(0));
            }
            this.sectionScrolls = scrolls;

            // 不用 Computed 缓存 displayedSectionIndex：Motion phase 是非 signal 状态，动态 get 可确保
            // 滚轮 handler 与属性 bind 始终读取当前 live panel；layoutDoneSignal 仍负责布局后重跑 clamp。
            this.activeScroll = new ReadableSignal<Integer>() {
                @Override
                public Integer get() {
                    int idx = displayedSectionIndex;
                    if (idx < 0 || idx >= scrolls.length) {
                        return Integer.valueOf(0);
                    }
                    int raw = scrolls[idx].get().intValue();
                    runtime.layoutDoneSignal().get();
                    Object cached = viewport.getCachedLayout();
                    if (!(cached instanceof LayoutBox)) {
                        return Integer.valueOf(Math.max(0, raw));
                    }
                    int maxScroll = SceneGeometry.maxScrollY(viewport);
                    return Integer.valueOf(Math.max(0, Math.min(maxScroll, raw)));
                }
            };
            // 输入 handler 只写 intent signal；flush effect 决定平滑滚轮或 scrollbar 直接定位。
            this.setScroll = this::requestDirectScroll;
            runtime.bind(scrollIntentRevision, ignored -> applyPendingScrollIntent());
            runtime.bind(activeScroll, value -> {
                int target = value.intValue();
                if ((!smoothScrollRunning || target != smoothScrollTarget)
                        && viewport.getScrollOffsetY() != target) {
                    snapScrollOffset(target);
                }
            });
            runtime.on(viewport, SceneEventType.SCROLL, (event, context) -> {
                if (smoothScrollByWheelDelta(event.getWheelDelta())) {
                    context.stopPropagation();
                }
            });
            // 项4：滚动条叠加在 viewport 右侧（scrollContainer ROW 内 viewport 旁的独立列），
            // 反映滚动位置/可滚动范围。几何由 bind 派生（订阅 activeScroll + rt.layoutDoneSignal），
            // 守「滚动只标 COMPOSITE 零重排 / handler 只写 signal / 失效级别由 setter 自决」。P0：scrollbar 内部直接订阅 rt.layoutDoneSignal()——
            // host 在 post-flush 主树与 overlay 完成布局后桥接最终 epoch，scrollbar 同帧 flush
            // 内重跑 effect 读最新 LayoutBox，
            // 零滞后覆盖 section 切换 + 窗口 resize 两种 content 高度变化场景。
            // BUG2：Props 拆 read/write——activeScroll 为动态 authority 目标，
            // setScroll 供 thumb 拖动/track 点击直接定位；滚轮由 viewport handler 单独平滑处理。
            // Scrollbar 的 handler/dragStart 必须读取当前可见 offset，而非滚轮已提前写入的终点 authority。
            // 仍读取 activeScroll 建立响应式依赖；逐帧位置由 applyScrollOffset 同步到 thumb。
            ReadableSignal<Integer> scrollbarDisplayScroll = new ReadableSignal<Integer>() {
                @Override
                public Integer get() {
                    activeScroll.get();
                    return Integer.valueOf(viewport.getScrollOffsetY());
                }
            };
            SceneScrollbar.Props sbProps = new SceneScrollbar.Props(
                    viewport, scrollbarDisplayScroll, setScroll,
                    SceneScrollbar.DEFAULT_TRACK_COLOR, SceneScrollbar.DEFAULT_THUMB_COLOR,
                    SceneScrollbar.DEFAULT_BAR_WIDTH, SceneScrollbar.DEFAULT_MIN_THUMB_HEIGHT,
                    setScroll);
            SceneScrollbar.Result sb = SceneScrollbar.create(runtime, sbProps);
            this.scrollbarColumn = sb.column();
            this.scrollbarThumb = sb.thumb();
            scrollContainer.appendChild(scrollbarColumn);
        });

        // A4c:构造期首刷已收口——生产渲染每帧走管线 FLUSH 相位物化;
        // headless 测试须在读取 Computed 前先冲刷调度器（见 ConfigScreenTest.setUp）。
    }

    /**
     * 创建固定标题条。
     *
     * <p>m2：主标题用 {@link ConfigSchema#title()}（人类可读，缺省回退 modId），
     * 副标题显示 modId 技术标识。</p>
     *
     * <p>注：pageRoot/viewport/scrollContainer 骨架由 {@link FormPageShell#build} 的 theme-aware
     * 重载统一构建，titleBar 因字号需求（FONT_TITLE=24/FONT_SUBTITLE=12，shell 的 text 不设字号）
     * 保留自建——构造期已传 buildTitleBar=false 跳过 shell 标题条构造，此处直接挂自建标题条到
     * 玻璃页壳；标题/副标题前景经 {@code formTheme} 信号取主题语义色（与 shell 默认路径同映射：
     * titleColor=foreground、mutedColor=mutedForeground），主题切换只重派生。</p>
     *
     * @return 标题条节点
     */
    private SceneNode createTitleBar() {
        SceneNode bar = SceneNode.column();
        bar.setPreferredHeight(0);
        bar.setFillParentWidth(true);
        bar.setMaxWidth(ConfigTheme.PAGE_MAX_WIDTH);
        bar.setGap(2);
        bar.setHitTestable(false);
        return bar;
    }

    /**
     * 创建 save 反馈独立行（S4）：仅在 {@code saveFeedbackSignal} 非 NONE 时挂载，
     * NONE 时隐藏不占高（rt.show 懒挂载：未激活子树不参与布局/绘制）。
     *
     * <p>requiresReload 冲突时额外挂「丢弃编辑并重新加载」按钮行（组件只建一次，
     * 显隐由 Signal/Computed + rt.show 驱动（不做命令式挂卸、组件只建一次、写入帧末批处理生效）；不自动 reload/merge）。</p>
     *
     * <p>G15/Shell：内容卡片底座改走主题路径——表面由 {@link SceneSurfaceBinder} 按来源
     * 主题 {@code GROUP} 配方独占绑定（低干扰内容底座，契约 §4.1），旧的
     * {@code SURFACE_CONTAINER}/{@code RADIUS_MD} 静态设色删除；反馈文字三态色经
     * {@code formTheme}：none→mutedColor、error→errorColor、成功→dirtyColor（主题 accent，
     * 替代旧固定绿 OK_COLOR——主题无 success 槽，叶子实例不自造色板）。</p>
     *
     * @return save 反馈条节点（condition 为 true 时显示）
     */
    private SceneNode createSaveFeedbackBar() {
        SceneNode col = SceneNode.column();
        col.setGap(6);
        col.setHitTestable(true);
        col.setFillParentWidth(true);
        col.setMaxWidth(ConfigTheme.PAGE_MAX_WIDTH);
        col.setPadding(8);
        // 固定 preferredHeight：作为页壳 COLUMN 内固定子，未设则 grow 求解器 UNCONSTRAINED 早退。
        // 预留 reload 按钮行高度（即使当前不显示，高度略余可接受；冲突态可完整显示按钮）。
        col.setPreferredHeight(ConfigTheme.SAVE_FEEDBACK_HEIGHT + 6 + ConfigTheme.BUTTON_HEIGHT);
        // GROUP 玻璃底座：染色/边框/圆角/浮雕/滤镜唯一写入者 = 表面绑定器（主题路径）。
        bindGroupSurface(runtime, col);

        SceneNode row = SceneNode.row();
        row.setGap(8);
        row.setHitTestable(false);
        row.setPreferredHeight(ConfigTheme.SAVE_FEEDBACK_HEIGHT);
        SceneNode feedback = text("", Computed.create(() -> {
                    FormTheme theme = formTheme.get();
                    SaveFeedback fb = adapter.saveFeedbackSignal().get();
                    if (fb == null || fb.isNone()) {
                        return Integer.valueOf(theme.mutedColor());
                    }
                    return Integer.valueOf(
                            fb.isError() ? theme.errorColor() : theme.dirtyColor());
                }),
                ConfigTheme.FONT_ERROR);
        runtime.bindComputed(() -> {
                    SaveFeedback fb = adapter.saveFeedbackSignal().get();
                    return fb == null ? "" : fb.message();
                },
                feedback::setText);
        row.appendChild(feedback);
        col.appendChild(row);

        // reload 按钮：condition = requiresReload；Supplier 只跑一次建按钮
        rt().show(col,
                Computed.create(() -> Boolean.valueOf(adapter.requiresReload())),
                this::createReloadButtonRow);

        return col;
    }

    /**
     * 创建「丢弃编辑并重新加载」按钮行（仅 requiresReload 时由 rt.show 挂载一次）。
     *
     * @return 按钮行节点
     */
    private SceneNode createReloadButtonRow() {
        SceneNode row = SceneNode.row();
        row.setGap(8);
        row.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT);
        SceneButton.Props props = new SceneButton.Props(
                Signal.create("丢弃编辑并重新加载"),
                Signal.create(Boolean.TRUE),
                this::discardEditsAndReload,
                SceneButtonVariant.STANDARD);
        MountHandle handle = runtime.mount(row, SceneButton.create(runtime, props));
        SceneNode btnRoot = handle.getRoot();
        if (btnRoot != null) {
            btnRoot.setPreferredWidth(ConfigTheme.BUTTON_WIDTH + ConfigTheme.dp(40));
            btnRoot.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT);
        }
        return row;
    }

    /**
     * 丢弃当前编辑并从磁盘重新加载（{@link ConfigManager#reloadDraftFromDisk}），
     * 经 {@link DraftSignalAdapter#replaceDraft} 保持 Signal identity；恢复可保存。
     * 不得自动 merge / 静默覆盖 Authority。
     *
     * <p>IO / 校验 / 冲突失败时：按 {@link ConfigReloadException.Reason} 结构化显示
     *（禁止英文匹配）；失败保留编辑 / requiresReload 冲突态；不静默折叠 NUMBER 为 0.0、
     * 不推进 Authority。</p>
     */
    private void discardEditsAndReload() {
        try {
            DraftBuffer fresh = manager.reloadDraftFromDisk();
            adapter.replaceDraft(fresh);
        } catch (blocamlimb.nhx100218.modernui.config.runtime.ConfigConflictException e) {
            // 通知期等：保留冲突与编辑
            adapter.setSaveFeedback(SaveFeedback.forConflict(e.conflictType()));
        } catch (blocamlimb.nhx100218.modernui.config.runtime.ConfigReloadException e) {
            SaveFeedback.Status status;
            String prefix;
            switch (e.reason()) {
                case VALIDATION:
                    status = SaveFeedback.Status.INVALID;
                    prefix = "重新加载校验失败";
                    break;
                case CONFLICT:
                    status = SaveFeedback.Status.CONFLICT;
                    if (e.conflictType() != null
                            && e.conflictType() != blocamlimb.nhx100218.modernui.config.runtime.SaveOutcome.ConflictType.NONE) {
                        adapter.setSaveFeedback(SaveFeedback.forConflict(e.conflictType()));
                        return;
                    }
                    prefix = "重新加载冲突";
                    break;
                case IO:
                default:
                    status = SaveFeedback.Status.IO_FAILED;
                    prefix = "重新加载失败";
                    break;
            }
            adapter.setSaveFeedback(new SaveFeedback(
                    status,
                    prefix + "。当前编辑已保留。"));
        } catch (blocamlimb.nhx100218.modernui.config.ConfigException e) {
            // 兼容其它 ConfigException：按 IO 友好反馈，不匹配英文
            adapter.setSaveFeedback(new SaveFeedback(
                    SaveFeedback.Status.IO_FAILED,
                    "重新加载失败。当前编辑已保留。"));
        }
    }


    /**
     * null 安全计数读取（flush 前 Computed 可能返回 null）。
     *
     * @param v 计数 signal 值
     * @return 非 null 计数
     */
    private static int safeCount(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    /** 滚轮目标合并到当前 section authority，并从当前显示位置平滑收敛。 */
    private boolean smoothScrollByWheelDelta(int wheelDelta) {
        // Signal.set 在 flush 前不可见；运行中或同帧 pending 目标承担连续事件累计基准。
        int currentTarget = scrollIntentPending
                ? pendingScrollTarget
                : (smoothScrollRunning ? smoothScrollTarget : activeScroll.get().intValue());
        int target = clampScrollOffset(currentTarget - wheelDelta);
        if (target == currentTarget) {
            return false;
        }
        requestScrollIntent(target, true);
        return true;
    }

    /** 滚动条拖动、track 点击与编程式写入请求在本帧 flush 直接定位。 */
    private void requestDirectScroll(Integer requested) {
        int target = clampScrollOffset(requested == null ? 0 : requested.intValue());
        requestScrollIntent(target, false);
    }

    /** handler 的唯一写入口：合并最新目标并递增 intent signal，不触碰节点或 Motion。 */
    private void requestScrollIntent(int target, boolean smooth) {
        pendingScrollTarget = target;
        pendingScrollSmooth = smooth;
        scrollIntentPending = true;
        scrollIntentRevision.set(Integer.valueOf(++scrollIntentRevisionCounter));
    }

    /** flush effect 消费最终 intent；Motion 初始 applier 与直接 snap 都已脱离输入调用栈。 */
    private void applyPendingScrollIntent() {
        if (!scrollIntentPending) {
            return;
        }
        int target = pendingScrollTarget;
        boolean smooth = pendingScrollSmooth;
        scrollIntentPending = false;
        if (smooth) {
            startSmoothScroll(target);
        } else {
            snapScrollOffset(target);
        }
        writeActiveSectionScroll(target);
    }

    /** 写当前 live section 的滚动 authority。 */
    private void writeActiveSectionScroll(int target) {
        int idx = displayedSectionIndex;
        if (sectionScrolls != null && idx >= 0 && idx < sectionScrolls.length) {
            sectionScrolls[idx].set(Integer.valueOf(target));
        }
    }

    /** 把目标限制到当前 viewport 可滚范围。 */
    private int clampScrollOffset(int target) {
        return Math.max(0, Math.min(SceneGeometry.maxScrollY(viewport), target));
    }

    /** 启动滚轮轨道；运行期间的新目标先累计，由下一次 sample 从当时显示值重定向。 */
    private void startSmoothScroll(int target) {
        int to = clampScrollOffset(target);
        if (smoothScrollRunning) {
            int current = viewport.getScrollOffsetY();
            long oldDelta = (long) smoothScrollTarget - current;
            long newDelta = (long) to - current;
            smoothScrollTarget = to;
            if (newDelta == 0L) {
                snapScrollOffset(to);
            } else if (oldDelta * newDelta < 0L) {
                // 反向滚轮立即截断旧方向；同向连续输入则保留到本帧 sample 后无停顿重定向。
                startSmoothScrollSegment(to);
            }
            return;
        }
        smoothScrollTarget = to;
        if (viewport.getScrollOffsetY() == to) {
            snapScrollOffset(to);
            return;
        }
        smoothScrollRunning = true;
        startSmoothScrollSegment(to);
    }

    /**
     * 跑一个从当前显示值到快照目标的段。若 route 在本帧写入了更新目标，先采样旧段一次，
     * 再在同一 sample 边界从刚得到的显示值重建轨道，避免持续滚轮每帧重启在 progress=0。
     */
    private void startSmoothScrollSegment(int segmentTarget) {
        int from = viewport.getScrollOffsetY();
        int to = clampScrollOffset(segmentTarget);
        // 动态内容收缩可能让旧目标越界；同步收敛字段，避免按旧值重复重建同一 clamp 段。
        smoothScrollTarget = to;
        if (from == to) {
            runtime.__cancelMotion(scrollMotionKey);
            smoothScrollRunning = false;
            return;
        }
        runtime.__startEaseOutMotion(scrollMotionKey, SCROLL_MOTION_MS,
                progress -> {
                    applyScrollOffset(Math.round(from + (to - from) * progress.floatValue()));
                    if (smoothScrollRunning && smoothScrollTarget != to) {
                        startSmoothScrollSegment(smoothScrollTarget);
                    }
                },
                () -> {
                    applyScrollOffset(to);
                    if (smoothScrollTarget != to) {
                        startSmoothScrollSegment(smoothScrollTarget);
                    } else {
                        smoothScrollRunning = false;
                    }
                });
    }

    /** 立即定位并取消尚未完成的滚轮轨道；用于 section 切换和 scrollbar 拖动。 */
    private void snapScrollOffset(int target) {
        boolean needsApply = smoothScrollRunning || viewport.getScrollOffsetY() != target;
        runtime.__cancelMotion(scrollMotionKey);
        smoothScrollRunning = false;
        smoothScrollTarget = target;
        if (needsApply) {
            applyScrollOffset(target);
        }
    }

    /** 同帧更新内容 geometry、thumb composite，并请求按新滚动几何重算 hover。 */
    private void applyScrollOffset(int offset) {
        boolean geometryChanged = viewport.getScrollOffsetY() != offset;
        viewport.setScrollOffsetY(offset);
        syncScrollbarThumb(offset);
        if (geometryChanged) {
            runtime.__requestHoverReconcileAfterScroll();
        }
    }

    /** 让 thumb 与平滑内容使用同一 offset，避免 authority 已到终点而 thumb 先跳过去。 */
    private void syncScrollbarThumb(int offset) {
        if (scrollbarThumb == null) {
            return;
        }
        Object viewportLayout = viewport.getCachedLayout();
        Object thumbLayout = scrollbarThumb.getCachedLayout();
        if (!(viewportLayout instanceof LayoutBox) || !(thumbLayout instanceof LayoutBox)) {
            return;
        }
        int maxScroll = SceneGeometry.maxScrollY(viewport);
        if (maxScroll <= 0) {
            scrollbarThumb.setTransform(Transform.translate(0.0f, 0.0f));
            return;
        }
        int trackRange = Math.max(0,
                ((LayoutBox) viewportLayout).getHeight() - ((LayoutBox) thumbLayout).getHeight());
        float thumbY = (float) trackRange * Math.max(0, Math.min(maxScroll, offset)) / maxScroll;
        scrollbarThumb.setTransform(Transform.translate(0.0f, thumbY));
    }

    /**
     * 创建纵向 SceneNavList 侧栏导航，直接 mount 到 bodyRow。
     *
     * @param parent   bodyRow 父节点
     * @param sections section 列表
     * @return navPane 根节点（已 mount 到 parent）
     */
    private SceneNode createSidebarNav(SceneNode parent, List<GroupSpec> groups) {
        SceneNode nav = SceneNode.column();
        nav.setPreferredWidth(ConfigTheme.NAV_PANE_WIDTH);
        nav.setFillParentHeight(true);
        nav.setPadding(ConfigTheme.NAV_PADDING);
        nav.setGap(ConfigTheme.NAV_GAP);
        SceneInteractionState navInteraction = runtime.interactionState(nav);
        navInteraction.hovered();
        navInteraction.pressed();
        navInteraction.focused();
        SceneSurfaceBinder.bind(runtime, nav,
                SceneThemes.surface(runtime, SceneTheme.Role.TOOLBAR),
                ALWAYS_ENABLED, navInteraction);

        SceneImageSource[] icons = navIcons();
        nav.appendChild(createNavItem(icons, 3, "modernui.config.group.home",
                Computed.create(() -> Boolean.TRUE.equals(dashboardSignal.get())),
                () -> dashboardSignal.set(Boolean.TRUE)));
        for (int i = 0; i < groups.size(); i++) {
            final int groupIndex = i;
            GroupSpec group = groups.get(i);
            ReadableSignal<Boolean> selected = Computed.create(() ->
                    Boolean.valueOf(!Boolean.TRUE.equals(dashboardSignal.get())
                            && activeGroupIndex() == groupIndex));
            nav.appendChild(createNavItem(icons, groupIndex, group.title, selected, () -> {
                dashboardSignal.set(Boolean.FALSE);
                if (!group.sectionIndices.isEmpty()) {
                    activeSectionSignal.set(group.sectionIndices.get(0));
                }
            }));
        }
        parent.appendChild(nav);
        return nav;
    }

    private SceneNode createNavItem(SceneImageSource[] icons, int iconIndex, String title,
                                    ReadableSignal<Boolean> selected, Runnable action) {
        SceneNode item = SceneNode.column();
        item.setFillParentWidth(true);
        item.setPreferredHeight(ConfigTheme.NAV_ITEM_HEIGHT);
        item.setPadding(ConfigTheme.NAV_ITEM_PADDING_H, ConfigTheme.NAV_ITEM_PADDING_V,
                ConfigTheme.NAV_ITEM_PADDING_H, ConfigTheme.NAV_ITEM_PADDING_V);
        item.setGap(ConfigTheme.dp(4));
        item.setCrossAxisAlign(CrossAxisAlign.CENTER);
        item.setCursor(SceneCursor.POINTER);

        SceneInteractionState interaction = runtime.interactionState(item);
        SceneNode iconPill = SceneNode.column();
        iconPill.setPreferredWidth(ConfigTheme.dp(56));
        iconPill.setPreferredHeight(ConfigTheme.dp(32));
        iconPill.setCornerRadius(ConfigTheme.dp(16));
        iconPill.setCrossAxisAlign(CrossAxisAlign.CENTER);
        iconPill.setMainAxisAlign(MainAxisAlign.CENTER);
        runtime.bind(Computed.create(() -> {
            if (selected.get()) {
                return Integer.valueOf(0x33FFB5A1);
            }
            if (interaction.pressed().get()) {
                return Integer.valueOf(0x1FFFB5A1);
            }
            if (interaction.hovered().get()) {
                return Integer.valueOf(0x14FFB5A1);
            }
            return Integer.valueOf(0);
        }), iconPill::setBackgroundColor);

        SceneNode icon = new SceneNode();
        icon.setPreferredWidth(ConfigTheme.NAV_ICON_SIZE);
        icon.setPreferredHeight(ConfigTheme.NAV_ICON_SIZE);
        icon.setHitTestable(false);
        if (icons != null && iconIndex < icons.length) {
            icon.setImageSource(icons[iconIndex]);
        }
        runtime.bind(Computed.create(
                        () -> Float.valueOf(selected.get() ? 1.0f : 0.55f)),
                icon::setOpacity);
        iconPill.appendChild(icon);
        item.appendChild(iconPill);

        item.appendChild(text(ConfigText.tr(title), Computed.create(() ->
                Integer.valueOf(selected.get() ? 0xFFFFB5A1 : 0xFFD8C2BC)),
                ConfigTheme.FONT_SUBTITLE));
        runtime.focusable(item, ALWAYS_ENABLED);
        runtime.on(item, SceneEventType.CLICK, (event, context) -> action.run());
        return item;
    }
    private static SceneImageSource[] navIcons() {
        if (sNavIcons != null) {
            return sNavIcons;
        }
        try (InputStream in = ConfigScreen.class.getResourceAsStream(
                "/assets/modernui/textures/gui/new_icon_white.png")) {
            if (in == null) {
                return null;
            }
            BufferedImage sheet = ImageIO.read(in);
            if (sheet == null) {
                return null;
            }
            Class<?> host = Class.forName(
                    "blocamlimb.nhx100218.modernui.uilib.ui.image.HostImageSource");
            Method factory = host.getMethod("bufferedImage", BufferedImage.class, String.class);
            int[][] cells = {{0, 0}, {5, 0}, {6, 0}, {0, 2}};
            SceneImageSource[] icons = new SceneImageSource[cells.length];
            for (int i = 0; i < cells.length; i++) {
                BufferedImage crop = sheet.getSubimage(cells[i][0] * 72, cells[i][1] * 72, 72, 72);
                icons[i] = (SceneImageSource) factory.invoke(null, crop,
                        "modernui:gui/nav_icon_" + i);
            }
            sNavIcons = icons;
        } catch (IOException | ReflectiveOperationException | LinkageError ignored) {
        }
        return sNavIcons;
    }
    private List<GroupSpec> createGroups(List<SectionSpec> sections) {
        List<GroupSpec> result = new ArrayList<GroupSpec>();
        for (String title : GROUP_TITLES) {
            result.add(new GroupSpec(title));
        }
        int fallback = GROUP_TITLES.length - 1;
        Map<String, Integer> byName = new HashMap<String, Integer>();
        for (int i = 0; i < sections.size(); i++) {
            byName.put(sections.get(i).name(), Integer.valueOf(i));
        }
        boolean[] assigned = new boolean[sections.size()];
        for (int i = 0; i < GROUP_SECTION_IDS.length; i++) {
            for (String id : GROUP_SECTION_IDS[i]) {
                Integer index = byName.get(id);
                if (index != null && !assigned[index.intValue()]) {
                    result.get(i).sectionIndices.add(index);
                    assigned[index.intValue()] = true;
                }
            }
        }
        for (int i = 0; i < sections.size(); i++) {
            if (!assigned[i]) {
                result.get(fallback).sectionIndices.add(Integer.valueOf(i));
            }
        }
        for (int i = result.size() - 1; i >= 0; i--) {
            if (result.get(i).sectionIndices.isEmpty()) {
                result.remove(i);
            }
        }
        return result;
    }

    private static int groupForSection(String name) {
        if (name == null) {
            return GROUP_TITLES.length - 1;
        }
        for (int i = 0; i < GROUP_SECTION_IDS.length; i++) {
            for (String id : GROUP_SECTION_IDS[i]) {
                if (id.equals(name)) {
                    return i;
                }
            }
        }
        return GROUP_TITLES.length - 1;
    }

    private int activeGroupIndex() {
        if (groups == null || groups.isEmpty() || activeSectionSignal == null) {
            return 0;
        }
        int section = activeSectionSignal.get().intValue();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).sectionIndices.contains(Integer.valueOf(section))) {
                return i;
            }
        }
        return 0;
    }

    private int localSectionIndex(GroupSpec group) {
        if (group == null || group.sectionIndices.isEmpty()) {
            return 0;
        }
        int section = activeSectionSignal.get().intValue();
        int local = group.sectionIndices.indexOf(Integer.valueOf(section));
        return local < 0 ? 0 : local;
    }

    private SceneNode createTopTabs() {
        SceneNode host = SceneNode.column();
        host.setFillParentWidth(true);
        host.setMaxWidth(ConfigTheme.PAGE_MAX_WIDTH);
        host.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT);
        for (int i = 0; i < groups.size(); i++) {
            final int groupIndex = i;
            rt().show(host,
                    Computed.create(() -> Boolean.valueOf(activeGroupIndex() == groupIndex)),
                    () -> createGroupTabs(groups.get(groupIndex)));
        }
        return host;
    }

    private SceneNode createGroupTabs(GroupSpec group) {
        SceneNode host = SceneNode.row();
        host.setFillParentWidth(true);
        host.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT);
        host.setGap(ConfigTheme.dp(6));
        host.setCrossAxisAlign(CrossAxisAlign.STRETCH);
        host.setMainAxisAlign(MainAxisAlign.CENTER);
        int maxLabelWidth = ConfigTheme.dp(140);
        for (int i = 0; i < group.sectionIndices.size(); i++) {
            final int sectionIndex = group.sectionIndices.get(i).intValue();
            SceneNode tab = SceneNode.column();
            tab.setWidthSizing(SceneNode.WidthSizing.SHRINK);
            tab.setPreferredHeight(ConfigTheme.BUTTON_HEIGHT);
            tab.setPadding(0, ConfigTheme.dp(8), 0, ConfigTheme.dp(8));
            tab.setGap(ConfigTheme.dp(4));
            tab.setCrossAxisAlign(CrossAxisAlign.CENTER);
            tab.setMainAxisAlign(MainAxisAlign.CENTER);
            tab.setCursor(SceneCursor.POINTER);
            int labelWidth = Math.min(runtime.measureTextWidth(
                    sections.get(sectionIndex).title(), ConfigTheme.NAV_TAB_FONT_SIZE), maxLabelWidth);
            tab.setPreferredWidth(labelWidth + ConfigTheme.dp(16));
            ReadableSignal<Boolean> selected = Computed.create(
                    () -> Boolean.valueOf(activeSectionSignal.get().intValue() == sectionIndex));
            SceneNode label = text(sections.get(sectionIndex).title(),
                    Computed.create(() -> Integer.valueOf(selected.get()
                            ? 0xFFFFB5A1 : 0xFFD8C2BC)),
                    ConfigTheme.NAV_TAB_FONT_SIZE);
            label.setMaxTextWidth(maxLabelWidth);
            label.setMaxLines(1);
            label.setEllipsis(true);
            tab.appendChild(label);
            SceneNode indicator = new SceneNode();
            indicator.setPreferredHeight(ConfigTheme.dp(3));
            indicator.setCornerRadius(ConfigTheme.dp(2));
            indicator.setPreferredWidth(labelWidth);
            runtime.bind(Computed.create(() -> Integer.valueOf(selected.get()
                    ? 0xFFFFB5A1 : 0)), indicator::setBackgroundColor);
            tab.appendChild(indicator);
            runtime.focusable(tab, ALWAYS_ENABLED);
            runtime.on(tab, SceneEventType.CLICK, (event, context) ->
                    activeSectionSignal.set(Integer.valueOf(sectionIndex)));
            host.appendChild(tab);
        }
        return host;
    }
    private SceneNode createDashboard() {
        SceneNode root = SceneNode.row();
        root.setFillParentWidth(true);
        root.setFillParentHeight(true);
        root.setPadding(ConfigTheme.dp(20));
        root.setGap(ConfigTheme.dp(16));
        root.setCrossAxisAlign(CrossAxisAlign.START);

        SceneNode left = dashboardCard();
        left.setFlexGrow(1);
        left.appendChild(dashboardTitle(ConfigText.tr("modernui.dashboard.title")));
        left.appendChild(dashboardBody(ConfigText.tr("modernui.dashboard.about")));

        SceneNode right = dashboardCard();
        right.setFlexGrow(1);
        right.appendChild(dashboardTitle(ConfigText.tr("modernui.dashboard.changelog")));
        right.appendChild(dashboardBody(ConfigText.tr("modernui.dashboard.changes")));

        root.appendChild(left);
        root.appendChild(right);
        return root;
    }

    private SceneNode dashboardCard() {
        SceneNode card = SceneNode.column();
        card.setFillParentWidth(true);
        card.setPadding(ConfigTheme.dp(24));
        card.setGap(ConfigTheme.dp(12));
        bindGroupSurface(runtime, card);
        return card;
    }

    private SceneNode dashboardTitle(String title) {
        SceneNode node = new SceneNode();
        node.setText(title);
        node.setFontSize(ConfigTheme.FONT_TITLE);
        node.setTextColor(0xFFFFB5A1);
        node.setMaxLines(2);
        return node;
    }

    private SceneNode dashboardBody(String body) {
        SceneNode node = new SceneNode();
        node.setText(body.replace("\\n", "\n"));
        node.setFontSize(ConfigTheme.FONT_LABEL);
        node.setTextColor(0xFFD8C2BC);
        node.setMaxLines(12);
        node.setLineHeightMultiplier(1.25D);
        return node;
    }
    private SceneNode createContent() {
        SceneNode node = SceneNode.column();
        node.setGap(ConfigTheme.dp(14));
        node.setFillParentWidth(true);
        node.setMaxWidth(ConfigTheme.CONTENT_MAX_WIDTH);
        return node;
    }

    /** 初始挂载单槽 section panel；后续切换沿用同一个 sectionOwner。 */
    private void renderFields(List<SectionSpec> sections) {
        if (!sections.isEmpty()) {
            mountSectionPanel(displayedSectionIndex);
        }
    }

    /** 接收导航目标；立即切换单槽，outgoing Owner 负责取消尚未完成的 reveal。 */
    private void requestSectionTransition(Integer requested) {
        int target = normalizeSectionIndex(requested);
        if (target < 0 || target == displayedSectionIndex) {
            return;
        }
        requestDismissOutgoingOverlays();
        switchSectionPanel(target);
    }

    /** 请求所有 active Config overlay 走各自受控 dismiss 回调；实际摘除仍由 portal signal 派生。 */
    private void requestDismissOutgoingOverlays() {
        for (SceneOverlayHost.Entry entry : runtime.getOverlayHost().topFirst()) {
            entry.requestDismiss();
        }
    }

    /** 把外部 section 下标收敛到 schema 范围；空 schema 返回 -1。 */
    private int normalizeSectionIndex(Integer requested) {
        int count = sections.size();
        if (count <= 0) {
            return -1;
        }
        int raw = requested == null ? 0 : requested.intValue();
        return Math.max(0, Math.min(count - 1, raw));
    }

    /**
     * 构建单个 section 面板：sectionTitle + 遍历 fields 调 registry.render 挂卡片。
     *
     * @param section section 元数据
     * @return section 面板节点
     */
    private SceneNode buildSectionPanel(SectionSpec section) {
        SceneNode card = SceneNode.column();
        card.setFillParentWidth(true);
        card.setGap(0);
        bindGroupSurface(runtime, card);
        List<SceneNode> revealTargets = new ArrayList<SceneNode>();
        List<FieldSpec> fields = section.fields();
        FieldShellBinder.setRowMode(true);
        try {
        for (int i = 0; i < fields.size(); i++) {
            FieldRenderer renderer = registry.resolve(fields.get(i));
            if (renderer == null) {
                continue;
            }
            SceneNode row = renderer.render(runtime, fields.get(i), adapter);
            SceneNode presentation = SceneNode.column();
            presentation.setFillParentWidth(true);
            presentation.setHitTestable(false);
            presentation.appendChild(row);
            card.appendChild(presentation);
            revealTargets.add(presentation);
            if (i + 1 < fields.size()) {
                SceneNode divider = new SceneNode();
                divider.setPreferredHeight(1);
                divider.setFillParentWidth(true);
                divider.setMargin(0, ConfigTheme.dp(12), 0, ConfigTheme.dp(12));
                divider.setBackgroundColor(0x2253433F);
                card.appendChild(divider);
            }
        }
        } finally {
            FieldShellBinder.setRowMode(false);
        }
        runtime.__staggeredReveal(revealTargets,
                SECTION_REVEAL_START_OFFSET_Y,
                SECTION_REVEAL_MS,
                SECTION_REVEAL_ITEM_DELAY_MS,
                SECTION_REVEAL_MAX_DELAY_MS);
        return card;
    }
    private SceneNode mountSectionPanel(int sectionIndex) {
        MountHandle[] next = new MountHandle[1];
        SectionSpec section = sections.get(sectionIndex);
        sectionOwner.run(() -> next[0] = runtime.mount(content, () -> buildSectionPanel(section)));
        sectionMount = next[0];
        displayedSectionIndex = sectionIndex;
        return sectionMount.getRoot();
    }

    /** 单槽切换：先完整回收 outgoing Owner，再构建 incoming，任何时点都不并存两个 live panel。 */
    private SceneNode switchSectionPanel(int sectionIndex) {
        if (sectionMount != null) {
            sectionMount.dispose();
            sectionMount = null;
        }
        SceneNode incoming = mountSectionPanel(sectionIndex);
        if (sectionScrolls != null && sectionIndex >= 0 && sectionIndex < sectionScrolls.length) {
            snapScrollOffset(Math.max(0, sectionScrolls[sectionIndex].get().intValue()));
        }
        // 即使两个 section 的 offset 相同，旧 hoveredNode 也已随 outgoing 子树卸载。
        runtime.__requestHoverReconcileAfterScroll();
        return incoming;
    }

    /**
     * 取场景运行时（等价 {@code runtime}）。
     *
     * @return 场景运行时
     */
    private SceneRuntime rt() {
        return runtime;
    }

    /**
     * 创建固定操作条：委托 {@link FormActionBar}（恢复默认 / spacer / 取消 + 保存 primary）。
     *
     * <p>S3：左右分区——恢复默认置最左（弱化低频破坏性操作），取消+保存置最右，
     * 保存在最右末位（主操作落在视线终点）。中间插 flexGrow=1 的 spacer 节点撑开剩余宽度
     * （scene MainAxisAlign 无 SPACE_BETWEEN，用 spacer 方案）。</p>
     *
     * <p>该固定行位于页壳根末尾，save/cancel/restore 始终可见、不随内容滚动。</p>
     *
     * <p>G15/Shell：改用 {@link FormActionBar} 的 theme-aware 无 {@code FormTheme} 参重载
     * （TOOLBAR 一颗玻璃底座，内部按钮走 BUTTON_STANDARD/PRIMARY 角色），删除旧的
     * {@code SURFACE_CONTAINER}/{@code RADIUS_LG} 后缀静态设色——表面唯一写入者是
     * {@code FormActionBar} 内部的 {@link SceneSurfaceBinder}（G11/ActionBar 遗留裁决，本次执行）。
     * 仅保留布局属性（fillParentWidth/maxWidth/padding/子按钮尺寸）。</p>
     *
     * @return 操作条节点
     */
    private SceneNode createActionBar() {
        ReadableSignal<Boolean> cancelEnabled = () -> {
            adapter.isDirtySignal().get();
            return Boolean.valueOf(adapter.draft().isDirtyAny());
        };
        ReadableSignal<Boolean> saveEnabled = () -> {
            adapter.canSaveSignal().get();
            return Boolean.valueOf(adapter.canSaveNow());
        };
        SceneNode bar = FormActionBar.build(runtime,
                Signal.create(Boolean.TRUE), () -> requestAction(ActionKind.RESTORE),
                cancelEnabled, () -> requestAction(ActionKind.CANCEL),
                saveEnabled, () -> requestAction(ActionKind.SAVE),
                ConfigTheme.ACTION_BAR_HEIGHT, ConfigTheme.dp(10),
                ConfigTheme.BUTTON_WIDTH, ConfigTheme.BUTTON_HEIGHT);
        SceneNode cancelButton = bar.__getChildren().get(2);
        SceneNode saveButton = bar.__getChildren().get(3);
        // list 拖拽终态尚未 flush 时按钮仍呈 disabled；先接收 CLICK，收敛后再按即时 Draft 判定。
        runtime.on(cancelButton, SceneEventType.CLICK, (event, context) -> {
            if (!adapter.draft().isDirtyAny()) {
                requestAction(ActionKind.CANCEL);
            }
        });
        runtime.on(saveButton, SceneEventType.CLICK, (event, context) -> {
            if (!adapter.canSaveNow()) {
                requestAction(ActionKind.SAVE);
            }
        });
        // 仅布局：宽度约束与内边距归 caller；表面（background/cornerRadius/…）由 FormActionBar
        // 内 TOOLBAR 绑定独占，此处不再静态设色。
        bar.setFillParentWidth(true);
        bar.setMaxWidth(ConfigTheme.PAGE_MAX_WIDTH);
        bar.setPadding(6);
        return bar;
    }

    private void requestAction(ActionKind kind) {
        ActionRequest request = new ActionRequest(kind, ++actionRevision);
        actionQueue.addLast(request);
        actionRequested.set(request);
    }

    /** 安装三跳收敛链；调用点位于 uiOwner scope，bindings 随 screen 一并释放。 */
    private void bindActionPipeline() {
        runtime.bind(actionRequested, request -> forwardAction(request, actionSettling));
        runtime.bind(actionSettling, request -> forwardAction(request, actionReady));
        runtime.bind(actionReady, request -> {
            if (request != null) {
                Effect.untrack(() -> drainActionQueue(request.revision));
            }
        });
    }

    private static void forwardAction(ActionRequest request, Signal<ActionRequest> nextStage) {
        if (request != null) {
            nextStage.set(request);
        }
    }

    private void drainActionQueue(int readyRevision) {
        try {
            while (!actionQueue.isEmpty()
                    && actionQueue.peekFirst().revision <= readyRevision) {
                executeAction(actionQueue.removeFirst().kind);
            }
        } catch (RuntimeException | Error failure) {
            discardActionsThrough(readyRevision);
            throw failure;
        }
    }

    /** 失败动作所属批次不得留在队列中被未来 CLICK 迟到重放。 */
    private void discardActionsThrough(int readyRevision) {
        while (!actionQueue.isEmpty()
                && actionQueue.peekFirst().revision <= readyRevision) {
            actionQueue.removeFirst();
        }
    }

    private void executeAction(ActionKind kind) {
        switch (kind) {
            case RESTORE:
                restoreDefaults();
                break;
            case CANCEL:
                if (adapter.draft().isDirtyAny()) {
                    cancelChanges();
                }
                break;
            case SAVE:
                if (adapter.canSaveNow()) {
                    saveChanges();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 保存：mgr.save(draft) + 结构化冲突/校验接入 + saveFeedbackSignal。
     *
     * <p>冲突走 {@link DraftSignalAdapter#applySaveFailure}（读 conflictType，禁止英文匹配）；
     * requiresReload 时不注入字段 error/errorCount，保存保持禁用；不自动 reload/重试/覆盖。</p>
     */
    private void scheduleAutosave() {
        autosavePending = true;
        runtime.__cancelMotion(autosaveKey);
        runtime.__startMotion(autosaveKey, 140, progress -> { }, this::flushAutosave);
    }

    private void flushAutosave() {
        if (!autosavePending) {
            return;
        }
        autosavePending = false;
        if (adapter.requiresReload() || adapter.draft().hasError()
                || !adapter.draft().isDirtyAny()) {
            return;
        }
        saveChanges();
    }
    private void saveChanges() {
        DraftBuffer draft = adapter.draft();
        lastSaveOutcome = manager.save(draft);
        if (lastSaveOutcome.isSuccess()) {
            adapter.afterSaveSync();
            adapter.setSaveFeedback(new SaveFeedback(SaveFeedback.Status.OK, "已保存"));
            return;
        }
        adapter.applySaveFailure(lastSaveOutcome);
    }

    /**
     * 取消：adapter.resetToCurrent()。
     */
    private void cancelChanges() {
        adapter.resetToCurrent();
    }

    /**
     * 恢复默认：按策略逐字段跳过、自定义或 adapter.resetFieldToDefault(path)。
     */
    private void restoreDefaults() {
        for (FieldSpec field : schema.allFields()) {
            String path = field.path();
            if (restorePolicy != null && restorePolicy.isSkipped(path)) {
                continue;
            }
            Consumer<DraftSignalAdapter> custom = restorePolicy != null ? restorePolicy.getCustom(path) : null;
            if (custom != null) {
                custom.accept(adapter);
            } else {
                adapter.resetFieldToDefault(path);
            }
        }
    }

    /**
     * 创建跟随主题的文字节点（前景经 {@code bind} 响应式写入，主题切换自动重派生）。
     *
     * @param value     文本
     * @param color     前景色信号（来源 {@code formTheme} 语义色派生）
     * @param fontSize  字号（UI 像素）
     * @return 文字节点
     */
    private SceneNode text(String value, ReadableSignal<Integer> color, int fontSize) {
        SceneNode node = new SceneNode();
        node.setText(value);
        node.setFontSize(fontSize);
        node.setHitTestable(false);
        runtime.bind(color, node::setTextColor);
        return node;
    }

    /**
     * 把来源主题的 GROUP 配方绑到内容卡片底座：染色/边框/圆角/浮雕/滤镜全归表面绑定器。
     *
     * <p>GROUP 是低干扰内容底座角色（契约 §4.1）；构建期先声明关心 hover/pressed/focus
     * （Router 对未创建的 signal 直接短路，不声明则后续事件驱动不了配方状态档），与
     * {@link FormPageShell}/{@link FormActionBar} 内部绑定同构。绑定器是这些属性的唯一写入者。</p>
     *
     * @param rt   场景运行时
     * @param node 内容卡片底座节点
     */
    private static void bindGroupSurface(SceneRuntime rt, SceneNode node) {
        ReadableSignal<SceneSurfaceStyle> group = SceneThemes.surface(rt, SceneTheme.Role.GROUP);
        SceneInteractionState interaction = rt.interactionState(node);
        interaction.hovered();
        interaction.pressed();
        interaction.focused();
        SceneSurfaceBinder.bind(rt, node, group, ALWAYS_ENABLED, interaction);
    }

    // ==================== 测试探针访问器（包级） ====================

    /** @return 内部场景运行时 */
    SceneRuntime __getRuntime() {
        return runtime;
    }

    /** @return 场景树根节点（世界遮罩，静态 ROOT_BG） */
    SceneNode __getRoot() {
        return root;
    }

    /** @return 玻璃页壳根节点（root 唯一子；PANEL 配方表面，标题/视口/操作行的真实父） */
    SceneNode __getPageRoot() {
        return pageRoot;
    }

    /** @return 滚动视口节点 */
    SceneNode __getViewport() {
        return viewport;
    }

    /** @return 视口内容容器节点 */
    SceneNode __getContent() {
        return content;
    }

    /** @return 标题条节点 */
    SceneNode __getTitleBar() {
        return titleBar;
    }


    /** @return 操作条节点 */
    SceneNode __getActionBar() {
        return actionBar;
    }

    /** @return 固定左侧导航根节点；0/1 section 时为 null */
    SceneNode __getNavRoot() {
        return navRoot;
    }

    /** @return 多 section 的双栏 bodyRow，否则 null */
    SceneNode __getBodyRow() {
        return bodyRow;
    }

    /** @return 滚动容器节点（ROW：viewport + scrollbar 列） */
    SceneNode __getScrollContainer() {
        return scrollContainer;
    }

    /** @return 滚动条列节点（scrollContainer 内 viewport 右侧独立列） */
    SceneNode __getScrollbarColumn() {
        return scrollbarColumn;
    }

    /** @return 当前活动 section 下标受控源 */
    Signal<Integer> __getActiveSectionSignal() {
        return activeSectionSignal;
    }

    /**
     * 切换到指定 section（导航入口的宿主无关形态）。
     *
     * <p>与导航点击 handler 写的是同一个受控源（{@code activeSectionSignal}），因此不引入第二条
     * 切 section 路径；给 headless 出图矩阵用——配置页一次只挂一个 section，没有这个入口就只能
     * 出到第一个 section 的图。与 {@code TestPlaygroundHost#showPage(int)} 同构的只是
     * 「signal-first、不依赖命中坐标」这一点；**越界语义刻意不同**——那边越界 clamp，这里 fail-fast
     * （下标写错要能当场看见，而不是静默出一张别的 section 的图）。</p>
     *
     * @param index section 下标，取值 [0, schema.sections().size())
     * @throws IllegalArgumentException 下标越界
     */
    public void showSection(int index) {
        final int count = schema.sections().size();
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException(
                    "section 下标越界：" + index + "（有效范围 0.." + (count - 1) + "）");
        }
        activeSectionSignal.set(Integer.valueOf(index));
    }

    /** @return 当前实际挂载的 section 下标 */
    int __getDisplayedSectionIndex() {
        return displayedSectionIndex;
    }

    /** @return 当前 live section 的动态滚动偏移只读显示源（clamp 到当前 maxScroll） */
    ReadableSignal<Integer> __getActiveScroll() {
        return activeScroll;
    }

    /** @return 滚动偏移直接写入回调（写 authority 并立即定位，用于 scrollbar/测试） */
    Consumer<Integer> __getSetScroll() {
        return setScroll;
    }

    /** 测试探针：注入滚轮 delta，返回是否产生了新滚动目标。 */
    boolean __scrollByWheelDeltaForTest(int wheelDelta) {
        return smoothScrollByWheelDelta(wheelDelta);
    }

    /** @return per-section scroll signals（每个 section 独立保持滚动位置） */
    Signal<Integer>[] __getSectionScrolls() {
        return sectionScrolls;
    }

    /** @return 草稿适配器 */
    DraftSignalAdapter __getAdapter() {
        return adapter;
    }

    /** @return 最近一次保存结果 */
    SaveOutcome __getLastSaveOutcome() {
        return lastSaveOutcome;
    }

    /** 测试探针：触发保存 */
    void __saveChanges() {
        saveChanges();
    }

    /** 测试探针：触发取消 */
    void __cancelChanges() {
        cancelChanges();
    }

    /** 测试探针：触发恢复默认 */
    void __restoreDefaults() {
        restoreDefaults();
    }

    /** 测试探针：触发丢弃编辑并重新加载 */
    void __discardEditsAndReload() {
        discardEditsAndReload();
    }

    @Override
    protected SceneNode getRoot() {
        return root;
    }

    /**
     * 销毁配置页：先回收 adapter 的 Computed，再回收 uiOwner 作用域内所有 Computed/Effect，
     * 最后销毁 scene runtime。
     */
    @Override
    public void dispose() {
        try {
            if (autosavePending) {
                runtime.__cancelMotion(autosaveKey);
                flushAutosave();
            }
            runtime.__cancelMotion(scrollMotionKey);
            try {
                adapter.dispose();
            } finally {
                uiOwner.dispose();
            }
        } finally {
            super.dispose();
        }
    }
}
