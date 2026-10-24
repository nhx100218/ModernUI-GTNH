package blocamlimb.nhx100218.modernui.uilib.ui.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * GL 自净修复（审查 P2 批）的结构契约：异常窗口必须与正常路径共用同一个保护域。
 *
 * <p>为什么用源码结构而不是替身：{@code UiRenderTarget} / {@code UiRenderContext} / {@code FontShaderProgram}
 * 直接链接 LWJGL 静态方法与固定管线立即模式，没有可注入的状态缝隙，异常分支在纯 JVM 里无法触发；
 * 而这些修复的目标恰恰只有异常路径才体现（正常路径改动前后逐位相同）。
 * 断言只钉「结构不变量」（回绑在 finally 内、保护域在 try 内、复位与 close 解耦），改回去即红。</p>
 *
 * <p>对应条目见 {@code docs/历史报告/审查/2026-09-19-GL使用自净审查.md}：P2 批 N2 / N3 / N5 / N7 / N16，
 */
public class GlStateRecoveryContractTest {

    private static final Path RENDER_TARGET = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/UiRenderTarget.java");
    private static final Path CLIP_STACK = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/ClipStack.java");
    private static final Path RENDER_CONTEXT = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/UiRenderContext.java");
    private static final Path BACKDROP = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/UiBackdropFilterRenderer.java");
    private static final Path FONT_SHADER = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/font/shader/FontShaderProgram.java");
    private static final Path SNAPSHOT_SERVICE = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/UiMainLayerSnapshotService.java");
    private static final Path COMPOSITOR = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/PaintContextCompositor.java");
    private static final Path FONT_GUARD = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/font/render/FontRenderStateGuard.java");
    private static final Path BACKDROP_SHADER = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/render/UiBackdropShaderProgram.java");
    private static final Path GL_ATTRIB_DEPTH = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/util/GlAttribDepth.java");
    private static final Path FRAME_FENCE = Paths.get(
            "src/main/java/blocamlimb/nhx100218/modernui/uilib/ui/host/UiFrameGlStateFence.java");

    /** N2：三处回贴的纹理回绑必须在 finally 内——异常路径同样要还原入口绑定。 */
    @Test
    public void textureRebindingHappensInsideFinally() throws Exception {
        String source = source(RENDER_TARGET);
        // 针不带分号：P7 起三处回绑都在 restoreStep 的 lambda 里（"…previousTextureBinding));"）。
        assertEquals("三处回贴都要读回原值并回绑", 3,
                occurrences(source, "GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextureBinding)"));
        assertTrue("必须真的读回入口绑定", source.contains("int previousTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);"));
        assertFalse("回绑必须搬进 finally，不能停在 try 体末尾",
                source.contains("previousTextureBinding);\n        } finally {"));
    }

    /** N3：圆角掩码重建的写掩码复原必须在 finally 内——否则 colorMask 全关被悬挂，本帧后续绘制静默不写色。 */
    @Test
    public void roundedClipMaskRestoresWriteMasksInFinally() throws Exception {
        String body = blockAfter(source(CLIP_STACK), "private static void rebuildRoundedClipMask(");
        int finallyToken = body.indexOf("} finally {");
        assertTrue("掩码重建必须带 finally", finallyToken >= 0);
        assertTrue("colorMask 复原必须在 finally 块体内",
                body.indexOf("ops.colorMask(true, true, true, true);", finallyToken) > finallyToken);
        assertTrue("depthMask 复原必须在 finally 块体内",
                body.indexOf("ops.depthMask(true);", finallyToken) > finallyToken);
    }

    /** N5：文本绘制的矩阵弹出必须包在 finally 内——委托的是第三方字体实现。 */
    @Test
    public void textMatrixPopHappensInsideFinally() throws Exception {
        String source = source(RENDER_CONTEXT);
        assertTrue("至少一处文本绘制把 glPopMatrix 放进了 finally",
                occurrences(source, "} finally {\n            GL11.glPopMatrix();") >= 1);
        assertFalse("不存在 push 后直接 pop 的裸序列",
                source.contains("GL11.glPopMatrix();\n        notifyMainLayerContentChanged();"));
    }

