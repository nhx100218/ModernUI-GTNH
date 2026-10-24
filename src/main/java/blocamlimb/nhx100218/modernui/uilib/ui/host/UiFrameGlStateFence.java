package blocamlimb.nhx100218.modernui.uilib.ui.host;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * UI 帧入口的 GL 状态围栏：保存进入时的真实 GL 状态，帧体执行后（含异常路径）逐项恢复。
 *
 * <p>三个宿主入口共用同一套语义：HUD 帧（{@code client.UiHudRenderListener}）、屏幕帧
 * （{@code ui.screen.McScreenBridge}）与 headless 帧（{@code internal.devtools.headless.HeadlessSession}，
 * 围栏起点在它自己的 {@code GlOffscreenSurface.beginFrame} 之后——surface 的 FBO/viewport/clear 属该 surface
 * 的自身契约，且 {@code readPixels()} 依赖"当前绑定的仍是 surface FBO"）。
 * 实例、快照和查询缓冲均跨帧复用（HUD 与屏幕各持静态实例，headless 持有会话级实例）。</p>
 *
 * <p>本围栏**不使用 attrib 属性组栈**（状态靠手工清单快照 + 显式恢复）：attrib 栈在 core profile 下的
 * 可用性只影响末尾的 {@code GlAttribDepth.popExcess} 兜底——它读不到深度时降级 no-op，首次留痕由
 * {@code util.GlAttribDepth} 承担（warn-once，见该类 javadoc）。GL 自净审查 N13 的处置口径以此为准：
 * 内层组件的 attrib 帧（{@code UiRenderTarget} / 字体守卫 / {@code GlStateScope} 等）在 core profile 下的
 * 可用性<b>未离线判定</b>——历史记录（{@code docs/反馈层/errors/ERROR-2026-07-14-host-image-core-profile-texture-matrix.md}）
 * 里的失败形态（深度查询返回 0、normalize 与双重围栏造成的伪下溢）属已随 323d25da 删除的旧机制，不能外推；
 * 可外推的只是其环境前提（core profile 下"0 不是合法栈深度"）。且没有"push/pop 真不可用"的离线证据——
 * 历史文档判的是<b>深度查询</b>语义。当前各站点也没有查询式能力探测（{@code GL_ATTRIB_STACK_DEPTH} 全仓
 * 0 命中），故本轮不写投机性降级：静默降级会把"整帧中止"换成"状态静默漂移"，而多数站点的 attrib 帧就是
 * 其唯一恢复机制；历史预防措施同样要求逐子能力探测 + 未知 GL error 必须 fail-closed。
 * 真机探测清单与候选设计见审查报告 §三 N13。</p>
 *
 * <p>快照覆盖：enable 位、blend 因子、颜色、scissor box、stencil 全量、colorMask/depthMask、
 * viewport、深度函数与 CLEAR 深度、active texture、<b>unit0 与入口 active unit 两个单元</b>的
 * TEXTURE_2D 绑定与 enable 位、program、VAO、array/element buffer，以及 FBO 入口可用（OpenGL30 档位
 * 或 {@code ARB_framebuffer_object}，见 LwjglGlAccess 的能力判据）时的 framebuffer 绑定。其余纹理单元不在快照内（捕获与恢复都只走这两个单元，见 captureOptionalBindings/
 * restoreOptionalBindings）——帧中途切到第三个单元并在那里绑定/改 enable 位不会被本围栏还原。</p>
 *
 * <p>不覆盖的状态与理由：纹理环境（texEnv）与混合方程不在快照内，由帧内的离屏层
 * （{@code UiRenderTarget} 进层时的全量 attrib 帧）与各组件自恢复；client 顶点数组状态亦不在此围栏
 * 职责内。矩阵是<b>按内容</b>捕获与写回的（{@code readFloats} + {@code glLoadMatrix}），不压栈——
 * PROJECTION 栈规格下限只有 2 层，围栏再占一层会把帧内文本路径推爆（headless 实测 GL_STACK_OVERFLOW）。
 * 改动本类的快照集合前请先读
 * {@code docs/历史报告/审查/2026-09-19-GL使用自净审查.md}。</p>
 */
