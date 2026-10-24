package blocamlimb.nhx100218.modernui.uilib.ui.render;

import java.util.concurrent.atomic.AtomicLong;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import blocamlimb.nhx100218.modernui.uilib.ui.base.cascade.UiBorderRadiusResolver;
import blocamlimb.nhx100218.modernui.uilib.ui.diagnostic.UiPerfMarkers;
import blocamlimb.nhx100218.modernui.uilib.ui.diagnostic.UiPerformanceMonitor;
import blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics;
import blocamlimb.nhx100218.modernui.uilib.util.UiNumbers;

/**
 * UI backdrop-filter 渲染管线。
 *
 * <p>从 {@code UiRenderContext} 抽出的"背后内容滤镜"职责：负责协调
 * 主层快照获取、shader 路径、固定管线 fallback、tint 兜底，以及全局
 * 最近一次渲染路径与诊断说明的记录。</p>
 *
 * <p>本类自身无外部公开 API；最近渲染路径仍由 {@link UiRenderContext} 暴露
 * （静态访问器通过本类 trampoline 读取）。</p>
 */
final class UiBackdropFilterRenderer {

    /** 诊断关闭时 detail 的占位常量：引用同一常量，不付出任何字符串构造。 */
    private static final String DIAGNOSTICS_DISABLED_DETAIL = "diagnostics-disabled";

    private static final float[][] UI_BACKDROP_BLUR_SAMPLES = new float[][] {
            { -1.0F, 0.0F, 0.18F },
            { 1.0F, 0.0F, 0.18F },
            { 0.0F, -1.0F, 0.18F },
            { 0.0F, 1.0F, 0.18F },
            { -1.0F, -1.0F, 0.12F },
            { 1.0F, -1.0F, 0.12F },
            { -1.0F, 1.0F, 0.12F },
            { 1.0F, 1.0F, 0.12F }
    };

    private static volatile BackdropFilterRenderPath lastRenderPath = BackdropFilterRenderPath.NONE;
    private static volatile String lastDetail = "not-run";
    /**
     * 进程级 backdrop 滤波请求累计次数（入口计数，与最终走哪条路径无关）。
     *
     * <p>存在理由：「最近一次路径」这类<b>最后值</b>读数无法回答「这段渲染窗口里究竟有没有发生玻璃请求」
     * —— 上一次请求的残留与本次真的走了同一路径在读数上完全一样。需要按窗口判定时（headless 出图摘要
     * 要如实报「本帧没走玻璃」而不是上次的残留）必须有一个<b>单调计数</b>可比较。</p>
     */
    private static final AtomicLong invocationCount = new AtomicLong();

    private UiBackdropFilterRenderer() {}

    /**
     * 返回最近一次 backdrop-filter 实际渲染路径。
     */
    static BackdropFilterRenderPath getLastRenderPath() {
        return lastRenderPath;
    }

    /**
     * 返回最近一次 backdrop-filter 诊断说明。
     */
    static String getLastDetail() {
        return lastDetail;
    }

    /**
     * 返回进程级 backdrop 滤波请求累计次数。
     *
     * <p>计数点在完整入口，因此被页面策略禁用、被档位短路、快照不可用、降级到固定管线或 tint 兜底
     * 的请求<b>同样计入</b> —— 它回答的是「有没有发生玻璃请求」，不是「shader 画了几次」。</p>
     *
     * @return 累计次数
     */
    static long getInvocationCount() {
        return invocationCount.get();
    }

    /**
     * 渲染一次 backdrop-filter，并记录最终路径。
     *
     * @param context 调用方渲染上下文
     * @param left 左侧坐标
     * @param top 顶部坐标
     * @param right 右侧坐标
     * @param bottom 底部坐标
     * @param blurRadius 模糊半径像素
     * @param saturation 饱和度倍率，1.0 表示不改变
     * @param cornerRadii 四角圆角
     */
    static void render(UiRenderContext context, int left, int top, int right, int bottom, int blurRadius,
            float saturation, UiBorderRadiusResolver.ResolvedCornerRadii cornerRadii) {
        render(context, left, top, right, bottom, blurRadius, saturation, cornerRadii, (UiGlassMaterial) null);
    }

    /**
     * 渲染一次 backdrop-filter，带 iOS 材质档。
     *
     * @param context 调用方渲染上下文
     * @param left 左侧坐标
     * @param top 顶部坐标
     * @param right 右侧坐标
     * @param bottom 底部坐标
     * @param blurRadius 模糊半径像素
     * @param saturation 饱和度倍率；material 非空时语义转为 vibrancy 乘子（1.0=严格采用材质配方值）
     * @param cornerRadii 四角圆角
     * @param material iOS 风格材质档；为 null 时走旧的线性饱和度语义
     */
    static void render(UiRenderContext context, int left, int top, int right, int bottom, int blurRadius,
            float saturation, UiBorderRadiusResolver.ResolvedCornerRadii cornerRadii, UiGlassMaterial material) {
        render(context, left, top, right, bottom, blurRadius, saturation, cornerRadii,
                UiBackdropEffect.classic(material));
    }

