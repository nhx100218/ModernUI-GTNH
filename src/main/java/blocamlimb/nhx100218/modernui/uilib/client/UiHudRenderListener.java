package blocamlimb.nhx100218.modernui.uilib.client;

import blocamlimb.nhx100218.modernui.uilib.client.hud.ClientHudServiceImpl;
import blocamlimb.nhx100218.modernui.uilib.client.hud.FramebufferViewportFactory;
import blocamlimb.nhx100218.modernui.uilib.client.hud.HudViewportMetrics;
import blocamlimb.nhx100218.modernui.uilib.client.hud.LiveMinecraftHudEnvironment;
import blocamlimb.nhx100218.modernui.uilib.client.hud.MinecraftHudEnvironment;
import blocamlimb.nhx100218.modernui.uilib.client.hud.SceneHudHost;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudAnchor;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudSpec;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudVisibility;
import blocamlimb.nhx100218.modernui.uilib.ui.diagnostic.UiPerformanceMonitor;
import blocamlimb.nhx100218.modernui.uilib.ui.env.UiEnvironment;
import blocamlimb.nhx100218.modernui.uilib.ui.host.UiFrameGlStateFence;
import blocamlimb.nhx100218.modernui.uilib.ui.host.UiHostRenderSupport;
import blocamlimb.nhx100218.modernui.uilib.ui.reactive.Signal;
import blocamlimb.nhx100218.modernui.uilib.ui.render.PaintContextCompositor;
import blocamlimb.nhx100218.modernui.uilib.ui.runtime.UiRuntimeAdapters;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.host.SceneHostAssembly;
import blocamlimb.nhx100218.modernui.uilib.ui.render.UiMainLayerSnapshotService;
import blocamlimb.nhx100218.modernui.uilib.ui.render.UiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.GL11;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** 唯一 Forge HUD render bridge；不取消事件，不承载业务布局。 */
public final class UiHudRenderListener {
    private static final UiFrameGlStateFence HUD_GL_STATE_FENCE = new UiFrameGlStateFence();

    /**
     * HUD 帧的采样界面名。
     *
     * <p>HUD 不是 GuiScreen，故用固定名；它与屏幕宿主的类名各成一组，二者的帧时间历史互不覆盖
     * （见 {@link UiPerformanceMonitor} 的按界面分组历史）。</p>
     */
    private static final String HUD_SAMPLE_SCREEN = "hud";
    private final ClientHudServiceImpl service = ClientHudServiceImpl.getInstance();
    private final SceneHudHost host = new SceneHudHost(service);
    // 共享渲染资源（离屏层池 + 快照池）的生命周期与 listener 同长，即与进程同长：
    // 两处池都有界（layer 池按借用计数复用并显式归还/丢弃；snapshot 池上限 32 并驱逐最旧未活跃槽），
    // 不是逐帧新建，故不需要 JVM 退出阶段的释放——
    // 且退出阶段多在非渲染线程，碰 GL 会触发 native 崩溃（先例见 FontService.shutdown 的跳过分支）。
    // 唯一必须释放的真实时机是 GL context 重建（宿主换渲染后端/重建窗口）：届时池里的 FBO/纹理 id 全部失效，
    // 必须在此接线 UiHostRenderSupport.closeSharedRenderResources 复位两处池，再让下一帧惰性重建。
    // 当前仓内没有 context 重建检测点（属已知缺口，登记在 GL 自净审查报告 N14）。
    private final PaintContextCompositor compositor = new PaintContextCompositor();
    private final UiMainLayerSnapshotService snapshots = new UiMainLayerSnapshotService();
    private final MinecraftHudEnvironment environment;
    /**
     * UI 环境端口（诊断开关的唯一来源）。
     *
     * <p>组合根持有：HUD 帧的采样会话与调试浮层的显隐都读它，不再直读配置静态字段。
     * 默认与 HUD 各窗口的 runtime 同源（{@link SceneHostAssembly#defaultEnvironment()}）。</p>
     */
    private final UiEnvironment uiEnvironment;
    /** debug HUD 显示的当前界面名（每帧在 renderHudFrame 更新）。 */
    private final Signal<String> debugScreenName = Signal.create("null");

    /** 创建 bridge，并把 UILib debug 文本注册为普通统一 HUD。 */
    public UiHudRenderListener() {
        this(new LiveMinecraftHudEnvironment(), SceneHostAssembly.defaultEnvironment());
    }

    /** 创建使用指定 Minecraft 环境的 bridge。 */
    UiHudRenderListener(MinecraftHudEnvironment environment) {
        this(environment, SceneHostAssembly.defaultEnvironment());
    }

    /** 创建使用指定 Minecraft 环境与 UI 环境端口的 bridge。 */
    UiHudRenderListener(MinecraftHudEnvironment environment, UiEnvironment uiEnvironment) {
        this.environment = environment;
        this.uiEnvironment = uiEnvironment;
        registerDebugHud();
    }

    /** 从生产环境提取 framebuffer 视口；GUI scale 只保留在环境诊断面。 */
    HudViewportMetrics viewport() {
        return viewport(environment);
    }

    /** 把指定生产环境提取为 framebuffer 视口。 */
    static HudViewportMetrics viewport(MinecraftHudEnvironment environment) {
        return FramebufferViewportFactory.create(environment.displayWidth(), environment.displayHeight());
    }