    /** N7：背景滤镜的保护域（clip 栈 + attrib 帧）必须建立在 try 之内。 */
    @Test
    public void backdropProtectionDomainStartsInsideTry() throws Exception {
        String source = source(BACKDROP);
        assertTrue("必须用标志位条件化恢复", source.contains("boolean clipPushed = false;")
                && source.contains("boolean attribPushed = false;"));
        assertFalse("pushClip / pushAttrib 不得留在 try 外（P8 起 push 经插桩工具，两种形态都要拦）",
                source.contains("context.pushClip(left, top, right, bottom, 0);\n        GlStateDiagnostics.pushAttrib")
                        || source.contains("context.pushClip(left, top, right, bottom, 0);\n        GL11.glPushAttrib"));
    }

    /** N16：初始化失败的复位必须与 close 解耦——close 自身在 GL 入口不可用时也会抛。 */
    @Test
    public void shaderInitializeResetsStateEvenWhenCloseFails() throws Exception {
        String source = source(FONT_SHADER);
        assertEquals("两个失败分支（RuntimeException / Error）都要走同一复位入口", 2,
                occurrences(source, "resetAfterFailedInitialize();"));
        assertTrue("复位必须把 initialized 的归位放进 finally",
                source.contains("try {\n            close();\n        } finally {\n            initialized.set(false);\n        }"));
    }

    /** N9：两处恢复链必须逐步累积失败，不能前项失败就跳过后续。 */
    @Test
    public void restoreChainsAccumulateFailures() throws Exception {
        String snapshotService = source(SNAPSHOT_SERVICE);
        assertTrue("快照服务每一步恢复都要经 restoreStep",
                occurrences(snapshotService, "restoreStep(restoreFailure,") >= 6);
        assertTrue("快照服务恢复失败必须统一抛出",
                snapshotService.contains("if (restoreFailure != null) {"));
        assertTrue("背景滤镜每一步恢复都要经 restoreStep",
                occurrences(source(BACKDROP), "restoreStep(restoreFailure,") >= 7);
    }

    /**
     * N9 + 低危复核项：变换层的 T 矩阵弹出必须无条件执行，且弹栈失败不得替换回贴失败。
     *
     * <p>原实现用 try/finally：finally 内弹栈若抛，会顶掉 composite 的原异常。现实现改为
     * 「两段 try，各自把失败累积进 rememberFailure，最后统一 rethrow」——弹栈仍在回贴之后
     * 无条件执行，但不再吞掉或替换主异常。</p>
     */
    @Test
    public void transformLayerPopsMatrixUnconditionallyAndAccumulatesPopupFailure() throws Exception {
        String body = blockAfter(source(COMPOSITOR), "boolean popTransformLayer()");
        int compositeCatch = body.indexOf("rememberFailure(failure, compositeFailure);");
        // 注意：本方法开头还有一次 MODELVIEW pop（步骤 1），故必须从 composite 之后找 T 的弹出。
        int popToken = body.indexOf("GL11.glPopMatrix();", compositeCatch);
        int popCatch = body.indexOf("rememberFailure(failure, popFailure);");
        assertTrue("回贴失败必须先累积再继续弹栈", compositeCatch >= 0);
        assertTrue("T 矩阵弹出必须在回贴尝试之后执行",
                popToken > compositeCatch && body.lastIndexOf("try {", popToken) > compositeCatch);
        // 「无条件执行」必须真的验：弹栈所在 try 与回贴 catch 之间不得出现条件/提前返回
        // （否则「回贴失败就不弹栈」这种变异会全绿放行——独立复核实测过该变异）。
        String betweenCompositeAndPop = body.substring(compositeCatch, popToken);
        assertFalse("弹栈不得被条件化：回贴 catch 与弹栈之间出现 if/else/return 即红",
                betweenCompositeAndPop.contains("if (") || betweenCompositeAndPop.contains("else")
                        || betweenCompositeAndPop.contains("return"));
        assertTrue("弹栈失败必须累积而不是替换回贴失败", popCatch > popToken);
        assertTrue("失败必须统一 rethrow 且两个失败都进了同一个累积器",
                body.contains("rethrow(failure[0]);"));
    }