    /**
     * 渲染一次 backdrop 效果（带完整配方：家族 + 材质档 + 液态强度）。
     *
     * @param context 调用方渲染上下文
     * @param left 左侧坐标
     * @param top 顶部坐标
     * @param right 右侧坐标
     * @param bottom 底部坐标
     * @param blurRadius 模糊半径像素
     * @param saturation 饱和度倍率；effect 带材质档时语义转为 vibrancy 乘子
     * @param cornerRadii 四角圆角
     * @param effect 效果配方；null 等价旧线性饱和度语义
     */
    static void render(UiRenderContext context, int left, int top, int right, int bottom, int blurRadius,
            float saturation, UiBorderRadiusResolver.ResolvedCornerRadii cornerRadii, UiBackdropEffect effect) {
        invocationCount.incrementAndGet();
        BackdropBlurPolicy policy = context == null ? BackdropBlurPolicy.inheritGlobal()
                : context.getBackdropBlurPolicy();
        BackdropBlurConfig config = BackdropBlurConfig.getInstance();
        if (!policy.resolveEnabled(config)) {
            recordPath(BackdropFilterRenderPath.NONE, "disabled by page policy");
            return;
        }
        // 材质档自带 tint / 亮边 / 噪点：blur=0 仍有视觉产出（等价 CSS
        // backdrop-filter: blur(0) saturate(...)——不糊但材质照旧），故不能按旧规则短路。
        // 旧语义（material=null）保持"blur<=0 且饱和度恒等"即无操作的既有短路。
        boolean noVisualEffect = effect == null
                && blurRadius <= 0 && Float.compare(saturation, 1.0F) == 0;
        if (right <= left || bottom <= top || noVisualEffect) {
            recordPath(BackdropFilterRenderPath.NONE, "skipped");
            return;
        }
        // W2 可见性短路：表面矩形与当前 clip 盒无像素级交集时，后续 scissor/stencil 必然把它整块裁掉，
        // 取快照、设 uniform、draw 全是白费（大半径玻璃表面常常整块滚出滚动视口）。
        // 放置位置刻意在既有短路之后：策略关闭/几何退化两条路径的 recordPath 语义保持原样。
        if (!isVisibleInCurrentClip(context, left, top, right, bottom)) {
            recordPath(BackdropFilterRenderPath.NONE, "clipped");
            return;
        }
        String pendingFallbackDetail = drawCurrentUiBackdropFilter(context, left, top, right, bottom, blurRadius,
                saturation, cornerRadii, effect);
        if (pendingFallbackDetail == null) {
            return;
        }
        drawTintFallback(context, left, top, right, bottom, blurRadius, saturation, cornerRadii,
                pendingFallbackDetail, effect);
    }

    /**
     * 走主流程：当前 UI 主层快照 + shader / 固定管线模糊。
     *
     * @return 当走完仍未完成绘制时返回 fallback 诊断字符串；成功完成绘制时返回 {@code null}
     */
    private static String drawCurrentUiBackdropFilter(UiRenderContext context, int left, int top, int right,
            int bottom, int blurRadius, float saturation, UiBorderRadiusResolver.ResolvedCornerRadii cornerRadii,
            UiBackdropEffect effect) {
        int screenWidth = context.getScreenWidth();
        int screenHeight = context.getScreenHeight();
        UiMainLayerSnapshotService snapshotService = context.getMainLayerSnapshotService();

        SampleRegion sampleRegion = UiMainLayerSnapshotService.resolveSampleRegion(screenWidth, screenHeight, left,
                top, right, bottom, blurRadius);
        if (sampleRegion == null) {
            return "texture-copy-unavailable";
        }

        int backdropSourceFramebufferId = context.getCurrentBackdropSourceFramebufferId();
        MainLayerSnapshot snapshot = snapshotService.acquireSnapshot(screenWidth, screenHeight,
                backdropSourceFramebufferId, context.getMainLayerContentRevisionForDiagnostics(), sampleRegion,
                blurRadius);
        if (snapshot == null) {
            return snapshotUnavailableDetail(snapshotService);
        }

        // Shader 用连续覆盖率裁出圆角；自身 stencil 会把半透明弧边再次硬切掉。
        // 矩形约束仍与祖先 scissor/stencil 求交，固定管线回退另加圆角裁剪。
        // 保护域（clip 栈 + attrib 帧）必须建立在 try 之前一行的位置上：原先 pushClip/pushAttrib 落在 try 外面，
        // 两次 glGetInteger 抛异常时两处栈各多一层且 finally 回滚不到（GL 自净审查 N7）。
        int previousProgram = 0;
        int previousActiveTexture = GL13.GL_TEXTURE0;
        int previousTextureBinding = 0;
        boolean clipPushed = false;
        boolean attribPushed = false;
        boolean stateCaptured = false;
        boolean drewBackdrop = false;
        boolean fixedPipelineClip = false;
        // P7：显式捕获 try 体失败，供 finally 与恢复失败合并。原先的裸 try/finally 在「try 体正在抛 +
        // 恢复步骤也抛」时会用 finally 的异常替换在飞异常（P4 挂账），诊断信息丢失。
        Throwable bodyFailure = null;
        try {
            context.pushClip(left, top, right, bottom, 0);
            clipPushed = true;
            GlStateDiagnostics.pushAttrib(GL11.GL_ALL_ATTRIB_BITS, "UiBackdropFilterRenderer");
            attribPushed = true;
            previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            previousTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            stateCaptured = true;
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, snapshot.getTextureId());
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            // 直接修改主层时保留已有 alpha；读取父 FBO 写入独立透明层时必须建立覆盖率，
            // 否则会留下 RGB 非零/alpha 为零的像素，回贴时变成加色。RGB 始终按覆盖率混合。
            // 合成层活动时（>=0）快照源是父层 FBO：这是"读取父 FBO 写入独立透明层"的判据，不是状态泄漏标记。
            boolean isolatedLayer = backdropSourceFramebufferId >= 0;
            GL11.glColorMask(true, true, true, true);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ZERO, isolatedLayer ? GL11.GL_ONE_MINUS_SRC_ALPHA : GL11.GL_ONE);

