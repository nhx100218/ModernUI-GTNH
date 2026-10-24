package blocamlimb.nhx100218.modernui.uilib.font.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;


/**
 * 字体渲染状态保护器。
 *
 * <p>TEXTURE_2D enable 是 per-texture-unit 状态，{@code glPushAttrib}/{@code glPopAttrib} 只保存/恢复
 * push/pop 时刻 active unit 的 per-unit 状态。flush 会在中途 {@code glActiveTexture(GL_TEXTURE0)} 并
 * 在 push 时的 active unit 上启用 TEXTURE_2D，导致 attrib pop 与单元错位。因此 push 时显式记录 unit0
 * 与 push 时 active unit 的 TEXTURE_2D enable，pop 时在各自 unit 上显式恢复，再切回 push 时 active unit。</p>
 */
public class FontRenderStateGuard implements FontRenderStateExecutor {

    /** 可注入的最小 GL 状态访问面，供同包测试在不初始化 LWJGL 的情况下验证状态守恒。 */
    interface GlAccess {

        void pushAttrib(int mask);

        void pushClientAttrib(int mask);

        void popAttrib();

        void popClientAttrib();

        int getInteger(int name);

        void readIntegers(int name, int[] target);

        boolean isEnabled(int capability);

        void setEnabled(int capability, boolean enabled);

        void matrixMode(int mode);

        void pushMatrix();

        void popMatrix();

        void activeTexture(int unit);

        void bindTexture2d(int texture);

        void useProgram(int program);

        void bindVertexArray(int vertexArray);

        void bindBuffer(int target, int buffer);

        void viewport(int x, int y, int width, int height);
    }

    private static final class SavedState {

        private final boolean matrixStateSaved;
        private final int attribDepthBeforePush;
        private final int[] viewport = new int[4];
        private int activeTexture;
        private int currentProgram;
        private int currentMatrixMode;
        private int textureBinding2DOnTexture0;
        private int textureBinding2DOnActiveTexture;
        private boolean texture2DEnabledOnTexture0;
        private boolean texture2DEnabledOnActiveTexture;
        private int vertexArrayBinding;
        private int arrayBufferBinding;
        private int elementArrayBufferBinding;

        private SavedState(boolean matrixStateSaved) {
            this.matrixStateSaved = matrixStateSaved;
            this.attribDepthBeforePush = blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.current();
        }
    }

    private final GlAccess gl;
    private final int[] viewportScratch = new int[4];
    private final Deque<SavedState> savedStates = new ArrayDeque<SavedState>();

    /** 创建生产 LWJGL 状态保护器。 */
    public FontRenderStateGuard() {
        this(new LwjglGlAccess());
    }

    /** 创建使用指定状态访问面的保护器。 */
    FontRenderStateGuard(GlAccess gl) {
        if (gl == null) {
            throw new IllegalArgumentException("gl 不得为 null");
        }
        this.gl = gl;
    }

    /**
     * 保存当前 OpenGL 状态。
     */
    public void push() {
        push(true);
    }

