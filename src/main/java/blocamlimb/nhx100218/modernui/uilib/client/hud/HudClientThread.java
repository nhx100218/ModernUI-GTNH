package blocamlimb.nhx100218.modernui.uilib.client.hud;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD 域「客户端主线程」契约的单一判定源。
 *
 * <p><b>契约</b>：HUD 注册表（{@link ClientHudServiceImpl}）、保留式 HUD 宿主（{@link SceneHudHost}）
 * 及其 scene runtime 的全部读写只在 Minecraft 客户端主线程发生。判定口径与
 * {@code blocamlimb.nhx100218.modernui.uilib.client.MinecraftMainThreadOracle} 同源——1.7.10 原版
 * 「当前线程 == 客户端线程」（SRG 名 {@code func_152345_ab}，MCP stable 12 未映射故按 SRG 调用）。</p>
 *
 * <p><b>为什么必须断言而不是只靠约定</b>：宿主状态是普通 {@code HashMap} 与可变 scene 对象，
 * 跨线程访问不会当场失败，而是在无关位置炸开——实测网络线程的断连清理与主线程
 * {@code WorldEvent.Unload} 并发清空保留窗口表，抛 {@code ConcurrentModificationException}
 * 于 {@code SceneHudHost.clearWorld}。断言把违约变成带站点的明确异常；修复点在调用方
 * （非主线程必须经 {@code MainThreadDispatcher} 派发），不靠加锁掩盖并发访问。</p>
 *
 * <p><b>降级</b>：无客户端运行时（headless 单测 / 服务端 JVM）时放行，并按站点留一条 WARN——
 * 与 D5a「headless 不静默放行」口径一致，也避免渲染路径每帧刷屏。其中「Minecraft 类不可用」
 * 判定一次即<b>永久短路</b>：类初始化失败在 JVM 内是粘性的（后续引用级联
 * {@code NoClassDefFoundError}），反复探测只会重复抛错——对齐踩坑记录
 * {@code docs/反馈层/踩坑记录.md}「headless 测试不能触 Minecraft 类初始化」。「实例尚未建立」
 * 则<b>不</b>短路：装配早期（preInit 注册 HUD）实例可能还没建立，之后仍须照常判定。</p>
 */
final class HudClientThread {

    /** 与既有 HUD 线程日志同名：运维按 logger 名过滤时行为不变。 */
    private static final Logger LOG = LogManager.getLogger("ModernUI HudThread");
    /** 已告警站点（headless 下同一站点只留一条痕迹）。 */
    private static final Set<String> WARNED_SITES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    /** 已判定「Minecraft 类不可用」：后续断言不再触碰该类，直接放行（粘性失败不重复探测）。 */
    private static volatile boolean minecraftClassUnavailable;

    private HudClientThread() {}

    /**
     * 断言当前线程是客户端主线程，否则抛 {@link IllegalStateException}。
     *
     * @param site 违约站点，用于异常消息与告警去重（如 {@code "HUD register/close"}）
     */
    static void assertCurrent(String site) {
        if (minecraftClassUnavailable) {
            warnUnavailable(site, null);
            return;
        }
        final Minecraft minecraft;
        try {
            minecraft = Minecraft.getMinecraft();
        } catch (Throwable unavailable) {
            // headless 测试 JVM / 服务端 JVM：Minecraft 静态初始化失败（LWJGL 链接缺失）。
            // 失败粘着于该 JVM 的类状态，故判定一次即永久放行；生产客户端路径恒有可用实例。
            minecraftClassUnavailable = true;
            warnUnavailable(site, unavailable);
            return;
        }
        if (minecraft == null) {
            warnUnavailable(site, null);
            return;
        }
        final boolean onClientThread;
        try {
            onClientThread = minecraft.func_152345_ab();
        } catch (Throwable unavailable) {
            // 实例在、判定 API 不可用：只本次放行，不短路（否则会永久失去契约检查）。
            warnUnavailable(site, unavailable);
            return;
        }
        if (!onClientThread) {
            throw new IllegalStateException(site + " must run on the client main thread");
        }
    }

    /**
     * 留一条按站点去重的降级痕迹。
     *
     * @param site  违约站点
     * @param cause 判定失败原因；null 表示「Minecraft 不可用」（原文案不变）
     */
    private static void warnUnavailable(String site, Throwable cause) {
        if (WARNED_SITES.add(site)) {
            if (cause == null) {
                LOG.warn("{} 线程校验被跳过（Minecraft 不可用，headless 环境）", site);
            } else {
                LOG.warn("{} 线程校验被跳过（Minecraft 不可用，headless 环境）：{}", site, cause.toString());
            }
        }
    }
}