    /** N15：字体围栏 push 必须能在中途失败时回滚，pop 必须累积失败。 */
    @Test
    public void fontGuardPushRollsBackAndPopAccumulates() throws Exception {
        String source = source(FONT_GUARD);
        assertEquals("push 的三个失败分支都要回滚", 3,
                occurrences(source, "rollbackPush(matrixStacksPushed, clientAttribPushed, attribPushed, state,"));
        assertTrue("pop 必须经 recordFailure 累积", occurrences(source, "recordFailure(failure,") >= 15);
        assertTrue("pop 失败必须统一抛出", source.contains("throwUnchecked(failure);"));
    }

    /** N8：背景滤镜着色器的失败路径必须回收两个 shader 与 program。 */
    @Test
    public void backdropShaderLoadProgramCleansUpOnFailure() throws Exception {
        String body = blockAfter(source(BACKDROP_SHADER), "private void loadProgram()");
        int finallyToken = body.indexOf("} finally {");
        assertTrue("loadProgram 必须带 finally", finallyToken >= 0);
        String finallyBody = blockAt(body, finallyToken);
        assertTrue("顶点 shader 回收必须在 finally 块体内",
                finallyBody.contains("GL20.glDeleteShader(vertexShaderId);"));
        assertTrue("片元 shader 回收必须在 finally 块体内",
                finallyBody.contains("GL20.glDeleteShader(fragmentShaderId);"));
        assertTrue("必须用 !linkedSuccessfully 守卫避免成功路径误删 program",
                finallyBody.contains("!linkedSuccessfully"));
        assertTrue("未链接成功时 program 也要回收",
                finallyBody.contains("GL20.glDeleteProgram(shaderProgramId);"));
    }

    /** R19 + N13：帧围栏必须自带 attrib 深度兜底与能力留痕。 */
    @Test
    public void frameFenceReclaimsAttribDepthAndWarnsWhenUnavailable() throws Exception {
        String source = source(FRAME_FENCE);
        assertTrue("capture 必须把帧起点深度写进快照",
                source.contains("snapshot.attribDepth = blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.current();"));
        assertTrue("restore 必须按量弹出", source.contains("GlAttribDepth.popExcess(snapshot.attribDepth);"));
        // 两种形态都要拦：裸 GL11.glPushAttrib 与 P8 起的 GlStateDiagnostics.pushAttrib（属性组栈一律不用）。
        assertFalse("围栏不使用 attrib 属性组栈（不依赖其 core profile 可用性）",
                source.contains("glPushAttrib") || source.contains("pushAttrib("));
        assertFalse("围栏不得用 glGetError 清错误队列（历史 ERROR 记录）", source.contains("glGetError"));
        assertFalse("围栏不得做栈参数查询（会留 GL 错误码）", source.contains("GL_MAX_ATTRIB_STACK_DEPTH"));
        assertTrue("attrib 栈降级留痕由 GlAttribDepth 承担（warn-once）",
                source(GL_ATTRIB_DEPTH).contains("LOG.warn"));
    }

    /** N15：push 回滚必须按 LIFO 逐项弹出（TEXTURE→PROJECTION→MODELVIEW），且每段独立累积。 */
    @Test
    public void fontGuardRollbackPopsMatrixStacksInLifoOrder() throws Exception {
        String body = blockAfter(source(FONT_GUARD), "private void rollbackPush(");
        int texture = body.indexOf("popMatrixStack(GL11.GL_TEXTURE)");
        int projection = body.indexOf("popMatrixStack(GL11.GL_PROJECTION)");
        int modelview = body.indexOf("popMatrixStack(GL11.GL_MODELVIEW)");
        assertTrue("三段弹出都要在回滚里且顺序为 LIFO", texture >= 0 && projection > texture && modelview > projection);
        assertTrue("每段都必须经 recordFailure 累积（前段失败不跳过后续）",
                occurrences(body, "recordFailure(rollbackFailure,") >= 4);
    }

