package blocamlimb.nhx100218.modernui.uilib;

import blocamlimb.nhx100218.modernui.config.ui.field.PickerSourceGuard;
import blocamlimb.nhx100218.modernui.config.ui.field.PickerSourceLifecycle;
import blocamlimb.nhx100218.modernui.uilib.client.AngelicaHudCachingSuppressor;
import blocamlimb.nhx100218.modernui.uilib.client.FontRenderTickListener;
import blocamlimb.nhx100218.modernui.uilib.client.MinecraftMainThreadOracle;
import blocamlimb.nhx100218.modernui.uilib.client.UiHudRenderListener;
import blocamlimb.nhx100218.modernui.uilib.client.UiInputTickListener;
import blocamlimb.nhx100218.modernui.uilib.client.command.ModernUiClientCommand;
import blocamlimb.nhx100218.modernui.uilib.config.modern.ChatFrameConfig;
import blocamlimb.nhx100218.modernui.uilib.font.FontService;
import blocamlimb.nhx100218.modernui.uilib.i18n.LanguageEpochService;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.input.ChatFrameIntent;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.input.ChatInputOpenListener;
import blocamlimb.nhx100218.modernui.uilib.net.api.NetService;
import blocamlimb.nhx100218.modernui.uilib.net.client.NetStoreUiBridge;
import blocamlimb.nhx100218.modernui.uilib.net.core.MainThreadDispatcher;
import blocamlimb.nhx100218.modernui.uilib.net.transport.NetSide;
import blocamlimb.nhx100218.modernui.uilib.resource.ResourceReloadService;
import blocamlimb.nhx100218.modernui.uilib.ui.image.DocumentRemoteImageCache;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.image.ItemRenderTierRegistry;
import blocamlimb.nhx100218.modernui.uilib.ui.input.UiInputService;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

/**
 * 客户端代理。
 */
public class ClientProxy extends CommonProxy {

    private final FontRenderTickListener fontRenderTickListener = new FontRenderTickListener();
    private final UiHudRenderListener uiHudRenderListener = new UiHudRenderListener();
    private final UiInputTickListener uiInputTickListener = new UiInputTickListener();
    private final AngelicaHudCachingSuppressor angelicaHudCachingSuppressor = new AngelicaHudCachingSuppressor();
    private final ChatInputOpenListener chatInputOpenListener = new ChatInputOpenListener();

