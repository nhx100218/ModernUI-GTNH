package blocamlimb.nhx100218.modernui.uilib.ui.image;

import java.util.Objects;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * 物品图标渲染的通用 GL 状态 scope：入口态快照与 {@code finally} 恢复。
 *
 * <p>真实 GL 实测（见 {@code docs/历史报告/审查/2026-09-19-GL使用自净审查.md} §六）：
 * {@code glPushAttrib(GL_ALL_ATTRIB_BITS)} 会恢复每单元的 TEXTURE_2D 绑定与 active texture，
 * 但**不恢复** program 绑定、VAO 绑定与矩阵栈内容。故本 scope 手动快照并恢复：
 * unit0 与入口 active unit 的 TEXTURE_2D 绑定、active texture、client-active texture、
 * program 与 VAO 绑定、矩阵模式；client 属性组（顶点数组、array/element buffer 绑定、
 * pixel store）经 {@code glPushClientAttrib} 覆盖。手动恢复 attrib 栈本已覆盖的项属保守冗余，
 * 目的是不依赖 attrib 栈在 core profile 下的可用性。</p>
 *
 * <p>矩阵栈内容不在此 scope 内保存：绘制核心自身配对 push/pop matrix，异常路径由其
 * {@code finally} 恢复；本 scope 只负责把矩阵模式恢复到入口值。scope 不支持嵌套进入。</p>
 */
public final class GlStateScope {

    /** 可注入的最小 GL 状态访问面，供同包测试在不初始化 LWJGL 的情况下验证状态守恒。 */
    interface GlAccess {

        void pushAttrib(int mask);

        void popAttrib();

        void pushClientAttrib(int mask);

        void popClientAttrib();

        int getInteger(int name);

        void activeTexture(int unit);

        void bindTexture2d(int texture);

        void clientActiveTexture(int unit);

        void useProgram(int program);

        void bindVertexArray(int vertexArray);

        void matrixMode(int mode);
    }

    /** 入口态快照：attrib 栈之外需要手动恢复的状态。 */
    private static final class SavedState {

        private int matrixMode;
        private int activeTexture;
        private int clientActiveTexture;
        private int textureBinding2DOnTexture0;
        private int textureBinding2DOnActiveTexture;
        /** program / VAO 绑定：attrib 栈不覆盖它们（真实 GL 实测，见 GL 自净审查 N21）；不可用时保留 -1。 */
        private int programBinding = -1;
        private int vertexArrayBinding = -1;
    }

    private final GlAccess gl;
    private final SavedState saved = new SavedState();
    private boolean entered;
    private int ambientDepthBeforeEnter = -1;

    /** 创建生产 LWJGL 状态 scope。 */
    public GlStateScope() {
        this(new LwjglGlAccess());
    }

    /** 创建使用指定状态访问面的 scope。 */
    GlStateScope(GlAccess gl) {
        if (gl == null) {
            throw new IllegalArgumentException("gl 不得为 null");
        }
        this.gl = gl;
    }