    /** 注册 UILib 内部 debug HUD：与业务方同款的窗口工厂 + scene 代码。 */
    private void registerDebugHud() {
        service.register(HudSpec.builder("modernui:debug").anchor(HudAnchor.TOP_RIGHT)
                .visibility(HudVisibility.IN_WORLD).stackOrder(Integer.MIN_VALUE).build(),
                rt -> {
                    blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode root = blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode.row()
                            .setHitTestable(false);
                    // 调试浮层开关：订阅诊断域的进程级通道（原为 Computed.create(() -> Config.uiDebug)，
                    // 那是没有任何依赖的一次性快照 —— 关闭→开启不重算，HUD 再也不出现）。
                    // 关闭时卸载内容树 → 空内容整窗隐藏（对齐旧 EMPTY 快照语义）。
                    rt.show(root, rt.environment().diagnostics().debugOverlayChanges(),
                            () -> {
                                blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode line =
                                        blocamlimb.nhx100218.modernui.uilib.ui.scene.node.SceneNode.row()
                                                .setHitTestable(false)
                                                .setTextColor(blocamlimb.nhx100218.modernui.uilib.ui.scene.paint.SceneChromeTokens.HUD_TEXT_MUTED)
                                                .setFontSize(14);
                                rt.bindText(line, debugScreenName);
                                return line;
                            });
                    return root;
                });
    }

    /** 在 Post(ALL) 中以 framebuffer 尺寸执行 scene layout→paint→replay。 */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event == null || event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) return;
        HudViewportMetrics viewport = viewport();
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        HUD_GL_STATE_FENCE.run(() -> renderHudFrame(event, minecraft, viewport, width, height));
    }

    /**
     * 在已捕获入口状态的围栏内完成一整帧 HUD 业务与清理。
     *
     * <p><b>采样会话位置（踩坑）</b>：本类的 HUD 帧围栏调用形态被源码结构门禁
     * {@code UiHudRenderListenerGlFenceTest} 按<b>字面计数</b>钉死——围栏 token 在源码中必须恰好出现
     * 一次、且先于本方法声明。故：①采样 begin/finish 不得包在围栏外层，也不得在本类的注释里复述
     * 该 token（复述同样会被计入，实测直接红）；②必须置于本方法内，用 try/finally 包住整个帧体与
     * 末尾重抛路径——否则异常路径会把会话泄漏在 ThreadLocal。</p>
     */
    private void renderHudFrame(RenderGameOverlayEvent.Post event, Minecraft minecraft,
            HudViewportMetrics viewport, int width, int height) {
        // 采样会话：HUD 与屏幕宿主是同帧内两条独立帧入口，各自成对 begin/finish；若某 HUD 窗口
        // 内容间接驱动了控件宿主，嵌套由 monitor 的线程内重入深度保护，不重复计数。
        // try/finally 包住整个帧体：两句清理与末尾重抛路径都必须经过 finishFrame，不把会话泄漏在 ThreadLocal。
        UiPerformanceMonitor monitor = UiPerformanceMonitor.getInstance();
        monitor.beginFrame(HUD_SAMPLE_SCREEN, width, height, width, height, uiEnvironment.diagnostics());
        try {
            // A8：值变才写——每帧无条件 set 会向全局调度器持续入队同值 pendingWrite。
            String screenName = currentScreenName();
            if (!screenName.equals(debugScreenName.get())) {
                debugScreenName.set(screenName);
            }
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
            GL11.glOrtho(0, width, height, 0, -1000, 1000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
            Throwable frameFailure = null;
            try {
                UiHostRenderSupport.prepareMainUiRenderState();
                compositor.beginFrame(); snapshots.beginFrame();
                UiRenderContext context = UiHostRenderSupport.createRenderContext(width, height, 0, 0,
                        event.partialTicks, compositor, snapshots, UiRuntimeAdapters.empty());
                host.render(context, viewport, minecraft.theWorld != null, minecraft.currentScreen != null);
            } catch (RuntimeException failure) {
                frameFailure = failure;
            } catch (Error failure) {
                frameFailure = failure;
            }
            Throwable cleanupFailure = finishHudFrame();
            if (cleanupFailure != null) {
                if (frameFailure != null) cleanupFailure.addSuppressed(frameFailure);
                throwUnchecked(cleanupFailure);
            }
            if (frameFailure != null) throwUnchecked(frameFailure);
        } finally {
            monitor.finishFrame();
        }
    }

    /** 两个帧清理互不短路，后续失败作为 suppressed 保留。 */
    private Throwable finishHudFrame() {
        Throwable failure = null;
        try {
            snapshots.finishFrame();
        } catch (RuntimeException cleanupFailure) {
            failure = cleanupFailure;
        } catch (Error cleanupFailure) {
            failure = cleanupFailure;
        }
        try {
            compositor.finishFrame();
        } catch (RuntimeException cleanupFailure) {
            if (failure == null) failure = cleanupFailure; else failure.addSuppressed(cleanupFailure);
        } catch (Error cleanupFailure) {
            if (failure == null) failure = cleanupFailure; else failure.addSuppressed(cleanupFailure);
        }
        return failure;
    }

    /** 重抛 HUD 生命周期捕获的 unchecked 失败。 */
    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        throw (Error) failure;
    }

    /** 世界生命周期结束时仅释放 session scene；mod 级 registration 保留供重连复用。 */
    public void clearWorld() { host.clearWorld(); }

    /** 客户端世界卸载时立即释放保留 scene。 */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) clearWorld();
    }

    private static String currentScreenName() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.currentScreen == null ? "null" : minecraft.currentScreen.getClass().getName();
    }
}