    /**
     * 客户端预初始化时注册字体渲染 Tick 监听。
     *
     * @param event Forge 预初始化事件
     */
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // 字体渲染骨架只在客户端引导，且必须晚于 ModernConfigBootstrap（读新栈配置值）、
        // 早于渲染/输入监听注册。判定权威在 FontService 内部，这里不做侧别判断。
        MyMod.LOG.debug("preInit 时序 [client]: FontService.initialize 开始");
        FontService.getInstance().initialize();
        MyMod.LOG.info("字体系统已启用：{}", FontService.getInstance().isInitialized());
        UiInputService.getInstance().initialize();
        // 候选源 SPI 的线程契约：装配期注入客户端判定源，生产路径恒有判定源；未安装时守卫降级放行，
        // 「装配期是否安装」由 PickerSourceGuardWiringTest 的源码守卫钉死（不靠运行时行为）。
        PickerSourceGuard.installThreadOracle(new MinecraftMainThreadOracle());
        // 资源/语言代际通道：进程单例挂到客户端资源管理器。1.7.10 注册即同步回调一次，
        // 故 resourceEpoch 立即从 0 变 1（初始态可见）。字体 reload 通道保持不动，二者不合并。
        // 分级表失效挂在同一条总线上（资源包换图后旧 UNRENDERABLE 不得驻留，改走一次代际广播）。
        ResourceReloadService.getInstance().addListener(epoch -> ItemRenderTierRegistry.invalidateAll("resource_reload"));
        ResourceReloadService.getInstance().registerToClient();
        LanguageEpochService.getInstance().install();
        NetStoreUiBridge.getInstance().initialize();
        registerModernUICommand();
        // 聊天框形态切换（4.10.1）：动作注册与配置持久化分居两层——internal.chat3 只发布切换语义，
        // 写盘/回灌实现在 config.modern（internal 不反向依赖配置包），装配层在此注入两个端口：
        // 先写盘、自定义输入屏按既有 CLOSING 动画收回去，关屏完成后才回灌运行态。
        // 与聊天总开关无关：切回原版后动作仍留在注册表，重新接管时按钮自然回来。
        ChatFrameIntent.install(ChatFrameConfig::persistVanilla, ChatFrameConfig::applyVanilla);
        MinecraftForge.EVENT_BUS.register(fontRenderTickListener);
        MinecraftForge.EVENT_BUS.register(uiHudRenderListener);
        MinecraftForge.EVENT_BUS.register(chatInputOpenListener);
        FMLCommonHandler.instance().bus().register(fontRenderTickListener);
        FMLCommonHandler.instance().bus().register(uiInputTickListener);
        // 宿主兼容：Angelica 的 HUD 缓存会让 UILib 背景滤镜（液态玻璃）采样不到世界画面 ⇒ 强制停用。
        // 挂 tick 而非 HUD 帧：HUD 帧本身会被宿主的 HUDCachingEarlyReturnTransformer 提前返回。
        FMLCommonHandler.instance().bus().register(angelicaHudCachingSuppressor);
        FMLCommonHandler.instance().bus().register(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::onJvmShutdown, "ModernUIShutdown"));
    }

    /**
     * 注册 {@code /modernui} 客户端命令。
     *
     * <p>内部开发工具（{@code internal.devtools}）整包不进发布产物——打包排除与
     * {@code verifyDevToolsNotPackaged} 门禁见 build.gradle.kts。故此处按「开发环境 + 类确实存在」
     * 双重探测：命中则交给开发工具装配（完整命令含 test/glass 场地，并连带注册网络自检端点集），
     * 否则注册发布产物内的通道命令。探测走 {@code Class.forName} 而非静态引用——发布产物里没有
     * 该包，静态引用会让本代理在校验期就解析失败。</p>
     *
     * <p>自检端点只在客户端注册：唯一驱动者就是客户端命令；在服务端注册只会白起常驻线程，
     * 并把 13 个调试端点暴露给任意客户端。</p>
     */
    private static void registerModernUICommand() {
        if (registerInternalDevTools()) {
            return;
        }
        ClientCommandHandler.instance.registerCommand(new ModernUiClientCommand());
    }

    /**
     * 开发环境装配内部开发工具客户端能力。
     *
     * @return true 表示已按开发环境完整命令装配；false 表示调用方应回退到发布产物内命令
     */
    private static boolean registerInternalDevTools() {
        if (!Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            return false;
        }
        try {
            Class<?> bootstrap = Class.forName("blocamlimb.nhx100218.modernui.uilib.internal.devtools.DevToolsClientBootstrap",
                    true, ClientProxy.class.getClassLoader());
            bootstrap.getMethod("registerClientDevTools").invoke(null);
            return true;
        } catch (ClassNotFoundException absentFromRelease) {
            return false;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("内部开发工具客户端装配失败", failure);
        }
    }

    /**
     * 客户端从服务端断连时清理缓存与网络状态。
     *
     * <p>在 1.7.10 Forge 中 {@link FMLNetworkEvent.ClientDisconnectionFromServerEvent} 同时覆盖单人退出与多人断连。</p>
     *
     * @param event 客户端断连事件
     */
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // HUD 宿主（保留窗口 + 独立 scene runtime）与渲染分级表的写路径只允许客户端主线程
        // （契约见 blocamlimb.nhx100218.modernui.uilib.client.hud.HudClientThread）。本事件在网络线程触发，直接调用
        // 会与主线程的 WorldEvent.Unload 清理并发——实测退出世界时 SceneHudHost.clearWorld 的
        // 保留窗口表在迭代中被清空（ConcurrentModificationException）。与下方候选源 SPI 同口径：
        // 入队客户端主线程，由 ClientTickEvent 排空后再清理；排空通道在断连后照常运行（主菜单也
        // tick），故清理不会被漏掉。
        try {
            MainThreadDispatcher.getInstance().enqueue(NetSide.CLIENT, new Runnable() {
                @Override
                public void run() {
                    try {
                        uiHudRenderListener.clearWorld();
                    } catch (RuntimeException exception) {
                        MyMod.LOG.warn("HUD 断连清理异常", exception);
                    }
                    try {
                        // 世界退出/断连：分级结论与跨世界旧图标不再有效（与候选源 release() 同批语义）。
                        ItemRenderTierRegistry.invalidateAll("client_disconnect");
                    } catch (RuntimeException exception) {
                        MyMod.LOG.warn("渲染分级表失效异常", exception);
                    }
                }
            });
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("HUD/分级表断连清理派发异常", exception);
        }
        try {
            DocumentRemoteImageCache.getInstance().clear();
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("远程图片缓存断连清理异常", exception);
        }
        try {
            NetService.getInstance().onClientDisconnected();
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("网络层断连清理异常", exception);
        }
        try {
            // 候选源 SPI 的会话级释放点（SPI javadoc：客户端断开 / 世界退出；**不在关屏调用**）。
            // 本事件在网络线程触发，而 SPI 全方法只允许客户端主线程（ADR A-01）⇒ 先派发到客户端主线程
            // 队列，由 ClientTickEvent（ForgeMainThreadDispatcherBridge）排空后再释放；
            // 排空通道在断连后照常运行（主菜单也 tick），故释放不会被漏掉。
            MainThreadDispatcher.getInstance().enqueue(NetSide.CLIENT, new Runnable() {
                @Override
                public void run() {
                    PickerSourceLifecycle.releaseAll("client_disconnect");
                }
            });
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("候选源会话释放派发异常", exception);
        }
    }

    private void onJvmShutdown() {
        try {
            DocumentRemoteImageCache.getInstance().shutdown();
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("远程图片缓存关停异常", exception);
        }
        try {
            FontService.getInstance().shutdown();
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("字体系统关停异常", exception);
        }
        try {
            NetService.getInstance().shutdown();
        } catch (RuntimeException exception) {
            MyMod.LOG.warn("网络层关停异常", exception);
        }
    }
}
