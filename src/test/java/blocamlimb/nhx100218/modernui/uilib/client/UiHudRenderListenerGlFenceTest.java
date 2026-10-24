package blocamlimb.nhx100218.modernui.uilib.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/** listener 的唯一 HUD 帧围栏位置与禁用 API 源码结构门禁。 */
public class UiHudRenderListenerGlFenceTest {
    private static final Path LISTENER = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/client/UiHudRenderListener.java");
    private static final Path FENCE = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/host/UiFrameGlStateFence.java");
    private static final Path SCREEN = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/screen/McScreenBridge.java");

    @Test
    public void ignoredEventsReturnBeforeTheSingleFrameFence() throws Exception {
        String source = source(LISTENER);
        int eventCheck = source.indexOf("event == null || event.type != RenderGameOverlayEvent.ElementType.ALL");
        int minecraftCheck = source.indexOf("if (minecraft == null) return;");
        int fence = source.indexOf("HUD_GL_STATE_FENCE.run(");

        assertTrue(eventCheck >= 0);
        assertTrue(minecraftCheck > eventCheck);
        assertTrue(fence > minecraftCheck);
        assertEquals(1, occurrences(source, "HUD_GL_STATE_FENCE.run("));
    }

    @Test
    public void completeHudLifecycleAndBothCleanupsStayInsideFenceDelegate() throws Exception {
        String source = source(LISTENER);
        int fence = source.indexOf("HUD_GL_STATE_FENCE.run(() -> renderHudFrame(");
        int renderMethod = source.indexOf("private void renderHudFrame(");
        int projection = source.indexOf("GL11.glMatrixMode(GL11.GL_PROJECTION)", renderMethod);
        int prepare = source.indexOf("UiHostRenderSupport.prepareMainUiRenderState()", renderMethod);
        int compositorBegin = source.indexOf("compositor.beginFrame()", renderMethod);
        int snapshotsBegin = source.indexOf("snapshots.beginFrame()", renderMethod);
        int context = source.indexOf("UiHostRenderSupport.createRenderContext(", renderMethod);
        int hostRender = source.indexOf("host.render(", renderMethod);
        int cleanup = source.indexOf("finishHudFrame()", renderMethod);

        assertTrue(fence >= 0);
        assertTrue(renderMethod > fence);
        assertTrue(projection > renderMethod);
        assertTrue(prepare > projection);
        assertTrue(compositorBegin > prepare);
        assertTrue(snapshotsBegin > compositorBegin);
        assertTrue(context > snapshotsBegin);
        assertTrue(hostRender > context);
        assertTrue(cleanup > hostRender);
        assertTrue(source.indexOf("snapshots.finishFrame()", cleanup) > cleanup);
        assertTrue(source.indexOf("compositor.finishFrame()", cleanup) > cleanup);
        assertFalse(source.contains("glPushMatrix"));
        assertFalse(source.contains("glPopMatrix"));
    }

    /**
     * 负向清单：守卫不得使用重兼容/诊断 API。
     *
     * <p>本方法原先只有十条 {@code assertFalse}，**没有任何正锚**：把 {@code UiFrameGlStateFence}
     * 整个掏空（甚至删掉那十条名字涉及的实现）也照样全绿。四条正锚的作用是先证明
     * 「读到了真的守卫源码」，再让负向清单有意义——R1 内聚轮（2026-09-04）补。
     * GL11 出现次数下界取自实测（本文件 {@code GL11.} 共 62 处），骤降即说明捕获/恢复被拆。</p>
     */
    @Test
    public void guardAvoidsHeavyCompatibilityFboAndDiagnosticApis() throws Exception {
        String source = source(FENCE);
        assertTrue("守卫源码必须读得到（空内容会让下面十条负向断言全部空转）", source.length() > 1000);
        assertTrue("守卫必须仍持有可注入的 GL 抽象", source.contains("private final GlAccess gl;"));
        assertTrue("围栏入口 run 必须存在", source.contains("void run(Runnable frame)"));
        assertTrue("捕获必须真读 GL 状态", source.contains("gl.isEnabled(GL11.GL_DEPTH_TEST)"));
        assertTrue("恢复路径必须存在", source.contains("private void restore()"));
        assertTrue("GL11 用量骤降说明捕获/恢复被拆（实测 62 处，地板 30）", occurrences(source, "GL11.") >= 30);

        // 两种形态都拦（裸 GL11.* 与 P8 起的 GlStateDiagnostics.*）。
        assertFalse(source.contains("glPushAttrib") || source.contains("pushAttrib("));
        assertFalse(source.contains("glPushClientAttrib") || source.contains("pushClientAttrib("));
        assertFalse(source.contains("glClientActiveTexture"));
        assertFalse(source.contains("GL_TEXTURE_MATRIX"));
        assertFalse(source.contains("GL_TEXTURE_STACK_DEPTH"));
        assertFalse(source.contains("Tessellator"));
        assertFalse(source.contains("glGetError"));
        // 「重兼容 FBO」的负向清单在 N11 之后按**意图**改写：围栏现在需要读回/写回宿主的
        // draw/read framebuffer 绑定（状态守恒，属 N11），但仍然不得创建/删除 FBO、读写 FBO 像素，
        // 也不得引用 MC 的 Framebuffer 包装类或渲染缓冲。原先按子串 "Framebuffer" 拦会把
        // glBindFramebuffer 一并拦掉，等同于禁止状态守恒。
        assertFalse("不得引用 MC 的 Framebuffer 包装类", source.contains("net.minecraft.client.shader.Framebuffer"));
        assertFalse("不得走 MC 的 getFramebuffer() 兼容路径", source.contains("getFramebuffer()"));
        assertFalse("不得创建 FBO", source.contains("glGenFramebuffers"));
        assertFalse("不得删除 FBO", source.contains("glDeleteFramebuffers"));
        assertFalse("不得读写 FBO 像素", source.contains("glReadPixels") || source.contains("glBlitFramebuffer"));
        assertFalse("不得挂 FBO 附件（附件管理属离屏层职责，不是状态守恒）",
                source.contains("glFramebufferTexture") || source.contains("glFramebufferRenderbuffer"));
        assertFalse("不得做 FBO 状态诊断", source.contains("glCheckFramebufferStatus"));
        assertFalse(source.contains("Renderbuffer"));
        assertFalse(source.contains("findDrift"));
    }