            float[] lightDir = resolveLightDirection(context, left, top, right, bottom);
            if (drawBackdropTextureWithShader(left, top, right, bottom, snapshot.getSampleLeft(),
                    snapshot.getSampleTop(), snapshot.getWidth(), snapshot.getHeight(), snapshot.getTextureWidth(),
                    snapshot.getTextureHeight(), snapshot.getDownsampleFactor(), blurRadius, saturation,
                    context.getBackdropBlurPolicy(), snapshot, effect, cornerRadii,
                    lightDir[0], lightDir[1], isolatedLayer)) {
                drewBackdrop = true;
                return null;
            }

            BackdropBlurPolicy policy = context.getBackdropBlurPolicy();
            BackdropBlurConfig config = BackdropBlurConfig.getInstance();
            if (!policy.resolveFixedPipelineEnabled(config)) {
                return "fixed-pipeline-disabled";
            }
            if (blurRadius <= 0) {
                return "shader-and-blur-unavailable";
            }

            context.pushClip(left, top, right, bottom, cornerRadii);
            fixedPipelineClip = true;
            GL11.glDisable(GL11.GL_BLEND);
            drawBackdropTextureQuad(left, top, right, bottom, snapshot.getSampleLeft(), snapshot.getSampleTop(),
                    snapshot.getWidth(), snapshot.getHeight(), 0.0F, 0.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float sampleStep = (float) resolveBackdropSampleStep(blurRadius);
            int sampleCount = Math.min(config.getFixedPipelineSampleCount(), UI_BACKDROP_BLUR_SAMPLES.length);
            for (int i = 0; i < sampleCount; i++) {
                float[] sample = UI_BACKDROP_BLUR_SAMPLES[i];
                GL11.glColor4f(1.0F, 1.0F, 1.0F, sample[2]);
                drawBackdropTextureQuad(left, top, right, bottom, snapshot.getSampleLeft(), snapshot.getSampleTop(),
                        snapshot.getWidth(), snapshot.getHeight(), sample[0] * sampleStep, sample[1] * sampleStep);
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            // 固定管线逐 quad 叠加无法表达 vibrancy/亮边/噪点，材质档在此降级为"仅模糊"。
            recordPath(BackdropFilterRenderPath.FIXED_PIPELINE,
                    fixedPipelinePathDetail(sampleCount, effect, snapshot));
            drewBackdrop = true;
            return null;
        } catch (RuntimeException failure) {
            bodyFailure = failure;
        } catch (LinkageError failure) {
            // LinkageError 单列只为与 GlStateScope.run / restoreStep 的口径逐字对齐；语义上它已被 catch (Error) 覆盖。
            bodyFailure = failure;
        } catch (Error failure) {
            bodyFailure = failure;
        } finally {
            // 逐步恢复 + 失败累积：前项抛异常不得跳过后续（原实现 glUseProgram 失败会让 attrib 帧
            // 与 clip 都不弹，releaseSnapshot 也被跳过，GL 自净审查 N9）。
            Throwable restoreFailure = null;
            if (fixedPipelineClip) {
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        context.popClip();
                    }
                });
            }
            if (stateCaptured) {
                final int program = previousProgram;
                final int activeUnit = previousActiveTexture;
                final int texture = previousTextureBinding;
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        GL20.glUseProgram(program);
                    }
                });
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        GL13.glActiveTexture(activeUnit);
                    }
                });
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                    }
                });
            }
            if (attribPushed) {
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        GlStateDiagnostics.popAttrib("UiBackdropFilterRenderer");
                    }
                });
            }
            if (clipPushed) {
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        context.popClip();
                    }
                });
            }
            if (snapshot != null) {
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        snapshotService.releaseSnapshot(snapshot);
                    }
                });
            }
            if (drewBackdrop) {
                restoreFailure = restoreStep(restoreFailure, new Runnable() {
                    @Override
                    public void run() {
                        context.notifyMainLayerContentChanged();
                    }
                });
            }
            // 在飞异常合并（P4 挂账 → P7 闭合）：恢复失败为主异常、try 体失败挂 suppressed；若 try 体
            // 失败是致命 Error 而恢复失败不是，按仓内统一口径把它升为主异常（appendFailure/isFatal）。
            // 两者都为空时不抛出：正常返回与提前 return 的返回值不受影响。
            Throwable combined = appendFailure(restoreFailure, bodyFailure);
            if (combined != null) {
                throwUnchecked(combined);
            }
        }
        // 编译器的可达性出口：正常路径在 try 体内 return，异常路径必在 finally 重抛，此处不可达。
        // 若将来有人改坏合并逻辑让 try 体失败被静默吞掉，宁可 fail-loud 也不要静默返回"成功"。
        throw new IllegalStateException("背景滤镜：try 体失败未被重抛（不可达出口被触达）");
    }

    /**
     * 执行一步恢复并累积失败（不中断后续步骤）。
     *
     * <p>捕获面为全部 unchecked 失败（含 VM 级 {@link Error}）：恢复链中途抛出时若直接向上传播，
     * 后面的恢复步骤全部被跳过，状态漂移会跨帧累积——这正是本仓自净审查要求消除的失败模式。
     * 致命性由 {@link #appendFailure} 保留：Error 升级为主异常而不是降级成 suppressed。</p>
     */
    private static Throwable restoreStep(Throwable failure, Runnable step) {
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

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new RuntimeException(failure);
    }

    private static boolean drawBackdropTextureWithShader(int left, int top, int right, int bottom, int sampleLeft,
            int sampleTop, int sampleWidth, int sampleHeight, int textureWidth, int textureHeight,
            int downsampleFactor, int blurRadius, float saturation, BackdropBlurPolicy backdropBlurPolicy,
            MainLayerSnapshot snapshot, UiBackdropEffect effect,
            UiBorderRadiusResolver.ResolvedCornerRadii panelCornerRadii, float lightDirX, float lightDirY,
            boolean isolatedLayer) {
        BackdropBlurConfig config = BackdropBlurConfig.getInstance();
        BackdropBlurPolicy policy = backdropBlurPolicy == null ? BackdropBlurPolicy.inheritGlobal()
                : backdropBlurPolicy;
        if (!policy.resolveShaderEnabled(config)) {
            recordPath(BackdropFilterRenderPath.FIXED_PIPELINE, "shader disabled by config");
            return false;
        }
        // 抽头预算来自进程级档位（volatile 直读：零分配、不建立依赖追踪、不触发主题重派生）。
        // 同一预算永远拿到同一程序实例，故本调用不会新建 GL program。
        int tapBudget = BackdropQualityService.getInstance().current().tapBudget();
        UiBackdropShaderProgram program = UiBackdropShaderProgram.programFor(tapBudget);
        if (!program.ensureInitialized()) {
            recordPath(BackdropFilterRenderPath.FIXED_PIPELINE, shaderUnavailableDetail(program));
            return false;
        }
        program.bind();
        program.setUniformI("mainTex", 0);
        program.setUniformF("sourceAlphaPass", 0.0F);
        program.setUniform2f("texelSize", 1.0F / (float) textureWidth, 1.0F / (float) textureHeight);
        program.setUniformF("blurRadius", resolveBackdropShaderRadius(blurRadius,
                downsampleFactor, policy));
        program.setUniformF("saturation", Math.max(0.0F, saturation));
        // 面板局部坐标基准：模型空间原点是面板左上角，减去后 GUI scale 自然约掉。
        program.setUniform2f("panelOrigin", (float) left, (float) top);
        program.setUniform2f("panelSizePx", (float) Math.max(1, right - left),
                (float) Math.max(1, bottom - top));
        // 四角半径（左上/右上/右下/左下），供 SDF 圆角亮边使用；与 panelSizePx 同一像素空间。
        UiBorderRadiusResolver.ResolvedCornerRadii radii = UiBorderRadiusResolver.scaleToFit(
                panelCornerRadii == null ? UiBorderRadiusResolver.ResolvedCornerRadii.uniform(0) : panelCornerRadii,
                Math.max(1, right - left), Math.max(1, bottom - top));
        program.setUniform4f("cornerRadii", (float) radii.getTopLeft(),
                (float) radii.getTopRight(), (float) radii.getBottomRight(), (float) radii.getBottomLeft());
        // 面板短边半宽（屏幕像素）：给折射位移做尺寸上限，见 applyMaterialUniforms。
        float panelShortHalfPx = Math.min(Math.max(1, right - left), Math.max(1, bottom - top)) * 0.5F;
        applyMaterialUniforms(program, effect, saturation, lightDirX, lightDirY,
                Math.max(1, downsampleFactor), panelShortHalfPx);
        drawBackdropTextureQuad(left, top, right, bottom, sampleLeft, sampleTop, sampleWidth, sampleHeight,
                0.0F, 0.0F);
        if (isolatedLayer) {
            // 首遍 RGB = src * coverage + dst * (1-coverage)，alpha 仅衰减旧目标。
            // 第二遍只累加 sampleAlpha * coverage；保留旧 replacement 在透明父层/非空子层
            // 的语义。不能让 sampleAlpha 参与首遍 RGB 混合，也不能把 RGB 送进加法混合。
            GL11.glColorMask(false, false, false, true);
            GL14.glBlendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
            program.setUniformF("sourceAlphaPass", 1.0F);
            drawBackdropTextureQuad(left, top, right, bottom, sampleLeft, sampleTop, sampleWidth, sampleHeight,
                    0.0F, 0.0F);
            GL11.glColorMask(true, true, true, true);
        }
        program.unbind();
        recordShaderSurfaceCounters(left, top, right, bottom, tapBudget, isolatedLayer);
        recordPath(BackdropFilterRenderPath.SHADER, shaderPathDetail(blurRadius, saturation, effect, snapshot));
        return true;
    }

    /**
     * 下发材质档 uniform。
     *
     * <p>material 为 null 时显式把 iosMaterial 置 0 并把所有材质附加项归零：
     * shader 走旧的线性饱和度分支，亮边/噪点也严格不产生，保证旧调用方观感
     * 与升级前逐像素一致（缺 uniform 只会静默留 0，但那依赖"恰好为 0"的巧合，
     * 显式赋值才可读）。</p>
     *
     * <p>材质档下入参 {@code saturationMultiplier} 的语义从"线性饱和度乘子"转为
     * <strong>vibrancy 乘子</strong>：1.0 表示严格采用材质配方值，&gt;1 更艳、&lt;1 更哑。
     * 之所以复用同一参数位而不是新增重载，是因为材质档本来就不读线性饱和度——
     * 留着一个被静默忽略的旋钮比改语义更会让人误判（验收页的滑杆会变成死控件）。
     * 不设 1.0 下限：低于 1 在 shader 里是"按亮度加权去饱和"的合法哑光玻璃观感，
     * 强行夹住反而会让整段低区间变成等值的死区。</p>
     *
     * @param program 目标着色器程序（按档位抽头预算选定；同一预算永远是同一实例）
     * @param material 材质档，可为 null
     * @param saturationMultiplier 旧语义的线性饱和度乘子，或材质档的 vibrancy 倍率
     * @param panelShortHalfPx 面板短边半宽（屏幕像素）：折射位移的尺寸上限来源，见方法体注释
     */
    private static void applyMaterialUniforms(UiBackdropShaderProgram program, UiBackdropEffect effect,
            float saturationMultiplier, float lightDirX, float lightDirY, int snapshotDownsampleFactor,
            float panelShortHalfPx) {
        UiGlassMaterial material = effect == null ? null : effect.getMaterial();
        boolean liquid = effect != null && effect.isLiquid();
        // 液态三参数在所有路径显式赋值（含 null/经典），缺省留 0 依赖"恰好为 0"不可读。
        program.setUniformF("liquidGlass", liquid ? 1.0F : 0.0F);
        // 作者侧屏幕像素 -> 纹理素（与 blurRadius 同口径换算）。下限不是 0：液态档一旦
        // 启用就必须肉眼可辨，原 2~12 区间在低强度段几乎无感，拉满也只有轻微弯折，
        // 用户会误判成"Liquid Glass 没生效"。斜率同时从 27 提到 40（50% 强度处 16.5 -> 26px）。
        //
        // 上限按面板短边收敛（0.8×半短边）：位移是无量纲的"绝对像素"，而可弯折的
        // 素材只有面板那么大。聊天气泡短边 28px 若照吃 26px 位移，缘带会把轮廓外很远
        // 的内容拽进来，边缘糊成一条脏带而不是鼓起的透镜缘。大面板不受约束。
        float refractionPx = liquid
                ? Math.min(6.0F + 40.0F * effect.getLensStrength(), panelShortHalfPx * 0.8F) : 0.0F;
        program.setUniformF("refraction",
                refractionPx / (float) snapshotDownsampleFactor);
        // 厚度 tint 是基础材质吸收率的相对增量，由 shader 乘 materialTint.a；
        // 不作为独立深色蒙层，否则大面板的宽折射带会呈现黑框。
        program.setUniformF("edgeTint", liquid ? 0.14F + 0.34F * effect.getLensStrength() : 0.0F);
        program.setUniform2f("lightDir", lightDirX, lightDirY);
        if (material == null) {
            program.setUniformF("iosMaterial", 0.0F);
            program.setUniformF("vibrancy", 1.0F);
            program.setUniform4f("materialTint", 1.0F, 1.0F, 1.0F, 0.0F);
            program.setUniform3f("materialLift", 0.0F, 0.0F, 0.0F);
            program.setUniformF("edgeHighlight", 0.0F);
            program.setUniformF("innerLightTop", 0.0F);
            program.setUniformF("innerShadowBottom", 0.0F);
            program.setUniformF("noiseAmount", 0.0F);
            // 旧语义：保持升级前的规则十字核行为，不做按像素旋转，逐像素可复现。
            program.setUniformF("kernelJitter", 0.0F);
            return;
        }
        program.setUniformF("iosMaterial", 1.0F);
        program.setUniformF("vibrancy", material.getVibrancy()
                * Math.max(0.0F, saturationMultiplier));
        program.setUniform4f("materialTint", material.getTintRed(), material.getTintGreen(),
                material.getTintBlue(), material.getTintAlpha());
        program.setUniform3f("materialLift", material.getLuminanceLift(),
                material.getLuminanceLift(), material.getLuminanceLift());
        // 液态档的镜面增益。上一版取 ×(1+3.6s)（50% 处 ×2.8）把峰值推到 +103/255、
        // 且压在 2px 宽的环带上，结果就是真机反馈的「边缘生硬」（1px 内 42->145、
        // 蓝通道 clip 到 255 = 一条画上去的白线）。这一版幅度退到 ×(1+1.4s)，
        // 光泽改由 shader 侧的**环带宽度 + 峰值内移 + 对向次高光**承担——
        // 参考 WebGlass：specular-strength 0.65 配 specular-width 0.25，软来自分布
        // 而不是来自更高的峰值。经典档恒 1.0 倍，严格不受影响。
        program.setUniformF("edgeHighlight", material.getEdgeHighlight()
                * (liquid ? 1.0F + 1.4F * effect.getLensStrength() : 1.0F));
        program.setUniformF("innerLightTop", material.getInnerLightTop());
        program.setUniformF("innerShadowBottom", material.getInnerShadowBottom());
        program.setUniformF("noiseAmount", material.getNoiseAmount());
        // 材质档启用按像素旋转采样盘：消除固定核的"蜡感"，且不含时间项故静止画面不闪烁。
        program.setUniformF("kernelJitter", 1.0F);
    }

    private static void drawBackdropTextureQuad(int left, int top, int right, int bottom, int sampleLeft, int sampleTop,
            int sampleWidth, int sampleHeight, float sampleOffsetX, float sampleOffsetY) {
        float leftU = UiNumbers.clamp01(((float) left + sampleOffsetX - (float) sampleLeft) / (float) sampleWidth);
        float rightU = UiNumbers.clamp01(((float) right + sampleOffsetX - (float) sampleLeft) / (float) sampleWidth);
        float topV = UiNumbers.clamp01(1.0F - ((float) top + sampleOffsetY - (float) sampleTop) / (float) sampleHeight);
        float bottomV = UiNumbers.clamp01(1.0F - ((float) bottom + sampleOffsetY - (float) sampleTop) / (float) sampleHeight);
        // 架构禁令:不使用原版包装类(Tessellator),直接 GL 立即模式
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(leftU, bottomV);
        GL11.glVertex2f((float) left, (float) bottom);
        GL11.glTexCoord2f(rightU, bottomV);
        GL11.glVertex2f((float) right, (float) bottom);
        GL11.glTexCoord2f(rightU, topV);
        GL11.glVertex2f((float) right, (float) top);
        GL11.glTexCoord2f(leftU, topV);
        GL11.glVertex2f((float) left, (float) top);
        GL11.glEnd();
    }

    private static void drawTintFallback(UiRenderContext context, int left, int top, int right, int bottom,
            int blurRadius, float saturation, UiBorderRadiusResolver.ResolvedCornerRadii cornerRadii,
            String fallbackDetail, UiBackdropEffect effect) {
        UiGlassMaterial material = effect == null ? null : effect.getMaterial();
        BackdropBlurConfig config = BackdropBlurConfig.getInstance();
        BackdropBlurPolicy policy = context.getBackdropBlurPolicy();
        if (!policy.resolveTintFallbackEnabled(config)) {
            recordPath(BackdropFilterRenderPath.NONE, tintFallbackDisabledDetail(fallbackDetail));
            return;
        }
        recordPath(BackdropFilterRenderPath.TINT_FALLBACK, tintFallbackDetail(fallbackDetail, effect));
        if (material != null) {
            // 材质档自带 tint 蒙层：降级时直接用它做纯色玻璃，保证 shader
            // 可用与否的两类机器看到的玻璃底色一致（模糊没了，但材质色与亮边还在）。
            int materialAlpha = UiNumbers.clamp(Math.round(material.getTintAlpha() * 255.0F)
                    + Math.max(0, blurRadius) / 2, 16, 200);
            int materialRgb = material.getTintArgb() & 0x00FFFFFF;
            int tintColor = materialAlpha << 24 | materialRgb;
            int highlightColor = UiNumbers.clamp(materialAlpha + 26, 32, 230) << 24 | materialRgb;
            context.drawSurface(left, top, right, bottom, tintColor, highlightColor, cornerRadii);
            return;
        }
        int tintAlpha = UiNumbers.clamp(18 + Math.max(0, blurRadius) * 2 + Math.round(Math.max(0.0F,
                saturation - 1.0F) * 16.0F), 18, 72);
        int highlightAlpha = UiNumbers.clamp(tintAlpha + 22, 32, 96);
        int tintColor = tintAlpha << 24 | 0x00FFFFFF;
        int highlightColor = highlightAlpha << 24 | 0x00FFFFFF;
        context.drawSurface(left, top, right, bottom, tintColor, highlightColor, cornerRadii);
    }

    private static void recordPath(BackdropFilterRenderPath renderPath, String detail) {
        lastRenderPath = renderPath == null ? BackdropFilterRenderPath.NONE : renderPath;
        lastDetail = detail == null ? "" : detail;
    }

    /**
     * W2 可见性事实来源：表面矩形是否与当前 clip 盒有像素级交集。
     *
     * <p>无上下文时（理论上只出现在测试/异常路径）不具备裁剪事实，一律按可见处理——
     * 短路只允许由"确实被裁掉"这一事实触发，不能由"不知道"触发。语义见
     * {@link UiRenderContext#intersectsCurrentClip(int, int, int, int)}。</p>
     */
    private static boolean isVisibleInCurrentClip(UiRenderContext context, int left, int top, int right,
            int bottom) {
        return context == null || context.intersectsCurrentClip(left, top, right, bottom);
    }

    /** W3 诊断门控：默认开启 ⇒ 默认行为与引入门控前逐字符一致。 */
    private static boolean diagnosticsEnabled() {
        return BackdropBlurConfig.getInstance().getDiagnosticsEnabled();
    }

    /**
     * shader 路径成功后的 detail：诊断关闭时不构造（detail 是每表面每帧的纯诊断成本，
     * 与采样正确性无关）。{@code lastRenderPath} 仍照旧恒写，既有路径断言不受影响。
     *
     * <p>下面这组 detail 构造器包内可见，仅为让离线测试能钉住 W3 门控
     * （与本包 {@code resolveBackdropShaderRadius} 同法）：门控发生在字符串拼接之前，
     * GL 路径无法在无 GL 环境下端到端验证。</p>
     */
    static String shaderPathDetail(int blurRadius, float saturation, UiBackdropEffect effect,
            MainLayerSnapshot snapshot) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        UiGlassMaterial material = effect == null ? null : effect.getMaterial();
        return "blur=" + blurRadius + ", saturation="
                + String.format(java.util.Locale.ROOT, "%.2f", Float.valueOf(Math.max(0.0F, saturation)))
                + (effect == null ? "" : ", " + describeEffect(effect, material))
                + ", snapshot=" + formatSnapshotState(snapshot);
    }

    /** 固定管线降级的 detail（诊断关闭时不构造，含 snapshot 状态串）。 */
    static String fixedPipelinePathDetail(int sampleCount, UiBackdropEffect effect,
            MainLayerSnapshot snapshot) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        return "shader-unavailable, samples=" + sampleCount
                + (effect == null ? "" : ", effect-degraded(no-vibrancy)")
                + ", snapshot=" + formatSnapshotState(snapshot);
    }

    /** tint 兜底的 detail（诊断关闭时不构造，含效果描述）。 */
    static String tintFallbackDetail(String fallbackDetail, UiBackdropEffect effect) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        UiGlassMaterial material = effect == null ? null : effect.getMaterial();
        return fallbackDetail + (effect == null ? "" : ", " + describeEffect(effect, material));
    }

    /** tint 兜底被禁用的 detail（诊断关闭时不构造）。 */
    static String tintFallbackDisabledDetail(String fallbackDetail) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        return "tint-fallback-disabled: " + fallbackDetail;
    }

    /** 快照不可用的 detail（快照失败会持续多帧，构造成本按帧发生）。 */
    static String snapshotUnavailableDetail(UiMainLayerSnapshotService snapshotService) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        return "snapshot-unavailable: " + snapshotService.getLastFailureDetail();
    }

    /** 着色器不可用的 detail（程序不可用时每个表面每帧都会走到这里）。 */
    static String shaderUnavailableDetail(UiBackdropShaderProgram program) {
        if (!diagnosticsEnabled()) {
            return DIAGNOSTICS_DISABLED_DETAIL;
        }
        return "shader unavailable: " + program.getLastFailureMessage();
    }

    /**
     * 玻璃规模计数器：只在真正走完 shader 绘制后累加。
     *
     * <p>口径（Lead 2026-09-12 冻结，覆盖设计文档 §3 原表述）：</p>
     * <ul>
     *   <li>{@code surfaces}：本帧实际进入 shader 路径的表面数——被 W2 裁剪短路、被策略/档位
     *       短路、降级到固定管线或 tint 兜底的都不计，isolatedLayer 的第二遍 draw 也不重复计；</li>
     *   <li>{@code areaPx}：上述表面的<b>名义矩形面积</b>之和（不按 clip 缩减、不乘遍数）；</li>
     *   <li>{@code taps}：Σ 面积 × 抽头预算 × 遍数（isolatedLayer 为独立透明层，需要两遍
     *       draw，故遍数=2；否则 1）。计第二遍是硬要求——漏掉会把独立层的采样量低估一半。</li>
     * </ul>
     *
     * <p>{@code UiPerformanceMonitor.recordCounter} 自带第一道门控（帧内按本帧诊断域判定、
     * 帧外入有界待折叠桶），故这里不再重复判断；本方法自身只做 int/long 运算，热路径零分配。
     * {@code taps} 是估计值（同像素在不同分支下抽头数不同，
     * 且边缘覆盖率不改变 draw 遍数），用于 full/eco 档位 A/B 的量级对比。</p>
     */
    private static void recordShaderSurfaceCounters(int left, int top, int right, int bottom, int tapBudget,
            boolean isolatedLayer) {
        long areaPx = (long) Math.max(0, right - left) * (long) Math.max(0, bottom - top);
        long passes = isolatedLayer ? 2L : 1L;
        UiPerformanceMonitor monitor = UiPerformanceMonitor.getInstance();
        monitor.recordCounter(UiPerfMarkers.COUNTER_FRAME_BACKDROP_SURFACES, 1L);
        monitor.recordCounter(UiPerfMarkers.COUNTER_FRAME_BACKDROP_AREA_PX, areaPx);
        monitor.recordCounter(UiPerfMarkers.COUNTER_FRAME_BACKDROP_TAPS, areaPx * (long) tapBudget * passes);
    }

    private static int resolveBackdropSampleStep(int blurRadius) {
        return Math.max(1, Math.min(12, Math.round(Math.max(1, blurRadius) / 2.5F)));
    }

    /**
     * 用户声明半径（屏幕 px）-> shader 实际半径（快照 texel）的唯一换算口径。
     *
     * <p>包私有仅为让 {@code BackdropBlurRadiusDomainTest} 能钉住「聊天可表达域 0..64 撞不到渲染层
     * 上限」这条不变量（与本包 {@code resolveBackdropSampleStep} 同法）。{@code 0.75F} 是
     * 用户 px -> 高斯半径换算，{@code /downsampleFactor} 是屏幕 px -> 快照 texel 换算，
     * 两者不是一层折扣；改任意一项都要同步那条测试。</p>
     */
    static float resolveBackdropShaderRadius(int blurRadius, int downsampleFactor,
            BackdropBlurPolicy backdropBlurPolicy) {
        if (blurRadius <= 0) {
            return 0.0F;
        }
        BackdropBlurConfig config = BackdropBlurConfig.getInstance();
        BackdropBlurPolicy policy = backdropBlurPolicy == null ? BackdropBlurPolicy.inheritGlobal()
                : backdropBlurPolicy;
        float maxShaderRadius = Math.min(config.getShaderBlurRadiusLimit(), policy.resolveMaxBlurRadius(config));
        return Math.max(1.0F, Math.min(maxShaderRadius, (float) blurRadius * 0.75F
                / (float) Math.max(1, downsampleFactor)));
    }

    private static String formatSnapshotState(MainLayerSnapshot snapshot) {
        if (snapshot == null) {
            return "none";
        }
        return (snapshot.isReused() ? "reused" : "captured") + " " + snapshot.getWidth() + "x"
                + snapshot.getHeight() + " @" + snapshot.getSampleLeft() + "," + snapshot.getSampleTop()
                + " fbo=" + snapshot.getReadFramebufferId() + " rev=" + snapshot.getContentRevision()
                + " region=" + snapshot.getRegionDetail() + " " + snapshot.getTileDetail()
                + " filter=" + snapshot.getFilterDetail();
    }

    /** 诊断串里的效果描述：家族 + 档名 + 液态强度（shader 与降级路径共用）。 */
    private static String describeEffect(UiBackdropEffect effect, UiGlassMaterial material) {
        StringBuilder sb = new StringBuilder();
        sb.append("family=").append(effect.getFamily().name());
        if (material != null) {
            sb.append(", material=").append(material.name())
                    .append(" vibrancy=")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", Float.valueOf(material.getVibrancy())));
        }
        if (effect.isLiquid()) {
            sb.append(" lens=").append(String.format(java.util.Locale.ROOT, "%.2f",
                    Float.valueOf(effect.getLensStrength())));
        }
        return sb.toString();
    }

    /**
     * 解析随动缘光的光源方向：以指针相对面板中心的方向为单位向量。
     *
     * <p>官方 Liquid Glass 的缘光"responds to device motion"；MC 1.7.10 无陀螺仪，
     * 宿主以鼠标指针为虚拟光源——指针在哪个方位，缘带就朝哪边最亮。指针不可得或
     * 恰在中心时退回静态默认光向 {@link #DEFAULT_LIGHT_ANGLE_DEG}。</p>
     *
     * <h3>默认光向取一手参考的数值约定（-55°，右上）</h3>
     * <p>参考 WebGlass tokens：{@code --wg-light-angle} 默认 -55，约定为"自 +x 顺时针
     * 计量、0=右、-90=上、90=下"，即屏幕 y 轴向下——与本 shader 的 {@code sdfGradient}
     * 同一坐标系，角度可直接换算不需翻转。该文档正文另有一句"upper-left"与其数值和
     * ASCII 示意图（光源箭头画在右上 ↗）自相矛盾，按数值+示意图两项取<b>右上 55°</b>。
     * 本仓此前用的 (0.32, -0.95) 等价 -71.4°（几乎正上），且旧注释误写作"左上"，
     * 一并纠正。要改成左上镜像：把角度取为 -125°。</p>
     *
     * <p>口径如实说明：{@code getMouseX/Y} 是屏幕绝对坐标，而面板矩形可能处于
     * 宿主局部空间（带 absX/absY 偏移），偏移大时缘光方向会偏。缘光只是装饰性
     * 方向调制，不影响采样正确性；若将来要精确，需把面板矩形换算到屏幕空间后
     * 再传进来（会牵动 render 签名，暂不做）。</p>
     */
    /**
     * 静态默认光向（度，屏幕 y 轴向下、自 +x 顺时针计量）。-55° = 右上、仰 55°，
     * 取自一手参考 WebGlass 的 --wg-light-angle 默认值。见 resolveLightDirection 的
     * 约定说明与文档内部矛盾处置。
     */
    private static final float DEFAULT_LIGHT_ANGLE_DEG = -55.0F;
    private static float[] resolveLightDirection(UiRenderContext context, int left, int top, int right,
            int bottom) {
        float centerX = (float) (left + right) * 0.5F;
        float centerY = (float) (top + bottom) * 0.5F;
        float dirX = (float) Math.cos(Math.toRadians(DEFAULT_LIGHT_ANGLE_DEG));
        float dirY = (float) Math.sin(Math.toRadians(DEFAULT_LIGHT_ANGLE_DEG));
        if (context != null) {
            float px = (float) context.getMouseX() - centerX;
            float py = (float) context.getMouseY() - centerY;
            float length = (float) Math.sqrt(px * px + py * py);
            if (length > 1.0F) {
                dirX = px / length;
                dirY = py / length;
            }
        }
        return new float[] { dirX, dirY };
    }
}