    /**
     * R3（P6 定论）：backdrop 在合成层内的采样源取父层的 <b>draw</b> 绑定；无合成层的回退仍取 read
     * 绑定，且这是写明的有意契约。
     *
     * <p>原判"取 draw 绑定 = 采样错 FBO"不成立：内容在父层 draw 目标里，快照服务再把它绑成
     * {@code GL_READ_FRAMEBUFFER} 做 {@code glCopyTexSubImage2D}——宿主 draw/read 分离时，采 read 侧
     * 才是错的。回退路径保留 read 与 Angelica HUD caching 的抑制机制耦合，改行为需真机复测，
     * 故此用例把两侧语义分别钉住。</p>
     */
    @Test
    public void backdropSourceFramebufferUsesDrawBindingAndDocumentsReadFallback() throws Exception {
        String compositor = source(COMPOSITOR);
        assertEquals("两处父层捕获都必须走同一个捕获入口", 2,
                occurrences(compositor, "int parentFramebufferId = captureParentFramebufferId();"));
        assertTrue("必须读 draw 绑定（语义等同 GL_FRAMEBUFFER_BINDING，名字写明避免再被读成误用）",
                compositor.contains("GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);"));
        assertFalse("不得残留易误读的 GL_FRAMEBUFFER_BINDING 查询（全限定形态）",
                compositor.contains("glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING)"));
        assertFalse("不得残留易误读的 GL_FRAMEBUFFER_BINDING 查询",
                compositor.contains("glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)"));
        assertTrue("访问器名必须写明是采样源而不是 read 绑定",
                compositor.contains("int getCurrentBackdropSourceFramebufferId()"));

        String fallback = blockAfter(source(SNAPSHOT_SERVICE), "private static int resolveReadFramebufferId(");
        assertTrue("无合成层的回退仍取 read 绑定（有意契约，与 Angelica HUD caching 抑制耦合）",
                fallback.contains("GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);"));
        assertTrue("回退语义必须在源码里写明理由（判据 5）",
                source(SNAPSHOT_SERVICE).contains("回退路径仍取 read 绑定"));
    }

    /**
     * P7：背景滤镜 finally 的<b>在飞异常合并</b>——try 体失败必须与恢复失败合并，不得被 finally 的
     * 抛出替换（P4 挂账项）。
     *
     * <p>为什么用源码结构：这条路径只有"try 体抛 + 恢复也抛"时才体现，纯 JVM 替身跑不出来；
     * 而"改回裸 try/finally"是极易发生的回退（少写一个 catch 块即可），故钉结构不变量。</p>
     */
    @Test
    public void backdropFinallyMergesInFlightBodyFailure() throws Exception {
        String source = source(BACKDROP);
        assertTrue("必须显式记录 try 体失败", source.contains("Throwable bodyFailure = null;"));
        assertEquals("三个 catch 分支都要捕获（RuntimeException/LinkageError/Error；LinkageError 单列是为与"
                + " GlStateScope/restoreStep 口径逐字对齐，语义上被 Error 覆盖）", 3,
                occurrences(source, "bodyFailure = failure;"));
        assertTrue("必须走仓内统一合并口径（恢复失败为主、try 体失败 suppressed、致命 Error 升级）",
                source.contains("Throwable combined = appendFailure(restoreFailure, bodyFailure);"));
        assertFalse("不得再出现裸 throwUnchecked(restoreFailure)（会替换在飞异常）",
                source.contains("throwUnchecked(restoreFailure);"));
        assertTrue("必须保留不可达出口的 fail-loud", occurrences(source, "try 体失败未被重抛") == 1);
        assertTrue("合并结果必须真的作为抛出源（只留合并语句不抛会被 fail-loud 兜住，但异常类型退化）",
                occurrences(source, "throwUnchecked(combined);") == 1);
    }