    @Test
    public void fenceIsNotReferencedByHudHostNodeCommandOrTextPaths() throws Exception {
        Path clientRoot = Paths.get("src/main/java/blocamlimb/nhx100218/modernui/uilib/client");
        int references = 0;
        try (java.util.stream.Stream<Path> files = Files.walk(clientRoot)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                references += occurrences(source(file), "UiFrameGlStateFence");
            }
        }
        // client 包内只允许 HUD listener 持有围栏：import + 字段类型 + 构造调用 = 3 处。
        assertEquals("围栏只允许由唯一 Forge HUD listener 在 client 包持有", 3, references);
    }

    /**
     * 屏幕帧必须被同一套围栏包住（GL 自净审查 N1 的落地判据）。
     *
     * <p>此前的屏幕入口只恢复矩阵栈，viewport 与 enable 位全部留给宿主；本用例同时钉住"围栏包住整帧"
     * 与"attrib 深度回收在 finally 内"（N10，异常重抛路径同样要回收）。</p>
     */
    @Test
    public void screenFrameIsWrappedByTheSameFenceAndReclaimsAttribDepthInFinally() throws Exception {
        String source = source(SCREEN);
        assertEquals(1, occurrences(source, "SCREEN_GL_STATE_FENCE.run("));

        // 包含性断言，不是顺序比较：帧体的每一段都必须落在围栏 lambda 体内。
        // 独立复核实测过「空壳围栏 + 帧体挪到围栏外」的变异会存活在纯顺序比较上，故此处按块体包含判定。
        String fenced = blockAfter(source, "SCREEN_GL_STATE_FENCE.run(");
        assertTrue("帧作用域建立必须在围栏内", fenced.contains("UiHostRenderSupport.beginMainUiFrame("));
        assertTrue("合成器帧必须在围栏内", fenced.contains("paintContextCompositor.beginFrame()"));
        assertTrue("快照服务帧必须在围栏内", fenced.contains("mainLayerSnapshotService.beginFrame()"));
        assertTrue("主渲染必须在围栏内", fenced.contains("surface.render(logicalWidth, logicalHeight, context, 0, 0)"));
        assertTrue("两个帧清理必须在围栏内", fenced.contains("mainLayerSnapshotService.finishFrame()")
                && fenced.contains("paintContextCompositor.finishFrame()"));
        assertTrue("宿主背景绘制必须在围栏内（它经原版 Tessellator 改 LIGHTING/FOG）",
                fenced.contains("drawDefaultBackground()"));

        // attrib 深度回收必须落在 drawScreen 帧体的 finally 块体内（异常重抛路径同样要回收）。
        String method = blockAfter(source, "public void drawScreen(");
        int reclaim = method.indexOf("GlAttribDepth.popExcess(frameBaseDepth)");
        assertTrue("attrib 深度回收必须存在", reclaim >= 0);
        int fenceCall = method.indexOf("SCREEN_GL_STATE_FENCE.run(");
        // 从回收点往前取最近的外层 finally（方法体内先出现的那个 finally 属于帧内的 finishFrame 块）。
        int finallyToken = method.lastIndexOf("} finally {", reclaim);
        assertTrue("围栏必须挂在 try 上", fenceCall >= 0 && finallyToken > fenceCall && finallyToken < reclaim);
        String finallyBody = blockAt(method, finallyToken);
        assertTrue("attrib 深度回收必须在 finally 块体内（N10）",
                finallyBody.contains("GlAttribDepth.popExcess(frameBaseDepth)"));
    }

    /** 读取 UTF-8 生产源码。 */
    private static String source(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /** 取标记之后第一个 '{' 起配平的块体（方法体 / lambda 体通用）。 */
    private static String blockAfter(String source, String marker) {
        int markerIndex = source.indexOf(marker);
        assertTrue("缺少标记：" + marker, markerIndex >= 0);
        return blockAt(source, markerIndex);
    }

    /** 取指定下标之后第一个 '{' 起配平的块体。 */
    private static String blockAt(String source, int markerIndex) {
        int openingBrace = source.indexOf('{', markerIndex);
        assertTrue("缺少起始花括号", openingBrace >= 0);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openingBrace + 1, index);
                }
            }
        }
        fail("块体花括号未配平");
        return "";
    }

    /** 统计固定源码片段出现次数。 */
    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }
}
