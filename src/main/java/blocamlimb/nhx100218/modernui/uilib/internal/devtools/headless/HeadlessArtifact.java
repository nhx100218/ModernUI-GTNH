package blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless;

import java.nio.file.Path;

import blocamlimb.nhx100218.modernui.uilib.internal.chat3.view.ChatHudWindow;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.view.ChatSceneController;

/**
 * 一次出图的产物：像素文件 + 自检 + 命令面摘要 + 能力快照 + 耗时。
 *
 * <p>不变量：<b>像素必须带证据</b>——自检回答「画出来没有」，命令面摘要回答「下发过什么命令」。
 * 只返回路径而不给出证据，是历史上「空白图被当成代码 bug」与「静默 no-op 无人察觉」的直接来源。</p>
 */
public final class HeadlessArtifact {

    private final HeadlessRequest request;
    private final HeadlessCapabilities capabilities;
    private final HeadlessSelfCheck.Report selfCheck;
    private final HeadlessDrawSummary drawSummary;
    private final Path output;
    private final long pngBytes;
    private final long elapsedMillis;
    private final int renderedFrames;
    private final String inputSummary;
    private final String performanceSummary;
    private final String backdropSummary;

    HeadlessArtifact(HeadlessRequest request, HeadlessCapabilities capabilities,
            HeadlessSelfCheck.Report selfCheck, HeadlessDrawSummary drawSummary, Path output, long pngBytes,
            long elapsedMillis, int renderedFrames, String inputSummary, String performanceSummary,
            String backdropSummary) {
        this.request = request;
        this.capabilities = capabilities;
        this.selfCheck = selfCheck;
        this.drawSummary = drawSummary;
        this.output = output;
        this.pngBytes = pngBytes;
        this.elapsedMillis = elapsedMillis;
        this.renderedFrames = renderedFrames;
        this.inputSummary = inputSummary;
        this.performanceSummary = performanceSummary;
        this.backdropSummary = backdropSummary;
    }

    /** @return 源请求 */
    public HeadlessRequest request() {
        return request;
    }

    /** @return 出图时的能力快照 */
    public HeadlessCapabilities capabilities() {
        return capabilities;
    }

    /** @return 像素自检报告 */
    public HeadlessSelfCheck.Report selfCheck() {
        return selfCheck;
    }

    /** @return 命令面摘要 */
    public HeadlessDrawSummary drawSummary() {
        return drawSummary;
    }

    /** @return PNG 绝对或相对路径 */
    public Path output() {
        return output;
    }

    /** @return PNG 字节数 */
    public long pngBytes() {
        return pngBytes;
    }

    /** @return 实际渲染帧数（稳定判据收敛时的帧数；等于上限表示未收敛） */
    public int renderedFrames() {
        return renderedFrames;
    }

    /** @return 本次 capture 的墙钟耗时（毫秒） */
    public long elapsedMillis() {
        return elapsedMillis;
    }

    /**
     * 帧内事实摘要（阶段耗时、计数、最慢控件），仅当请求声明诊断采样时存在。
     *
     * <p>它是耗时事实而非像素事实：同一命令两次出图的该字段天然不同，故不参与任何出图对拍，
     * 也不写进 PNG。回答的问题是「这一帧花在哪」，而不是「画成了什么」。</p>
     *
     * @return 统计摘要；未采样时为 null
     */
    public String performanceSummary() {
        return performanceSummary;
    }

    /**
     * 本次出图期间的 backdrop（玻璃）滤波事实：走了哪条路径 + 该路径的诊断说明。
     *
     * <p>它回答的是「这张图的玻璃是不是真的按材质档画的」：{@code shader} 是完整路径，
     * {@code fixed-pipeline} / {@code tint-fallback} 是降级档（观感证据只到模糊/实色）；
     * {@code none} 表示本窗口<b>最后一次玻璃请求没有成功路径</b> —— 既可能是「一次请求都没发起」（此时正文
     * 是未发起说明），也可能是最后一次请求被策略 / 档位 / 几何 / 裁剪短路（正文给出该原因）；两者按同行
     * {@code requests=} 的<b>有无</b>区分（有 = 发生过请求、无 = 未发起）。判定取请求计数差而非「最近一次路径」的最后值：后者在批量同进程出图时会把
     * 上一档的残留报成本档事实（见 {@code UiRenderContext#getBackdropFilterInvocationCount()}）。</p>
     *
     * <p><b>可参与对拍</b>：路径、请求数与 detail 里的 {@code rev=N} 都只由请求决定（时间事实已解耦，
     * F27），实测同命令多次运行该行逐字节相同。它仍是<b>过程读数</b>而非像素事实，故不进 PNG；
     * 默认打印的理由是玻璃路径决定像素证据的<b>强度分级</b>（不看它无法判断一张图能否据此下观感结论）。</p>
     *
     * @return 玻璃路径摘要
     */
    public String backdropSummary() {
        return backdropSummary;
    }

    /** @return 多行可读摘要（CLI 默认输出） */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("[headless] request: ").append(request.summary()).append('\n');
        sb.append("[headless] capabilities: ").append(capabilities.summary()).append('\n');
        sb.append("[headless] input: ").append(inputSummary).append('\n');
        sb.append("[headless] commands: ").append(drawSummary.describe()).append('\n');
        sb.append("[headless] output: ").append(output).append(" (").append(pngBytes).append(" bytes)")
                .append('\n');
        sb.append("[headless] self-check: ").append(selfCheck.summary()).append('\n');
        sb.append("[headless] backdrop: ").append(backdropSummary).append('\n');
        for (String note : selfCheck.notes()) {
            sb.append("[headless]   - ").append(note).append('\n');
        }
        sb.append("[headless] frames: ").append(renderedFrames).append('/').append(request.maxFrames());
        if (renderedFrames >= request.maxFrames()) {
            sb.append("（达到帧上限仍未收敛：检查是否有持续变化的动画/时间源）");
        }
        sb.append('\n');
        if (HeadlessRequest.CHAT_INPUT_PAGE.equals(request.pageId())) {
            // 输入屏形态的读数：像素产物受开合动画相位影响（生产 render 取墙钟），滚动语义只能靠读数钉。
            ChatSceneController controller = ChatHudWindow.ensureRegistered();
            sb.append("[headless] scroll: offset=").append(controller.history().getScroll())
                    .append(" max=").append(controller.history().getMaxScrollOffset())
                    .append(" visible=").append(controller.visibleLineCount()).append('\n');
        }
        if (performanceSummary != null) {
            sb.append("[headless] perf: ").append(performanceSummary).append('\n');
        }
        sb.append("[headless] elapsed: ").append(elapsedMillis).append(" ms");
        return sb.toString();
    }
}