    /**
     * P7 复核项：三处 present/composite 的 finally 必须"先弹 attrib 帧、再回绑"，且逐步累积失败。
     *
     * <p>原顺序是"先回绑再 pop"：回绑一旦抛异常，attrib 帧永不弹出（栈泄漏跨帧累积）。改为 restoreStep
     * 累积后，pop 在最前，后续步骤失败不再跳过它。</p>
     */
    @Test
    public void compositeFinallyPopsAttribBeforeRebindingTexture() throws Exception {
        String source = source(RENDER_TARGET);
        assertEquals("三处 finally 都要走累积口径（P8 起 pop 经插桩工具，形态随之变化）", 3,
                occurrences(source, "restoreStep(failure, () -> GlStateDiagnostics.popAttrib(\"UiRenderTarget\"));"));
        assertFalse("不得再出现裸的「先回绑再 pop」序列",
                source.contains("GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextureBinding);\n"
                        + "            GL11.glPopAttrib();"));
        assertEquals("三处都要在末尾统一重抛（含 restoreAfterBegin 的既有出口）", 4,
                occurrences(source, "rethrowCloseFailure(failure[0]);"));
    }

    /**
     * P8：attrib 帧站点必须全部经 {@code GlStateDiagnostics} 插桩（N13/R11 的排错入口）。
     *
     * <p>判据：裸的 {@code GL11.glPushAttrib(...)} 一族只允许出现在插桩工具自身，以及
     * {@code GlAttribDepth.popExcess}（它有自己的 warn-once 留痕，且只做"弹第三方多余层"）。
     * 新增站点若直接调裸 API，Core Profile 真机上就只剩一次没有上下文信息的异常。</p>
     */
    @Test
    public void attribFrameSitesAreInstrumentedForDiagnostics() throws Exception {
        List<String> rawCallFiles = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String source = source(file);
                if (source.contains("GL11.glPushAttrib(") || source.contains("GL11.glPopAttrib(")
                        || source.contains("GL11.glPushClientAttrib(")
                        || source.contains("GL11.glPopClientAttrib(")) {
                    rawCallFiles.add(file.toString().replace('\\', '/'));
                }
            }
        }
        Collections.sort(rawCallFiles);
        assertEquals("裸 attrib 调用只允许在插桩工具与 GlAttribDepth 的自有清理路径里，其余站点必须走"
                        + " GlStateDiagnostics（否则 N13/R11 在真机上没有可排查的日志锚点）",
                Arrays.asList("src/main/java/blocamlimb/nhx100218/modernui/uilib/util/GlAttribDepth.java",
                        "src/main/java/blocamlimb/nhx100218/modernui/uilib/util/GlStateDiagnostics.java"),
                rawCallFiles);
    }

    /**
     * P8/R11：字符页的 GL 错误闸门必须真的把现场交给插桩（含 entry 对照数据）。
     *
     * <p>行为用例只能证明工具方法本身可用；这条结构断言钉住"闸门确实调了它、且传了进入时的排空数据"，
     * 否则插桩在真机上等于没接（删掉调用不会有任何测试变红）。</p>
     */
    @Test
    public void glyphGateFailureIsHandedToDiagnosticsWithEntryComparison() throws Exception {
        String source = source(Paths.get("src/main/java/blocamlimb/nhx100218/modernui/uilib/font/page/GlyphPage.java"));
        assertTrue("闸门失败必须调插桩（R11 现场）",
                source.contains("GlStateDiagnostics.warnGlyphGateFailure(phase, glError, "
                        + "lastEntryGlError, lastEntryGlErrorDrained);"));
        assertTrue("进入时的排空数据必须被记账（否则无法区分第三方遗留与本次 push）",
                source.contains("lastEntryGlError = firstError;"));
    }

    /** 读取 UTF-8 生产源码。 */
    private static String source(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /** 取标记之后第一个 '{' 起配平的块体。 */
    private static String blockAfter(String source, String marker) {
        int markerIndex = source.indexOf(marker);
        assertTrue("缺少标记：" + marker, markerIndex >= 0);
        return blockAt(source, markerIndex);
    }

    /** 取指定下标之后第一个 '{' 起配平的块体。 */
    private static String blockAt(String source, int markerIndex) {
        int openingBrace = source.indexOf('{', markerIndex);
        assertTrue("缺少起始花括号（markerIndex=" + markerIndex + "）", openingBrace >= 0);
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
        fail("块体花括号未配平（markerIndex=" + markerIndex + "）");
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