public final class UiFrameGlStateFence {

    /** 可注入的最小 GL 状态访问面。 */
    interface GlAccess {
        void beginCapture();
        void beginRestore();
        boolean supportsActiveTexture();
        boolean supportsBuffers();
        boolean supportsProgram();
        boolean supportsVertexArray();
        boolean supportsFramebuffer();
        boolean isEnabled(int capability);
        int getInteger(int name);
        void readIntegers(int name, int[] target);
        void readFloats(int name, float[] target);
        void readBooleans(int name, boolean[] target);
        void setEnabled(int capability, boolean enabled);
        void matrixMode(int mode);
        void loadMatrix(float[] values);
        void activeTexture(int unit);
        void bindTexture2d(int texture);
        void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha);
        void color(float red, float green, float blue, float alpha);
        void scissor(int x, int y, int width, int height);
        void stencilFunc(int function, int reference, int valueMask);
        void stencilMask(int writeMask);
        void stencilOp(int fail, int depthFail, int depthPass);
        void colorMask(boolean red, boolean green, boolean blue, boolean alpha);
        void depthMask(boolean enabled);
        void depthFunc(int function);
        void clearDepth(float depth);
        void bindFramebuffer(int target, int framebuffer);
        void viewport(int x, int y, int width, int height);
        void useProgram(int program);
        void bindVertexArray(int vertexArray);
        void bindBuffer(int target, int buffer);
    }

    private final GlAccess gl;
    private final Snapshot snapshot = new Snapshot();
    private boolean active;

    /** 创建生产 LWJGL 状态围栏。 */
    public UiFrameGlStateFence() {
        this(new LwjglGlAccess());
    }

    /** 创建使用指定状态访问面的围栏。 */
    UiFrameGlStateFence(GlAccess gl) {
        if (gl == null) throw new IllegalArgumentException("gl");
        this.gl = gl;
    }

    /**
     * 在单次 capture/restore 边界内执行完整 UI 帧（HUD 或屏幕）。
     * restore 失败优先抛出，业务失败作为 suppressed 保留。
     */
    public void run(Runnable frame) {
        if (frame == null) throw new IllegalArgumentException("frame");
        if (active) throw new IllegalStateException("UI 帧 GL 状态围栏不允许重入（HUD 与屏幕各自持有实例）");
        active = true;
        try {
            capture();
            Throwable frameFailure = null;
            try {
                frame.run();
            } catch (RuntimeException failure) {
                frameFailure = failure;
            } catch (Error failure) {
                frameFailure = failure;
            }

            Throwable restoreFailure = null;
            try {
                restore();
            } catch (RuntimeException failure) {
                restoreFailure = failure;
            } catch (Error failure) {
                restoreFailure = failure;
            }
            if (restoreFailure != null) {
                restoreFailure = appendFailure(restoreFailure, frameFailure);
                throwUnchecked(restoreFailure);
            }
            if (frameFailure != null) throwUnchecked(frameFailure);
        } finally {
            active = false;
        }
    }

    /** 捕获真实入口状态；矩阵按内容读回不压栈，并打一次环境指纹（N13/R11 排错入口）。 */
    private void capture() {
        gl.beginCapture();
        // N13/R11 插桩：每进程只打一次 GL 环境指纹（版本/renderer/能力档位/attrib 深度可读性）。
        // 真机报状态污染时，第一眼就能确认是不是 Angelica/Core Profile 那一档。
        blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.logEnvironmentOnce("UiFrameGlStateFence");
        // 帧序号：让插桩日志里的 frame=N 能与本帧关联（N13/R11 排查用）。
        blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.beginFrame();
        // 帧起点深度：帧末按量弹回，避免第三方（如 FFP 变体编译）在帧内多压的 attrib 帧跨帧累积。
        // 两个宿主入口（HUD / 屏幕）因此同享同一套兜底；屏幕入口另有一层帧级兜底作双保险（R19）。
        snapshot.attribDepth = blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.current();
        snapshot.matrixMode = gl.getInteger(GL11.GL_MATRIX_MODE);
        snapshot.depthTest = gl.isEnabled(GL11.GL_DEPTH_TEST);
        snapshot.cullFace = gl.isEnabled(GL11.GL_CULL_FACE);
        snapshot.alphaTest = gl.isEnabled(GL11.GL_ALPHA_TEST);
        snapshot.lighting = gl.isEnabled(GL11.GL_LIGHTING);
        snapshot.blend = gl.isEnabled(GL11.GL_BLEND);
        snapshot.scissorTest = gl.isEnabled(GL11.GL_SCISSOR_TEST);
        snapshot.stencilTest = gl.isEnabled(GL11.GL_STENCIL_TEST);
        snapshot.texture2d = gl.isEnabled(GL11.GL_TEXTURE_2D);
        snapshot.blendSrcRgb = gl.getInteger(GL14.GL_BLEND_SRC_RGB);
        snapshot.blendDstRgb = gl.getInteger(GL14.GL_BLEND_DST_RGB);
        snapshot.blendSrcAlpha = gl.getInteger(GL14.GL_BLEND_SRC_ALPHA);
        snapshot.blendDstAlpha = gl.getInteger(GL14.GL_BLEND_DST_ALPHA);
        gl.readFloats(GL11.GL_CURRENT_COLOR, snapshot.color);
        gl.readIntegers(GL11.GL_SCISSOR_BOX, snapshot.scissor);
        snapshot.stencilFunction = gl.getInteger(GL11.GL_STENCIL_FUNC);
        snapshot.stencilReference = gl.getInteger(GL11.GL_STENCIL_REF);
        snapshot.stencilValueMask = gl.getInteger(GL11.GL_STENCIL_VALUE_MASK);
        snapshot.stencilWriteMask = gl.getInteger(GL11.GL_STENCIL_WRITEMASK);
        snapshot.stencilFail = gl.getInteger(GL11.GL_STENCIL_FAIL);
        snapshot.stencilDepthFail = gl.getInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
        snapshot.stencilDepthPass = gl.getInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);
        gl.readBooleans(GL11.GL_COLOR_WRITEMASK, snapshot.colorMask);
        snapshot.depthMask = gl.getInteger(GL11.GL_DEPTH_WRITEMASK) != GL11.GL_FALSE;
        gl.readIntegers(GL11.GL_VIEWPORT, snapshot.viewport);
        // 深度函数与 CLEAR 深度：帧内的离屏层会写它们（MinecraftHostImageRenderer.prepareHostImageState
        // 就硬置 depthFunc(LEQUAL)），UiHostRenderSupport 的 deferred 回放还会硬置 depthFunc/clearDepth
        // （该路径当前库内零调用，接线后立即适用）。离屏层的 attrib 帧虽能兜住一部分，但宿主
        // （实体/世界渲染）依赖的是真值，围栏必须自己恢复（N11）。
        snapshot.depthFunc = gl.getInteger(GL11.GL_DEPTH_FUNC);
        gl.readFloats(GL11.GL_DEPTH_CLEAR_VALUE, snapshot.depthClearValue);

        snapshot.hasActiveTexture = gl.supportsActiveTexture();
        snapshot.hasProgram = gl.supportsProgram();
        snapshot.hasVertexArray = gl.supportsVertexArray();
        snapshot.hasBuffers = gl.supportsBuffers();
        snapshot.hasFramebuffer = gl.supportsFramebuffer();
        captureOptionalBindings();
        captureMatrices();
    }

    /** 能力存在时捕获 texture0、入口 active unit 与现代对象 binding。 */
    private void captureOptionalBindings() {
        if (snapshot.hasActiveTexture) {
            snapshot.activeTexture = gl.getInteger(GL13.GL_ACTIVE_TEXTURE);
            snapshot.activeTexture2d = snapshot.texture2d;
            snapshot.activeTextureBinding = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                if (snapshot.activeTexture != GL13.GL_TEXTURE0) gl.activeTexture(GL13.GL_TEXTURE0);
                snapshot.texture0Enabled = gl.isEnabled(GL11.GL_TEXTURE_2D);
                snapshot.texture0Binding = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
            } finally {
                gl.activeTexture(snapshot.activeTexture);
            }
        }
        // FBO 绑定：离屏层进出会改写，宿主的 draw/read framebuffer 必须各自回到进入时的值（N11）。
        // 分开捕获的理由：GL3.0 起 draw 与 read 可分离绑定，GL_FRAMEBUFFER_BINDING 只等价于
        // GL_DRAW_FRAMEBUFFER_BINDING（同值 0x8CA6），用 glBindFramebuffer(GL_FRAMEBUFFER, …) 恢复会
        // 连带把 read 覆盖成 draw 值。口径与 UiRenderTarget/UiMainLayerSnapshotService 一致。
        // 查询只在 OpenGL30 档位存在时发出，避免在无 FBO 的 context 上留 GL error。
        if (snapshot.hasFramebuffer) {
            snapshot.drawFramebufferBinding = gl.getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            snapshot.readFramebufferBinding = gl.getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        }
        if (snapshot.hasProgram) snapshot.program = gl.getInteger(GL20.GL_CURRENT_PROGRAM);
        if (snapshot.hasVertexArray) snapshot.vertexArray = gl.getInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        if (snapshot.hasBuffers) {
            snapshot.arrayBuffer = gl.getInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            snapshot.elementBuffer = gl.getInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        }
    }

    /**
     * 捕获 PROJECTION / MODELVIEW 的矩阵<b>内容</b>（不压栈）。
     *
     * <p>为什么不用 {@code glPushMatrix}：PROJECTION 栈的规格下限只有 2 层，而帧内
     * {@code UiHostRenderSupport.beginMainUiFrame} 与字体围栏（{@code FontRenderStateGuard}）各自还要压一层。
     * 围栏再占一层会把文本路径推爆——实测 headless 文本页报 GL 错误码 1283（GL_STACK_OVERFLOW），
     * 而同一构型下只有本围栏的额外压栈是新变量。注意规格下限（PROJECTION 2 层）不是实测值：
     * 本机驱动上限为 4 层，重构前帧内峰值已 4/4、重构后 3/4（余量 1 层，见审查报告 §六之三）。
     * 改为读回真实矩阵 + 帧末 {@code glLoadMatrix} 写回后，既不占任何栈深度，恢复的也是进入时的真实值
     * （判据 1 优于栈快照）。</p>
     *
     * <p>栈失衡不在本围栏职责内：帧体自身以 try-with-resources 配对（{@code MainFrameScope}）；
     * 第三方泄漏的层级既不是本围栏压入的，也不由本围栏代偿。代价要说清：内容写回作用于"当前栈顶"，
     * 帧内若多压一层未弹，写回的是那一层的顶（入口内容不落回）；若多弹一层，旧压栈方案会在帧末暴露
     * {@code GL_STACK_UNDERFLOW}，本方案则静默少一层——两种失衡都属帧体自身契约
     * （{@code GlAttribDepth} 只弹 attrib 栈，不代偿矩阵栈）。</p>
     */
    private void captureMatrices() {
        gl.readFloats(GL11.GL_PROJECTION_MATRIX, snapshot.projectionMatrix);
        gl.readFloats(GL11.GL_MODELVIEW_MATRIX, snapshot.modelviewMatrix);
    }

    /** 恢复所有显式状态；矩阵内容、active texture 与 matrix mode 在末尾恢复。 */
    private void restore() {
        gl.beginRestore();
        Throwable failure = null;
        try {
            restoreOptionalBindings();
        } catch (RuntimeException restoreFailure) {
            failure = restoreFailure;
        } catch (Error restoreFailure) {
            failure = restoreFailure;
        }
        try {
            restoreFixedState();
        } catch (RuntimeException restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        } catch (Error restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        }
        try {
            gl.matrixMode(GL11.GL_MODELVIEW);
            gl.loadMatrix(snapshot.modelviewMatrix);
        } catch (RuntimeException restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        } catch (Error restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        }
        try {
            gl.matrixMode(GL11.GL_PROJECTION);
            gl.loadMatrix(snapshot.projectionMatrix);
        } catch (RuntimeException restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        } catch (Error restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        }
        if (snapshot.hasActiveTexture) {
            try {
                gl.activeTexture(snapshot.activeTexture);
            } catch (RuntimeException restoreFailure) {
                failure = appendFailure(failure, restoreFailure);
            } catch (Error restoreFailure) {
                failure = appendFailure(failure, restoreFailure);
            }
        }
        try {
            gl.matrixMode(snapshot.matrixMode);
        } catch (RuntimeException restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        } catch (Error restoreFailure) {
            failure = appendFailure(failure, restoreFailure);
        }
        // attrib 栈深度兜底：即使上面的恢复有失败也要执行（popExcess 内部自降级、不重抛）。
        blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.popExcess(snapshot.attribDepth);
        if (failure != null) throwUnchecked(failure);
    }

    /** 恢复能力感知的 framebuffer、texture、program、VAO 与 buffer binding。 */
    private void restoreOptionalBindings() {
        if (snapshot.hasFramebuffer) {
            gl.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshot.drawFramebufferBinding);
            gl.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, snapshot.readFramebufferBinding);
        }
        if (snapshot.hasProgram) gl.useProgram(snapshot.program);
        if (snapshot.hasActiveTexture) {
            gl.activeTexture(GL13.GL_TEXTURE0);
            gl.bindTexture2d(snapshot.texture0Binding);
            gl.setEnabled(GL11.GL_TEXTURE_2D, snapshot.texture0Enabled);
            if (snapshot.activeTexture != GL13.GL_TEXTURE0) {
                gl.activeTexture(snapshot.activeTexture);
                gl.bindTexture2d(snapshot.activeTextureBinding);
                gl.setEnabled(GL11.GL_TEXTURE_2D, snapshot.activeTexture2d);
            }
        } else {
            gl.setEnabled(GL11.GL_TEXTURE_2D, snapshot.texture2d);
        }
        if (snapshot.hasVertexArray) gl.bindVertexArray(snapshot.vertexArray);
        if (snapshot.hasBuffers) {
            gl.bindBuffer(GL15.GL_ARRAY_BUFFER, snapshot.arrayBuffer);
            gl.bindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, snapshot.elementBuffer);
        }
    }

    /** 恢复固定管线、裁切、模板、写掩码、颜色和视口状态。 */
    private void restoreFixedState() {
        gl.blendFuncSeparate(snapshot.blendSrcRgb, snapshot.blendDstRgb,
                snapshot.blendSrcAlpha, snapshot.blendDstAlpha);
        gl.color(snapshot.color[0], snapshot.color[1], snapshot.color[2], snapshot.color[3]);
        gl.scissor(snapshot.scissor[0], snapshot.scissor[1], snapshot.scissor[2], snapshot.scissor[3]);
        gl.stencilFunc(snapshot.stencilFunction, snapshot.stencilReference, snapshot.stencilValueMask);
        gl.stencilMask(snapshot.stencilWriteMask);
        gl.stencilOp(snapshot.stencilFail, snapshot.stencilDepthFail, snapshot.stencilDepthPass);
        gl.colorMask(snapshot.colorMask[0], snapshot.colorMask[1], snapshot.colorMask[2], snapshot.colorMask[3]);
        gl.depthMask(snapshot.depthMask);
        gl.depthFunc(snapshot.depthFunc);
        gl.clearDepth(snapshot.depthClearValue[0]);
        gl.viewport(snapshot.viewport[0], snapshot.viewport[1], snapshot.viewport[2], snapshot.viewport[3]);
        gl.setEnabled(GL11.GL_DEPTH_TEST, snapshot.depthTest);
        gl.setEnabled(GL11.GL_CULL_FACE, snapshot.cullFace);
        gl.setEnabled(GL11.GL_ALPHA_TEST, snapshot.alphaTest);
        gl.setEnabled(GL11.GL_LIGHTING, snapshot.lighting);
        gl.setEnabled(GL11.GL_BLEND, snapshot.blend);
        gl.setEnabled(GL11.GL_SCISSOR_TEST, snapshot.scissorTest);
        gl.setEnabled(GL11.GL_STENCIL_TEST, snapshot.stencilTest);
    }

    /** 合并清理失败，保留第一次失败作为主异常；同一实例不得自挂 suppressed。 */
    private static Throwable appendFailure(Throwable primary, Throwable next) {
        if (primary == null) return next;
        if (next == null) return primary;
        if (primary == next) return primary;
        primary.addSuppressed(next);
        return primary;
    }

    /** 仅重抛围栏允许捕获的 unchecked 失败。 */
    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        throw (Error) failure;
    }

    /** 跨帧复用的 UI 帧 GL 状态快照。 */
    private static final class Snapshot {
        private boolean depthTest, cullFace, alphaTest, lighting, blend, scissorTest, stencilTest, texture2d;
        private boolean depthMask;
        private boolean hasActiveTexture, hasProgram, hasVertexArray, hasBuffers, hasFramebuffer;
        private boolean activeTexture2d, texture0Enabled;
        private int matrixMode;
        /** capture 时的 attrib 栈深度：帧末按量弹出第三方在帧内泄漏的层级（R19）。 */
        private int attribDepth = -1;
        private int blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha;
        private int stencilFunction, stencilReference, stencilValueMask, stencilWriteMask;
        private int stencilFail, stencilDepthFail, stencilDepthPass;
        private int activeTexture, activeTextureBinding, texture0Binding;
        private int program, vertexArray, arrayBuffer, elementBuffer;
        /** 帧内离屏层会改写 depthFunc 与 CLEAR 深度，宿主可能是任一非默认值（N11）。 */
        private int depthFunc;
        /** draw/read 分离绑定：必须分别捕获与恢复（N11）。 */
        private int drawFramebufferBinding, readFramebufferBinding;
        private final float[] depthClearValue = new float[1];
        private final float[] color = new float[4];
        /** 矩阵内容快照：不压栈，帧末按值写回（PROJECTION 栈规格下限只有 2 层）。 */
        private final float[] projectionMatrix = new float[16];
        private final float[] modelviewMatrix = new float[16];
        private final int[] scissor = new int[4];
        private final boolean[] colorMask = new boolean[4];
        private final int[] viewport = new int[4];
    }

    /** 生产 LWJGL2 状态访问器；可选 API 只在当前 context 对应能力存在时调用。 */
    private static final class LwjglGlAccess implements GlAccess {
        // LWJGL2 的 glGetInteger/glGetFloat/glGetBoolean(int, XxxBuffer) 重载经 BufferChecks
        // 对缓冲区 remaining 恒定校验 >= 16，容量 4 的查询缓冲会在任何 capture 时崩溃（issue #70）；
        // 查询结果复制仍只取 target.length 个元素，不依赖缓冲满容量。
        private final IntBuffer integers = ByteBuffer.allocateDirect(16 * Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        private final FloatBuffer floats = ByteBuffer.allocateDirect(16 * Float.BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        private final ByteBuffer booleans = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());

        // 刻意不做 GL 查询式能力探测：core profile 下对已移除的栈参数查询会留下 GL 错误码，
        // 而本仓有「入口 GL error 必须为空」的契约（历史见 ERROR-2026-07-14 记录：内层探测清掉外层错误）。
        // 本类没有需要留痕的降级分支（attrib 栈不用、FBO/纹理查询都有档位门），
        // 第三方 attrib 泄漏与「深度不可读时降级 no-op」的 warn-once 由 util.GlAttribDepth 承担。
        @Override public void beginCapture() { }
        @Override public void beginRestore() { }
        // 能力判据 = 档位或提供同一批无后缀入口的扩展：GL2.1 + ARB_* 构型下入口可用而档位为 false，
        // 只看版本位会静默跳过整类恢复（P6 对合成器门控的定论同源，独立复核 F7-3）。
        @Override public boolean supportsActiveTexture() { return capabilities().OpenGL13; }
        @Override public boolean supportsBuffers() {
            return capabilities().OpenGL15 || capabilities().GL_ARB_vertex_buffer_object;
        }
        @Override public boolean supportsProgram() {
            return capabilities().OpenGL20 || capabilities().GL_ARB_shader_objects;
        }
        @Override public boolean supportsVertexArray() {
            return capabilities().OpenGL30 || capabilities().GL_ARB_vertex_array_object;
        }
        @Override public boolean supportsFramebuffer() {
            return capabilities().OpenGL30 || capabilities().GL_ARB_framebuffer_object;
        }
        @Override public boolean isEnabled(int capability) { return GL11.glIsEnabled(capability); }
        @Override public int getInteger(int name) { return GL11.glGetInteger(name); }
        @Override public void readIntegers(int name, int[] target) {
            integers.clear();
            GL11.glGetInteger(name, integers);
            for (int index = 0; index < target.length; index++) target[index] = integers.get(index);
        }
        @Override public void readFloats(int name, float[] target) {
            floats.clear();
            GL11.glGetFloat(name, floats);
            for (int index = 0; index < target.length; index++) target[index] = floats.get(index);
        }
        @Override public void readBooleans(int name, boolean[] target) {
            booleans.clear();
            GL11.glGetBoolean(name, booleans);
            for (int index = 0; index < target.length; index++) target[index] = booleans.get(index) != GL11.GL_FALSE;
        }
        @Override public void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
        }
        @Override public void matrixMode(int mode) { GL11.glMatrixMode(mode); }
        @Override public void loadMatrix(float[] values) {
            floats.clear();
            floats.put(values, 0, 16);
            floats.flip();
            GL11.glLoadMatrix(floats);
        }
        @Override public void activeTexture(int unit) { GL13.glActiveTexture(unit); }
        @Override public void bindTexture2d(int texture) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); }
        @Override public void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        }
        @Override public void color(float red, float green, float blue, float alpha) {
            GL11.glColor4f(red, green, blue, alpha);
        }
        @Override public void scissor(int x, int y, int width, int height) { GL11.glScissor(x, y, width, height); }
        @Override public void stencilFunc(int function, int reference, int valueMask) {
            GL11.glStencilFunc(function, reference, valueMask);
        }
        @Override public void stencilMask(int writeMask) { GL11.glStencilMask(writeMask); }
        @Override public void stencilOp(int fail, int depthFail, int depthPass) {
            GL11.glStencilOp(fail, depthFail, depthPass);
        }
        @Override public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
            GL11.glColorMask(red, green, blue, alpha);
        }
        @Override public void depthMask(boolean enabled) { GL11.glDepthMask(enabled); }
        @Override public void depthFunc(int function) { GL11.glDepthFunc(function); }
        @Override public void clearDepth(float depth) { GL11.glClearDepth(depth); }
        @Override public void bindFramebuffer(int target, int framebuffer) {
            GL30.glBindFramebuffer(target, framebuffer);
        }
        @Override public void viewport(int x, int y, int width, int height) { GL11.glViewport(x, y, width, height); }
        @Override public void useProgram(int program) { GL20.glUseProgram(program); }
        @Override public void bindVertexArray(int vertexArray) { GL30.glBindVertexArray(vertexArray); }
        @Override public void bindBuffer(int target, int buffer) { GL15.glBindBuffer(target, buffer); }

        /** 读取当前线程绑定 context 的能力。 */
        private static ContextCapabilities capabilities() {
            return GLContext.getCapabilities();
        }
    }
}