    /**
     * 在保护的 GL 状态边界中执行任务。
     *
     * <p>入口态在进入时快照，任务正常完成或抛出异常时都恢复；恢复失败作为主导异常抛出，
     * 任务异常以 suppressed 保留（不因 finally 语义被替换）。</p>
     *
     * <p>唯一的例外是「恢复失败为非致命、任务失败为致命 {@link Error}（非 {@link LinkageError}）」：
     * 此时按仓内统一口径把致命的任务失败升级为主异常，恢复失败转 suppressed——VM 级失败
     * 不应被降级为附注。</p>
     *
     * @param task 要执行的任务
     */
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        enter();
        Throwable taskFailure = null;
        try {
            task.run();
        } catch (RuntimeException exception) {
            taskFailure = exception;
        } catch (LinkageError error) {
            taskFailure = error;
        } catch (Error error) {
            taskFailure = error;
        }
        Throwable exitFailure = null;
        try {
            exit();
        } catch (RuntimeException exception) {
            exitFailure = exception;
        } catch (Error error) {
            // exit() 的恢复链会把致命 Error 升为主异常抛出（见 recordFailure/appendFailure），
            // 故这里必须一并接住：否则 run() 直接传播，任务异常被静默丢弃（与类 javadoc 相反）。
            exitFailure = error;
        }
        // 恢复失败作为主导异常，任务异常作为 suppressed 保留——原先 try/finally 会直接吞掉任务异常（N22）。
        // 合并统一走 appendFailure：同一实例不自挂 suppressed，且致命 Error 不会被降级。
        if (exitFailure != null) {
            rethrow(appendFailure(exitFailure, taskFailure));
        }
        if (taskFailure != null) {
            rethrow(taskFailure);
        }
    }

    private void enter() {
        if (entered) {
            throw new IllegalStateException("GL 状态 scope 不支持嵌套进入");
        }
        boolean attribPushed = false;
        boolean clientAttribPushed = false;
        ambientDepthBeforeEnter = blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.current();
        try {
            gl.pushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            attribPushed = true;
            gl.pushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT | GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
            clientAttribPushed = true;
            saved.matrixMode = gl.getInteger(GL11.GL_MATRIX_MODE);
            saved.activeTexture = gl.getInteger(GL13.GL_ACTIVE_TEXTURE);
            saved.clientActiveTexture = -1;
            // 前置复位：能力缺失时保留 -1，且不让上一次 run 的成功捕获串到本次（复审意见）。
            saved.programBinding = -1;
            saved.vertexArrayBinding = -1;
            try {
                saved.clientActiveTexture = gl.getInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
            } catch (RuntimeException ignored) {
                // core profile 后端可能不支持 GL_CLIENT_ACTIVE_TEXTURE 查询：
                // 保留 -1，exit 时跳过恢复。
            } catch (LinkageError ignored) {
                // 同上。
            }
            try {
                saved.programBinding = gl.getInteger(GL20.GL_CURRENT_PROGRAM);
            } catch (RuntimeException ignored) {
                // 后端无 GL2.0 program 能力：保留 -1，exit 时跳过恢复。
            } catch (LinkageError ignored) {
                // 同上。
            }
            try {
                saved.vertexArrayBinding = gl.getInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            } catch (RuntimeException ignored) {
                // 后端无 GL3.0 VAO 能力：保留 -1，exit 时跳过恢复。
            } catch (LinkageError ignored) {
                // 同上。
            }
            gl.activeTexture(GL13.GL_TEXTURE0);
            saved.textureBinding2DOnTexture0 = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
            if (saved.activeTexture != GL13.GL_TEXTURE0) {
                gl.activeTexture(saved.activeTexture);
                saved.textureBinding2DOnActiveTexture = gl.getInteger(GL11.GL_TEXTURE_BINDING_2D);
            } else {
                saved.textureBinding2DOnActiveTexture = saved.textureBinding2DOnTexture0;
            }
            gl.activeTexture(saved.activeTexture);
            entered = true;
        } catch (RuntimeException exception) {
            rollbackEnter(clientAttribPushed, attribPushed);
            throw exception;
        } catch (LinkageError error) {
            rollbackEnter(clientAttribPushed, attribPushed);
            throw error;
        } catch (Error error) {
            rollbackEnter(clientAttribPushed, attribPushed);
            throw error;
        }
    }

    /** enter 中途失败时回滚已压入的 attrib/client attrib 栈，避免状态泄漏。 */
    private void rollbackEnter(boolean clientAttribPushed, boolean attribPushed) {
        if (clientAttribPushed) {
            try {
                gl.popClientAttrib();
            } catch (RuntimeException ignored) {
                // 回滚失败保留原始异常，不再抛出。
            } catch (LinkageError ignored) {
                // 同上。
            }
        }
        if (attribPushed) {
            try {
                gl.popAttrib();
            } catch (RuntimeException ignored) {
                // 同上。
            } catch (LinkageError ignored) {
                // 同上。
            }
        }
    }

    private void exit() {
        if (!entered) {
            throw new IllegalStateException("GL 状态恢复缺少对应的进入边界");
        }
        Throwable failure = null;
        // 栈平衡优先：popClientAttrib / popAttrib 必须先于其它恢复步骤执行；
        // 后续步骤失败只记录，绝不阻断弹出（防止 attrib 栈泄漏累积）。
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.popClientAttrib();
            }
        });
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.popAttrib();
            }
        });
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.activeTexture(GL13.GL_TEXTURE0);
            }
        });
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.bindTexture2d(saved.textureBinding2DOnTexture0);
            }
        });
        if (saved.activeTexture != GL13.GL_TEXTURE0) {
            failure = recordFailure(failure, new Runnable() {
                @Override
                public void run() {
                    gl.activeTexture(saved.activeTexture);
                }
            });
            failure = recordFailure(failure, new Runnable() {
                @Override
                public void run() {
                    gl.bindTexture2d(saved.textureBinding2DOnActiveTexture);
                }
            });
        }
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.activeTexture(saved.activeTexture);
            }
        });
        if (saved.clientActiveTexture >= 0) {
            failure = recordFailure(failure, new Runnable() {
                @Override
                public void run() {
                    gl.clientActiveTexture(saved.clientActiveTexture);
                }
            });
        }
        if (saved.programBinding >= 0) {
            failure = recordFailure(failure, new Runnable() {
                @Override
                public void run() {
                    gl.useProgram(saved.programBinding);
                }
            });
        }
        if (saved.vertexArrayBinding >= 0) {
            failure = recordFailure(failure, new Runnable() {
                @Override
                public void run() {
                    gl.bindVertexArray(saved.vertexArrayBinding);
                }
            });
        }
        failure = recordFailure(failure, new Runnable() {
            @Override
            public void run() {
                gl.matrixMode(saved.matrixMode);
            }
        });
        // 围堵第三方渲染路径（如 FFP 变体编译）泄漏的 attrib 栈深度。
        blocamlimb.nhx100218.modernui.uilib.util.GlAttribDepth.popExcess(ambientDepthBeforeEnter);
        ambientDepthBeforeEnter = -1;
        entered = false;
        rethrow(failure);
    }

    /**
     * 执行一步恢复并记录失败（不中断后续步骤）。
     *
     * <p>捕获面为全部 unchecked 失败（含 VM 级 {@link Error}），理由同
     * {@code UiBackdropFilterRenderer#restoreStep}：恢复链不得因中途抛出而跳过后续步骤。
     * 致命性由 {@link #appendFailure} 保留。</p>
     */
    private Throwable recordFailure(Throwable failure, Runnable step) {
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
     * 合并失败：第一个失败为主异常，后续失败挂 suppressed。
     *
     * <p>两处防御：同一实例不得自挂 suppressed（{@code addSuppressed(self)} 会抛
     * {@code IllegalArgumentException}，把恢复失败替换成参数异常）；致命 {@link Error}
     * 不得被降级——升级为主异常、原主异常转 suppressed。口径与
     * {@code UiRenderTarget}/{@code UiHostRenderSupport}/{@code MinecraftHostImageRenderer} 一致。</p>
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

    private static void rethrow(Throwable failure) {
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new RuntimeException(failure);
    }

    /** 生产 LWJGL2 状态访问器。 */
    private static final class LwjglGlAccess implements GlAccess {

        @Override
        public void pushAttrib(int mask) {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.pushAttrib(mask, "GlStateScope");
        }

        @Override
        public void popAttrib() {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.popAttrib("GlStateScope");
        }

        @Override
        public void pushClientAttrib(int mask) {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.pushClientAttrib(mask, "GlStateScope");
        }

        @Override
        public void popClientAttrib() {
            blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics.popClientAttrib("GlStateScope");
        }

        @Override
        public int getInteger(int name) {
            // 能力感知：GL2.0/3.0 缺席时不发查询。core profile 下这类 pname 可能只置 GL 错误码并返回陈旧值，
            // 上层会误判「捕获成功」，退出时反而调用不可用的绑定 API，把成功渲染变成失败（复审意见）。
            if (name == GL20.GL_CURRENT_PROGRAM && !capabilities().OpenGL20) {
                throw new IllegalStateException("后端不支持 GL2.0 program 查询");
            }
            if (name == GL30.GL_VERTEX_ARRAY_BINDING && !capabilities().OpenGL30) {
                throw new IllegalStateException("后端不支持 GL3.0 VAO 查询");
            }
            return GL11.glGetInteger(name);
        }

        /** 读取当前线程绑定 context 的能力。 */
        private static ContextCapabilities capabilities() {
            return GLContext.getCapabilities();
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
        public void clientActiveTexture(int unit) {
            GL13.glClientActiveTexture(unit);
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
        public void matrixMode(int mode) {
            GL11.glMatrixMode(mode);
        }
    }
}