    /**
     * 保存当前 OpenGL 状态。
     *
     * @param includeMatrixState 是否同时保存固定管线矩阵栈
     */
    public void push(boolean includeMatrixState) {
        SavedState state = new SavedState(includeMatrixState);
        boolean attribPushed = false;
        boolean clientAttribPushed = false;
        int matrixStacksPushed = 0;
        try {
            gl.pushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            attribPushed = true;
            gl.pushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
            clientAttribPushed = true;
            if (includeMatrixState) {
                state.currentMatrixMode = gl.getInteger(GL11.GL_MATRIX_MODE);
                pushMatrixStack(GL11.GL_MODELVIEW);
                matrixStacksPushed++;
                pushMatrixStack(GL11.GL_PROJECTION);
                matrixStacksPushed++;
                pushMatrixStack(GL11.GL_TEXTURE);
                matrixStacksPushed++;
            }

            state.activeTexture = gl.getInteger(GL13.GL_ACTIVE_TEXTURE);
            state.currentProgram = gl.getInteger(GL20.GL_CURRENT_PROGRAM);
            gl.activeTexture(GL13.GL_TEXTURE0);
            state.textureBinding2DOnTexture0 = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
            state.texture2DEnabledOnTexture0 = gl.isEnabled(GL11.GL_TEXTURE_2D);
            if (state.activeTexture != GL13.GL_TEXTURE0) {
                gl.activeTexture(state.activeTexture);
                state.textureBinding2DOnActiveTexture = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
                state.texture2DEnabledOnActiveTexture = gl.isEnabled(GL11.GL_TEXTURE_2D);
            } else {
                state.textureBinding2DOnActiveTexture = state.textureBinding2DOnTexture0;
                state.texture2DEnabledOnActiveTexture = state.texture2DEnabledOnTexture0;
            }
            gl.activeTexture(state.activeTexture);
            state.vertexArrayBinding = gl.getInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            state.arrayBufferBinding = gl.getInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            state.elementArrayBufferBinding = gl.getInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            gl.readIntegers(GL11.GL_VIEWPORT, viewportScratch);
            System.arraycopy(viewportScratch, 0, state.viewport, 0, state.viewport.length);
            savedStates.push(state);
        } catch (RuntimeException exception) {
            rollbackPush(matrixStacksPushed, clientAttribPushed, attribPushed, state, exception);
            throw exception;
        } catch (LinkageError error) {
            rollbackPush(matrixStacksPushed, clientAttribPushed, attribPushed, state, error);
            throw error;
        } catch (Error error) {
            rollbackPush(matrixStacksPushed, clientAttribPushed, attribPushed, state, error);
            throw error;
        }
    }

