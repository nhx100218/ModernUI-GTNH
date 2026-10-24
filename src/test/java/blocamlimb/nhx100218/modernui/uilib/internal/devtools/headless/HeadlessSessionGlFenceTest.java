package blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * headless 帧围栏结构契约（GL 自净审查 R19）。
 *
 * <p>headless 与 HUD/屏幕共用 {@code UiFrameGlStateFence}；围栏起点刻意在 {@code surface.beginFrame}
 * 之后——surface 自己的 FBO 绑定必须留在围栏外，否则帧末恢复会把当前绑定换回帧前值，
 * 而 {@code GlOffscreenSurface.readPixels()} 读的正是"当前绑定的 framebuffer"。</p>
 *
 * <p>为什么用源码结构：headless 帧需要真实 GL 上下文，纯 JVM 测试跑不起来。断言按<b>块体包含</b>判定
 * （不是顺序比较）：同一缺陷类在 {@code UiHudRenderListenerGlFenceTest} 已被实测过——纯顺序比较会放行
 * 「空壳围栏 + 帧体挪到围栏外」的变异。另加一条负向断言：帧驱动步骤不得留在 {@code advanceOneFrame}
 * 自身（内联回退即红）。</p>
 */
public class HeadlessSessionGlFenceTest {

    private static final Path SESSION = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/internal/devtools/headless/HeadlessSession.java");

    @Test
    public void frameBodyStaysInsideTheSharedFrameFenceAfterSurfaceBegin() throws Exception {
        String source = source();
        // 标记带上方法体开括号：javadoc 里的 {@link ...} 也含 '{'，只按方法名取块会截到注释里去。
        String advanceBody = blockAfter(source, "private void advanceOneFrame(int renderedFrames) {");
        String fenceBody = blockAfter(source, "private void renderOneFrameWithinFence(int renderedFrames) {");

        assertTrue("必须持有与 HUD/屏幕同一实现的帧围栏",
                source.contains("private final UiFrameGlStateFence frameGlStateFence = new UiFrameGlStateFence();"));
        assertEquals("帧循环只能有唯一一处围栏入口", 1, occurrences(source, "frameGlStateFence.run("));
        assertTrue("围栏必须委托给唯一的帧体方法",
                advanceBody.contains("frameGlStateFence.run(() -> renderOneFrameWithinFence(renderedFrames));"));
        assertTrue("帧体方法必须声明在围栏调用之后",
                source.indexOf("private void renderOneFrameWithinFence(int renderedFrames) {")
                        > source.indexOf("frameGlStateFence.run("));

        // 包含性断言：帧体的每一段都必须落在围栏委托方法体内（同 UiHudRenderListenerGlFenceTest 的口径）。
        assertTrue("帧作用域建立必须在围栏内", fenceBody.contains("UiHostRenderSupport.beginMainUiFrame("));
        assertTrue("合成器帧必须在围栏内", fenceBody.contains("paintContextCompositor.beginFrame()"));
        assertTrue("快照服务帧必须在围栏内", fenceBody.contains("mainLayerSnapshotService.beginFrame()"));
        assertTrue("宿主渲染必须在围栏内", fenceBody.contains("host.render(request.width(), request.height(),"
                + " renderContext, 0, 0)"));
        assertTrue("两个帧清理必须在围栏内", fenceBody.contains("mainLayerSnapshotService.finishFrame()")
                && fenceBody.contains("paintContextCompositor.finishFrame()"));

        // 负向断言：帧体不得留在 advanceOneFrame 里（"空壳围栏 + 帧体外置"变异即红）。
        assertFalse("帧前置不得留在围栏外", advanceBody.contains("beginMainUiFrame("));
        assertFalse("宿主渲染不得留在围栏外", advanceBody.contains("host.render("));
        assertFalse("帧清理不得留在围栏外", advanceBody.contains("finishFrame()"));

        // surface FBO 绑定必须在围栏之前（readPixels 依赖它仍在）。
        int resetFrame = advanceBody.indexOf("renderContext.resetFrame();");
        int surfaceBegin = advanceBody.indexOf("surface.beginFrame(request.background());");
        int fenceRun = advanceBody.indexOf("frameGlStateFence.run(");
        assertTrue("surface 帧起点必须存在", resetFrame >= 0 && surfaceBegin > resetFrame);
        assertTrue("围栏起点必须在 surface.beginFrame 之后", fenceRun > surfaceBegin);
        assertEquals("帧体方法只应被围栏调用一次", 1, occurrences(source, "renderOneFrameWithinFence(renderedFrames)"));
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(SESSION), StandardCharsets.UTF_8);
    }

    /** 取标记之后第一个 '{' 起配平的块体；标记请带上方法体的开括号（javadoc 的 {@code {@link}} 也含 '{'）。 */
    private static String blockAfter(String source, String marker) {
        int markerIndex = source.indexOf(marker);
        assertTrue("缺少标记：" + marker, markerIndex >= 0);
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
        fail("块体花括号未配平：" + marker);
        return "";
    }

    /** 统计固定源码片段出现次数。 */
    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }
}
