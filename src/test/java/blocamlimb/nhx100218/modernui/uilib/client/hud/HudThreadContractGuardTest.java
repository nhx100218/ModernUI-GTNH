package blocamlimb.nhx100218.modernui.uilib.client.hud;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * HUD 线程契约的源码结构守卫。
 *
 * <p><b>为什么需要结构守卫而不是行为测试</b>：本次真实机缺陷（退出世界
 * {@code ConcurrentModificationException}）的成因是「网络线程直接调用 {@code clearWorld}」。
 * 行为测试守不住它——headless 下线程断言按设计放行（无客户端实例），并发窗口又不可复现；
 * 把派发改回直接调用、或删掉宿主入口的断言，整套测试仍然全绿。故按既有范式
 * （{@code PickerSourceGuardWiringTest}）用源码结构钉死两条契约：</p>
 * <ol>
 *   <li>断连清理必须落在 {@code MainThreadDispatcher.enqueue(NetSide.CLIENT, …)} 的派发块内；</li>
 *   <li>HUD 宿主两个状态入口（{@code render} / {@code clearWorld}）必须经
 *       {@link HudClientThread} 断言，且判定只有这一处来源。</li>
 * </ol>
 * <p>两条断言都做过变异验证：把清理移出派发块 / 删掉入口断言 / 在服务实现里重写一份判定，
 * 对应用例立即红。</p>
 */
public class HudThreadContractGuardTest {

    private static final String CLIENT_PROXY = "src/main/java/blocamlimb/nhx100218/modernui/uilib/ClientProxy.java";
    private static final String SCENE_HUD_HOST = "src/main/java/blocamlimb/nhx100218/modernui/uilib/client/hud/SceneHudHost.java";
    private static final String HUD_SERVICE = "src/main/java/blocamlimb/nhx100218/modernui/uilib/client/hud/ClientHudServiceImpl.java";

    @Test
    public void disconnectCleanupRunsInsideClientMainThreadDispatch() throws Exception {
        String proxy = source(CLIENT_PROXY);
        String body = methodBody(proxy, "public void onClientDisconnect(");
        String enqueue = "MainThreadDispatcher.getInstance().enqueue(NetSide.CLIENT";
        int enqueueAt = body.indexOf(enqueue);
        Assert.assertTrue("断连清理必须经客户端主线程派发（enqueue 未找到）", enqueueAt >= 0);
        int runnableBody = body.indexOf('{', enqueueAt);
        Assert.assertTrue("派发任务体未找到", runnableBody >= 0);
        String dispatched = blockAt(body, runnableBody);
        Assert.assertTrue("HUD 断连清理必须落在客户端主线程派发块内（网络线程直接调用会与主线程 WorldEvent.Unload 并发）",
                dispatched.contains("uiHudRenderListener.clearWorld()"));
    }

    @Test
    public void hostStateEntryPointsAssertClientThread() throws Exception {
        String host = source(SCENE_HUD_HOST);
        String render = methodBody(host, "public void render(UiRenderBackend backend, int width, int height,");
        Assert.assertTrue("render 入口必须断言客户端主线程",
                render.contains("HudClientThread.assertCurrent(\"SceneHudHost.render\")"));
        String clearWorld = methodBody(host, "public void clearWorld()");
        Assert.assertTrue("clearWorld 入口必须断言客户端主线程",
                clearWorld.contains("HudClientThread.assertCurrent(\"SceneHudHost.clearWorld\")"));
    }

    @Test
    public void hudThreadCheckHasSingleSource() throws Exception {
        String service = source(HUD_SERVICE);
        Assert.assertTrue("HUD 注册服务必须经 HudClientThread 断言",
                service.contains("HudClientThread.assertCurrent("));
        Assert.assertFalse("线程判定不得在服务实现里另起一份（单一判定源）",
                service.contains("func_152345_ab"));
    }

    private static String source(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }

    /** 取指定声明处的方法体（含外层花括号之间的全部内容）。 */
    private static String methodBody(String source, String declaration) {
        int declarationAt = source.indexOf(declaration);
        Assert.assertTrue("未找到声明：" + declaration, declarationAt >= 0);
        int bodyAt = source.indexOf('{', declarationAt);
        Assert.assertTrue("声明后未找到方法体：" + declaration, bodyAt >= 0);
        return blockAt(source, bodyAt);
    }

    /** 花括号配平取块体（start 指向 '{'；返回不含最外层花括号的内容）。 */
    private static String blockAt(String source, int start) {
        int depth = 0;
        for (int i = start; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start + 1, i);
                }
            }
        }
        throw new AssertionError("源码花括号不配平");
    }
}