    /**
     * 恢复之前保存的 OpenGL 状态。
     */
    public void pop() {
        if (savedStates.isEmpty()) {
            throw new IllegalStateException("字体渲染状态恢复缺少对应的保存边界");
        }
        SavedState state = savedStates.pop();
        // 逐步恢复 + 失败累积：任一步抛异常都不能跳过后续（原实现 popAttrib 失败会让
        // program/纹理/VAO/buffer/viewport 全部不还原且 attrib 栈失衡，GL 自净审查 N15）。
        Throwable failure = null;
        if (state.matrixStateSaved) {
            // GL_TEXTURE 矩阵栈属于 active texture 单元。字体批次会切到 TEXTURE0，
            // 必须先回到 push 时的单元，否则 unit0 下溢而入口单元不断积累未弹出的矩阵。
            failure = recordFailure(failure, () -> gl.activeTexture(state.activeTexture));
            failure = recordFailure(failure, () -> popMatrixStack(GL11.GL_TEXTURE));
            failure = recordFailure(failure, () -> popMatrixStack(GL11.GL_PROJECTION));
            failure = recordFailure(failure, () -> popMatrixStack(GL11.GL_MODELVIEW));
            failure = recordFailure(failure, () -> gl.matrixMode(state.currentMatrixMode));
        }
        failure = recordFailure(failure, () -> gl.popClientAttrib());
        failure = recordFailure(failure, () -> gl.popAttrib());
        // 围堵第三方渲染路径（如 FFP 变体编译）在守卫区间内泄漏的 attrib 栈深度。
        final SavedState restored = state;
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.popExcess(restored.attribDepthBeforePush);
            }
        });

        failure = recordFailure(failure, () -> gl.useProgram(state.currentProgram));
        failure = recordFailure(failure, () -> gl.activeTexture(GL13.GL_TEXTURE0));
        failure = recordFailure(failure, () -> gl.bindTexture2d(state.textureBinding2DOnTexture0));
        failure = recordFailure(failure, () -> gl.setEnabled(GL11.GL_TEXTURE_2D, state.texture2DEnabledOnTexture0));
        if (state.activeTexture != GL13.GL_TEXTURE0) {
            failure = recordFailure(failure, () -> gl.activeTexture(state.activeTexture));
            failure = recordFailure(failure, () -> gl.bindTexture2d(state.textureBinding2DOnActiveTexture));
            failure = recordFailure(failure,
                    () -> gl.setEnabled(GL11.GL_TEXTURE_2D, state.texture2DEnabledOnActiveTexture));
        }
        failure = recordFailure(failure, () -> gl.activeTexture(state.activeTexture));
        failure = recordFailure(failure, () -> gl.bindVertexArray(state.vertexArrayBinding));
        failure = recordFailure(failure, () -> gl.bindBuffer(GL15.GL_ARRAY_BUFFER, state.arrayBufferBinding));
        failure = recordFailure(failure, () -> gl.bindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, state.elementArrayBufferBinding));

        failure = recordFailure(failure,
                () -> gl.viewport(state.viewport[0], state.viewport[1], state.viewport[2], state.viewport[3]));
        if (state.matrixStateSaved) {
            failure = recordFailure(failure, () -> gl.matrixMode(state.currentMatrixMode));
        }
        if (failure != null) {
            throwUnchecked(failure);
        }
    }

    /**
     * 在保护的 OpenGL 状态边界中执行任务。
     *
     * @param task 要执行的任务
     */
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        push(true);
        try {
            task.run();
        } finally {
            pop();
        }
    }

    /**
     * 在保护的 OpenGL 状态边界中执行任务，可跳过矩阵栈保存。
     *
     * @param task 要执行的任务
     * @param includeMatrixState 是否同时保存固定管线矩阵栈
     */
    public void run(Runnable task, boolean includeMatrixState) {
        Objects.requireNonNull(task, "task");
        push(includeMatrixState);
        try {
            task.run();
        } finally {
            pop();
        }
    }

    private void pushMatrixStack(int matrixMode) {
        gl.matrixMode(matrixMode);
        gl.pushMatrix();
    }

    /**
     * push 中途失败时回滚已压入的 attrib / client attrib / 矩阵栈与 active texture 单元。
     *
     * <p>失败优先级：致命 {@link Error} > 回滚失败 > 压入失败（压入失败作为 suppressed 保留）。
     * 回滚每一步都不跳过后续，理由同 {@link #pop()}。</p>
     */
    private void rollbackPush(int matrixStacksPushed, boolean clientAttribPushed, boolean attribPushed,
            SavedState state, Throwable pushFailure) {
        Throwable rollbackFailure = null;
        // 先回到 push 时的 active unit：GL_TEXTURE 矩阵栈属于 texture unit，而 push 在切到 TEXTURE0
        // 之后（:135 起）才可能失败，此时若先弹矩阵栈就会弹到 unit0（下溢）并让入口单元残留一层未弹帧。
        // 与本文件 pop() 的既有规则一致。state.activeTexture 只在读取成功后才非 0（GL_TEXTURE0 是 33984），
        // 0 表示尚未读到，此时不动单元。
        if (state.activeTexture != 0) {
            rollbackFailure = recordFailure(rollbackFailure, () -> gl.activeTexture(state.activeTexture));
        }
        // 逐步累积：任一步弹栈失败都不能跳过后续——否则矩阵栈会残留多层帧，
        // 与 UiFrameGlStateFence.restore 的逐项累积写法对齐（独立复核 C 项）。
        if (matrixStacksPushed >= 3) {
            rollbackFailure = recordFailure(rollbackFailure, () -> popMatrixStack(GL11.GL_TEXTURE));
        }
        if (matrixStacksPushed >= 2) {
            rollbackFailure = recordFailure(rollbackFailure, () -> popMatrixStack(GL11.GL_PROJECTION));
        }
        if (matrixStacksPushed >= 1) {
            final int matrixModeToRestore = state.currentMatrixMode;
            rollbackFailure = recordFailure(rollbackFailure, () -> popMatrixStack(GL11.GL_MODELVIEW));
            rollbackFailure = recordFailure(rollbackFailure, () -> gl.matrixMode(matrixModeToRestore));
        }
        try {
            if (clientAttribPushed) {
                gl.popClientAttrib();
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendFailure(rollbackFailure, exception);
        } catch (Error error) {
            rollbackFailure = appendFailure(rollbackFailure, error);
        }
        try {
            if (attribPushed) {
                gl.popAttrib();
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendFailure(rollbackFailure, exception);
        } catch (Error error) {
            rollbackFailure = appendFailure(rollbackFailure, error);
        }
        if (rollbackFailure != null) {
            throwUnchecked(appendFailure(rollbackFailure, pushFailure));
        }
        throwUnchecked(pushFailure);
    }

    /**
     * 执行一步恢复并累积失败（不中断后续步骤）。
     *
     * <p>捕获面为全部 unchecked 失败（含 VM 级 {@link Error}）：恢复链中途抛出时直接向上传播会
     * 跳过后续步骤，导致 program/纹理/VAO/buffer/viewport 全部不还原（GL 自净审查 N15 的同类失败模式）。
     * 致命性由 {@link #appendFailure} 保留，不会被降级成 suppressed。</p>
     */
    private static Throwable recordFailure(Throwable failure, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException exception) {
            return appendFailure(failure, exception);
        } catch (Error error) {
            return appendFailure(failure, error);
        }
        return failure;
    }

    /**
     * 合并失败：第一个失败为主异常，后续失败挂 suppressed；致命 {@link Error} 升级为主异常。
     *
     * <p>同一实例不得自挂 suppressed（{@code addSuppressed(self)} 抛 {@code IllegalArgumentException}，
     * 会把真正的失败替换成参数异常）——push 失败与回滚失败完全可能是同一个实例。</p>
     */
    private static Throwable appendFailure(Throwable primary, Throwable additional) {
        if (primary == null) {
            return additional;
        }
        if (additional == null) {
            return primary;
        }
        if (primary == additional) {
            return primary;
        }
        if (isFatal(additional) && !isFatal(primary)) {
            additional.addSuppressed(primary);
            return additional;
        }
        primary.addSuppressed(additional);
        return primary;
    }

    /** {@link LinkageError} 属可恢复的类加载失败，其余 Error 视为致命。 */
    private static boolean isFatal(Throwable failure) {
        return failure instanceof Error && !(failure instanceof LinkageError);
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new RuntimeException(failure);
    }

    private void popMatrixStack(int matrixMode) {
        gl.matrixMode(matrixMode);
        gl.popMatrix();
    }

    /** 生产 LWJGL2 状态访问器；查询缓冲跨调用复用。 */
    private static final class LwjglGlAccess implements GlAccess {

        // LWJGL2 的 glGetInteger(int, IntBuffer) 重载经 BufferChecks 对缓冲区 remaining 恒定校验 >= 16，
        // 容量 4 的查询缓冲会在任何 push 时崩溃（与 issue #70 同源）；复制仍只取 target.length 个元素。
        private final java.nio.IntBuffer integers = java.nio.ByteBuffer.allocateDirect(16 * Integer.BYTES)
                .order(java.nio.ByteOrder.nativeOrder())
                .asIntBuffer();

        @Override
        public void pushAttrib(int mask) {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.pushAttrib(mask, "FontRenderStateGuard");
        }

        @Override
        public void pushClientAttrib(int mask) {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.pushClientAttrib(mask, "FontRenderStateGuard");
        }

        @Override
        public void popAttrib() {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.popAttrib("FontRenderStateGuard");
        }

        @Override
        public void popClientAttrib() {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.popClientAttrib("FontRenderStateGuard");
        }

        @Override
        public int getInteger(int name) {
            return GL11.glGetInteger(name);
        }

        @Override
        public void readIntegers(int name, int[] target) {
            integers.clear();
            GL11.glGetInteger(name, integers);
            for (int index = 0; index < target.length; index++) {
                target[index] = integers.get(index);
            }
        }

        @Override
        public boolean isEnabled(int capability) {
            return GL11.glIsEnabled(capability);
        }

        @Override
        public void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }

        @Override
        public void matrixMode(int mode) {
            GL11.glMatrixMode(mode);
        }

        @Override
        public void pushMatrix() {
            GL11.glPushMatrix();
        }

        @Override
        public void popMatrix() {
            GL11.glPopMatrix();
        }

        @Override
        public void activeTexture(int unit) {
            GL13.glActiveTexture(unit);
        }

        @Override
        public void bindTexture2d(int texture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }

        @Override
        public void useProgram(int program) {
            GL20.glUseProgram(program);
        }

        @Override
        public void bindVertexArray(int vertexArray) {
            GL30.glBindVertexArray(vertexArray);
        }

        @Override
        public void bindBuffer(int target, int buffer) {
            GL15.glBindBuffer(target, buffer);
        }

        @Override
        public void viewport(int x, int y, int width, int height) {
            GL11.glViewport(x, y, width, height);
        }
    }
}
