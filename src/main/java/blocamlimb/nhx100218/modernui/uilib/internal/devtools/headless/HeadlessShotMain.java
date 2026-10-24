package blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * headless 出图命令行出口：一条命令拿 PNG（单张、分辨率矩阵、页面矩阵）。
 *
 * <pre>
 * modernui-shot.bat --page=playground --size=1280x720 --out=out/shot.png
 * modernui-shot.bat --page=playground --sizes=640x360,1280x720,1920x1080,2560x1440 --out=out/shot.png
 * modernui-shot.bat --page=playground --page-indexes=0,1,2,3,4,5,6,7,8 --out=out/page.png
 * modernui-shot.bat --page=playground --actions="move 315 88; frame; click; wait 4"
 * modernui-shot.bat --page=chat --sizes=640x360,1280x720,2560x1440 --out=out/chat.png
 * modernui-shot.bat --page=chat --size=1280x720 --font-scales=100,150,200 --out=out/chat.png
 * modernui-shot.bat --page=chat --themes=liquid-glass-dark,liquid-glass-light --out=out/theme.png
 * modernui-shot.bat --page=hud --size=1920x1080 --debug
 * modernui-shot.bat --page=config --size=1280x720 --out=out/config.png
 * modernui-shot.bat --page=config --page-indexes=0,1,2 --out=out/config-section.png
 * </pre>

 * <p><b>三个批处理轴</b>：{@code --page-indexes} × {@code --font-scales} × {@code --sizes} 按笛卡尔积
 * 出图，多档时 {@code --out} 自动追加轴后缀（见 {@link #resolveOutput}），末尾输出汇总行。</p>
 *
 * <p><b>环境轴与尺寸轴同级</b>：字号倍率是 runtime 侧的环境量（见 {@code HeadlessRequest#fontScalePercent()}），
 * 诊断采样同理 —— 二者都不是「渲染选项」而是本次出图的环境声明，故与尺寸一样可按档位扫。</p>
 *
 * <p>批处理维度：`--sizes` 与 `--page-indexes` 可同时给出，按「页面 × 尺寸」笛卡尔积出图；
 * 多档时 {@code --out} 自动追加 {@code -p<下标>-<W>x<H>} 后缀，末尾输出汇总行。
 * 退出码：0 成功；2 参数错误；3 设施失败（装配 / 帧推进 / 读回 / 编码）；4 像素自检未通过或批量中存在失败档位；
 * 5 运行环境不具备 headless 出图能力（{@link #EXIT_ENVIRONMENT_UNAVAILABLE}）；
 * 6 当前类路径缺少请求所需的类型（{@link #EXIT_CLASSPATH_INSUFFICIENT}，通常是启动器选错）。</p>
 *
 * <p><b>为什么 5 要独立于 3</b>：二者都是「没出成图」，但处置相反 —— 3 说明设施或 UI 坏了，必须查；
 * 5 说明这台机器没能力出图（natives 加载失败 / 无窗口句柄能力 / 上下文建不起来），换环境即可。
 * 混在一起时，无图形环境上的调用方只能「一律报障」或「一律静默跳过」，两种都错。
 * 判据来自 {@link HeadlessFailure#isEnvironmentUnavailable()}，不解析消息文本。</p>
 */
public final class HeadlessShotMain {

    /**
     * 退出码：运行环境不具备 headless 出图能力（不是被测代码的缺陷）。
     *
     * <p>调用方契约：测试据它 {@code Assume} 跳过（见测试域 {@code HeadlessShotGate}），
     * agent 据它提示换 JDK / 加 Xvfb，而不是去查 UI。</p>
     */
    public static final int EXIT_ENVIRONMENT_UNAVAILABLE = 5;

    /**
     * 退出码：当前类路径缺少请求所需的类型（典型：用最小集启动器跑触及 Minecraft 类型的页面）。
     *
     * <p><b>为什么它要独立于 3 与 5</b>（与 {@link #EXIT_ENVIRONMENT_UNAVAILABLE} 独立于 3 同源）：
     * 三种失败的处置互不相同 —— 3 是设施 / UI 坏了，要查代码；5 是这台机器没能力出图，要换环境；
     * 6 是<b>启动器选错了</b>，换机器与查代码都无效，只能换 {@code modernui-shot-full.bat}。混进 3 会让
     * agent 去查一个没坏的 UI；混进 5 会让调用方把「配置错误」当成「环境不具备」跳过。</p>
     *
     * <p>实测触发路径：最小集类路径（{@code modernui-shot.bat}，107 项、不含重编译的 Minecraft 类）下跑
     * {@code --page=chat}，装配期抛 {@code NoClassDefFoundError: net/minecraft/util/IChatComponent}。
     * 该错误此前直接冒泡出 {@code main} ⇒ JVM 退出码 <b>1</b>（不在契约内，脚本与
     * {@code HeadlessShotGate} 无从分流）。</p>
     */
    public static final int EXIT_CLASSPATH_INSUFFICIENT = 6;

    private HeadlessShotMain() {
    }

    /**
     * 进程入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } finally {
            // 出图完成即释放 GL 上下文与承载它的隐藏窗口容器：AWT 的退出钩子要等窗口资源回收，
            // 不显式释放会让 System.exit 停在该钩子里 —— 实测表现为「图已经写出来了，进程却不退出」。
            GlOffscreenSurface.shutdownContext();
        }
        System.exit(code);
    }

    /**
     * 可测试的运行体（不调用 System.exit）：**保证任何出口都落在退出码契约内**。
     *
     * <p>它只做一件事 —— 兜住 {@link #runRequest} 未捕获的错误。正常路径上 {@link HeadlessFailure}
     * 已在各出口自行处理，故第一个 catch 是防御性的；真正起作用的是第二个：未捕获的 {@code Error}
     * （类路径缺件时的 {@code NoClassDefFoundError} 等）此前直接冒泡出 {@code main}，JVM 以
     * <b>退出码 1</b> 收场 —— 不在契约内，脚本与 {@code HeadlessShotGate} 无从分流。</p>
     *
     * <p>兜底落点单一是刻意的：分散到各出口（probe / 查询 / 单档 / 隔离）必然漂移，同
     * {@link #exitCodeOf} 的理由。</p>
     *
     * @param args 命令行参数
     * @return 退出码（0 / 2 / 3 / 4 / 5 / 6）
     */
    static int run(String[] args) {
        try {
            return runRequest(args);
        } catch (HeadlessFailure failure) {
            failure.printDiagnosis(System.err);
            return exitCodeOf(failure);
        } catch (Throwable unexpected) {
            return diagnoseUnexpected(unexpected);
        }
    }

    /**
     * 请求解析与执行主体（不调用 System.exit）。
     *
     * @param args 命令行参数
     * @return 退出码
     */
    private static int runRequest(String[] args) {
        String page = "playground";
        String pages = null;
        String sizes = null;
        String pageIndexesArg = null;
        int pageIndex = -1;
        int width = 1280;
        int height = 720;
        int frames = 2;
        int settle = 2;
        int maxFrames = 60;
        int background = HeadlessRequest.DEFAULT_BACKGROUND;
        String text = HeadlessRequest.DEFAULT_PROBE_TEXT;
        String script = "";
        String out = null;
        long clockMillis = HeadlessRequest.DEFAULT_CLOCK_MILLIS;
        int fontScalePercent = SceneRuntime.FONT_SCALE_NONE_PERCENT;
        String fontScales = null;
        String theme = null;
        String themes = null;
        String backdropQuality = null;
        String backdropQualities = null;
        boolean diagnostics = false;
        boolean shareContext = false;
        boolean probeOnly = false;
        // 目标寻址：--nodes[=all] 打印树投影；--find=TEXT 按可见文本找可命中节点；--center=PATH 解地址取中心点。
        boolean nodes = false;
        boolean nodesAll = false;
        String find = null;
        String center = null;
        boolean framesGiven = false;
        try {
            for (String arg : args) {
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsage(System.out);
                    return 0;
                } else if ("--probe".equals(arg)) {
                    probeOnly = true;
                } else if ("--nodes".equals(arg)) {
                    nodes = true;
                } else if ("--nodes=all".equals(arg)) {
                    nodes = true;
                    nodesAll = true;
                } else if (arg.startsWith("--find=")) {
                    find = arg.substring("--find=".length());
                } else if (arg.startsWith("--center=")) {
                    center = arg.substring("--center=".length());
                } else if (arg.startsWith("--pages=")) {
                    pages = arg.substring("--pages=".length());
                } else if (arg.startsWith("--page=")) {
                    page = arg.substring("--page=".length());
                } else if (arg.startsWith("--page-indexes=")) {
                    pageIndexesArg = arg.substring("--page-indexes=".length());
                } else if (arg.startsWith("--page-index=")) {
                    pageIndex = Integer.parseInt(arg.substring("--page-index=".length()));
                } else if (arg.startsWith("--out=")) {
                    out = arg.substring("--out=".length());
                } else if (arg.startsWith("--bg=")) {
                    background = parseBackground(arg.substring("--bg=".length()));
                } else if (arg.startsWith("--text=")) {
                    text = arg.substring("--text=".length());
                } else if (arg.startsWith("--actions=")) {
                    script = arg.substring("--actions=".length());
                } else if (arg.startsWith("--script=")) {
                    script = readScriptFile(arg.substring("--script=".length()));
                } else if (arg.startsWith("--frames=")) {
                    frames = Integer.parseInt(arg.substring("--frames=".length()));
                    framesGiven = true;
                } else if (arg.startsWith("--settle=")) {
                    settle = Integer.parseInt(arg.substring("--settle=".length()));
                } else if (arg.startsWith("--max-frames=")) {
                    maxFrames = Integer.parseInt(arg.substring("--max-frames=".length()));
                } else if (arg.startsWith("--sizes=")) {
                    sizes = arg.substring("--sizes=".length());
                } else if (arg.startsWith("--size=")) {
                    int[] parsed = parseSize(arg.substring("--size=".length()));
                    width = parsed[0];
                    height = parsed[1];
                } else if (arg.startsWith("--themes=")) {
                    themes = arg.substring("--themes=".length());
                } else if (arg.startsWith("--theme=")) {
                    theme = arg.substring("--theme=".length());
                } else if (arg.startsWith("--backdrop-qualities=")) {
                    backdropQualities = arg.substring("--backdrop-qualities=".length());
                } else if (arg.startsWith("--backdrop-quality=")) {
                    backdropQuality = arg.substring("--backdrop-quality=".length());
                } else if (arg.startsWith("--font-scales=")) {
                    fontScales = arg.substring("--font-scales=".length());
                } else if (arg.startsWith("--font-scale=")) {
                    fontScalePercent = Integer.parseInt(arg.substring("--font-scale=".length()));
                } else if ("--debug".equals(arg)) {
                    diagnostics = true;
                } else if ("--share-context".equals(arg)) {
                    shareContext = true;
                } else if (arg.startsWith("--clock=")) {
                    clockMillis = Long.parseLong(arg.substring("--clock=".length()));
                } else {
                    System.err.println("[headless] 未知参数：" + arg);
                    printUsage(System.err);
                    return 2;
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[headless] 参数解析失败：" + e.getMessage());
            printUsage(System.err);
            return 2;
        }


        List<String> pageNameTargets = new ArrayList<String>();
        List<Integer> pageTargets = new ArrayList<Integer>();
        List<int[]> sizeTargets = new ArrayList<int[]>();
        List<Integer> fontScaleTargets = new ArrayList<Integer>();
        List<String> themeTargets = new ArrayList<String>();
        List<String> backdropQualityTargets = new ArrayList<String>();
        try {
            // 页面轴：--page 是单页面（缺省），--pages 给出多页面矩阵；两者同时给出时 --pages 胜。
            if (pages == null || pages.trim().isEmpty()) {
                pageNameTargets.add(page);
            } else {
                for (String part : pages.split(",")) {
                    if (!part.trim().isEmpty()) {
                        pageNameTargets.add(part.trim());
                    }
                }
                if (pageNameTargets.isEmpty()) {
                    pageNameTargets.add(page);
                }
            }
            if (pageIndexesArg == null || pageIndexesArg.trim().isEmpty()) {
                pageTargets.add(Integer.valueOf(pageIndex));
            } else {
                for (String part : pageIndexesArg.split(",")) {
                    pageTargets.add(Integer.valueOf(part.trim()));
                }
            }
            if (sizes == null || sizes.trim().isEmpty()) {
                sizeTargets.add(new int[] {width, height});
            } else {
                for (String part : sizes.split(",")) {
                    sizeTargets.add(parseSize(part.trim()));
                }
            }
            if (fontScales == null || fontScales.trim().isEmpty()) {
                fontScaleTargets.add(Integer.valueOf(fontScalePercent));
            } else {
                for (String part : fontScales.split(",")) {
                    fontScaleTargets.add(Integer.valueOf(part.trim()));
                }
            }
            // 主题档允许 null（= 不干预，各页面用自己生产装配的默认），故用「空串即缺省」表达。
            if (themes == null || themes.trim().isEmpty()) {
                themeTargets.add(theme);
            } else {
                for (String part : themes.split(",")) {
                    String name = part.trim();
                    themeTargets.add(name.isEmpty() ? null : name);
                }
            }
            // 玻璃档位同样允许 null（= 不干预，维持进程当前档位）。
            if (backdropQualities == null || backdropQualities.trim().isEmpty()) {
                backdropQualityTargets.add(backdropQuality);
            } else {
                for (String part : backdropQualities.split(",")) {
                    String name = part.trim();
                    backdropQualityTargets.add(name.isEmpty() ? null : name);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[headless] 批量参数非法：" + e.getMessage());
            return 2;
        }
        // config 页不接收外观档（主题对它是**配置内容**：配置页在页壳树构建前安装自己的偏好信号）。
        // 命令层给一条提示而不是静默忽略——「跑了没变化」与「参数没接线」在产物上不可区分，
        // 而本仓的通例是「不许静默降级」。
        boolean themeGiven = false;
        for (String targetTheme : themeTargets) {
            if (targetTheme != null) {
                themeGiven = true;
            }
        }
        if (themeGiven && pageNameTargets.contains(HeadlessRequest.CONFIG_PAGE)) {
            System.out.println("[headless] 提示：config 页不接收 --theme —— 主题对配置页是配置内容"
                    + "（页壳树构建前安装自己的偏好信号），不是请求级环境量；该参数对 config 档无效");
        }
        if (themeGiven && pageNameTargets.contains(HeadlessRequest.CHAT_INPUT_PAGE)) {
            // 与 config 页同款「收下不接线」风险：输入屏在自己的构造里建树，页内没有主题安装时机
            // （独立审查实测四档主题产物逐字节相同）。显式提示而不是静默忽略 —— 本仓通例。
            System.out.println("[headless] 提示：chat-input 页不接收 --theme —— 输入屏在构造期建树、"
                    + "页内无主题安装时机（SceneThemes 必须在建树前安装）；该参数对 chat-input 档无效");
        }

        int total = pageNameTargets.size() * pageTargets.size() * sizeTargets.size()
                * fontScaleTargets.size() * themeTargets.size() * backdropQualityTargets.size();
        boolean multi = total > 1;
        // 页面段进产物后缀只在多页面时：单页面时页面名已在默认文件名前缀里，再加一段是冗余；
        // 而多页面共用同一个 --out 时必须能区分（见 resolveOutput）。
        boolean multiPage = pageNameTargets.size() > 1;
        // 轴展开 → 请求列表：档名/字号域等校验与产物命名在这里一次收口；后面的「隔离调度」与
        // 「同进程渲染」消费同一份列表，不各自再推一遍参数（推两遍必然漂移）。
        List<HeadlessRequest> requests = new ArrayList<HeadlessRequest>();
        try {
            for (String pageName : pageNameTargets) {
                // 页面相关的默认值按页算（聊天系的演示消息集与最小帧数不能串给别的页面）。
                String pageText = defaultTextFor(pageName, text);
                int pageFrames = defaultFramesFor(pageName, frames, framesGiven);
                for (Integer targetPageIndex : pageTargets) {
                    for (String targetTheme : themeTargets) {
                        for (String targetBackdropQuality : backdropQualityTargets) {
                            for (Integer targetFontScale : fontScaleTargets) {
                                for (int[] size : sizeTargets) {
                                    int scalePercent = targetFontScale.intValue();
                                    List<String> suffixParts = new ArrayList<String>();
                                    if (multiPage) {
                                        suffixParts.add("-pg" + pageName);
                                    }
                                    if (targetPageIndex.intValue() >= 0) {
                                        suffixParts.add("-p" + targetPageIndex);
                                    }
                                    if (targetTheme != null) {
                                        suffixParts.add("-th" + targetTheme);
                                    }
                                    if (targetBackdropQuality != null) {
                                        suffixParts.add("-bq" + targetBackdropQuality);
                                    }
                                    if (scalePercent != SceneRuntime.FONT_SCALE_NONE_PERCENT) {
                                        suffixParts.add("-fs" + scalePercent);
                                    }
                                    suffixParts.add("-" + size[0] + "x" + size[1]);
                                    Path output = resolveOutput(out, pageName, suffixParts, multi);
                                    requests.add(HeadlessRequest.builder().page(pageName)
                                            .pageIndex(targetPageIndex.intValue())
                                            .size(size[0], size[1]).frames(pageFrames)
                                            .background(background).text(pageText)
                                            .script(script).settle(settle).maxFrames(maxFrames)
                                            .clock(clockMillis).fontScale(scalePercent)
                                            .diagnostics(diagnostics).theme(targetTheme)
                                            .backdropQuality(targetBackdropQuality)
                                            .output(output).build());
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[headless] 请求非法：" + e.getMessage());
            return 2;
        }

        if (probeOnly) {
            try (HeadlessSession session = HeadlessSession.open(requests.get(0))) {
                System.out.println("[headless] capabilities: " + session.capabilities().summary());
            } catch (HeadlessFailure failure) {
                failure.printDiagnosis(System.err);
                return exitCodeOf(failure);
            }
            return 0;
        }

        // 目标寻址模式：先推进一帧（布局结果要等帧管线写回 cachedLayout），再投影 / 查询。
        // 多档轴同时给出时逐档报告（与出图矩阵同语义）—— 此前静默只处理第一档，会让
        // 「--sizes=A,B --nodes」看起来只出了 A 档而无任何提示（独立复核指出）。
        // 某一档失败不中断其余档：矩阵验收要的是「哪几档坏了」的全貌，早退会把后面的档藏起来
        // （独立复核指出）。退出码按最严聚合：任一处设施失败(3) > 有档无结果(4) > 全成功(0)。
        if (nodes || find != null || center != null) {
            int queryFailures = 0;
            boolean facilityFailed = false;
            boolean environmentMissing = false;
            for (HeadlessRequest request : requests) {
                if (requests.size() > 1) {
                    System.out.println("[headless] --- " + labelOf(request) + " ---");
                }
                int code = reportTargets(request, nodes, nodesAll, find, center);
                if (code == 3) {
                    facilityFailed = true;
                } else if (code == EXIT_ENVIRONMENT_UNAVAILABLE) {
                    environmentMissing = true;
                } else if (code != 0) {
                    queryFailures++;
                }
            }
            if (facilityFailed) {
                return 3;
            }
            if (queryFailures > 0) {
                return 4;
            }
            // 环境不具备时连帧都推不动，报 5 才不会把「没查」说成「没查到」。
            return environmentMissing ? EXIT_ENVIRONMENT_UNAVAILABLE : 0;
        }

        // 多档默认逐档独立进程：见 runIsolated 的性能与正确性权衡说明。
        if (multi && !shareContext) {
            return runIsolated(requests);
        }

        int okCount = 0;
        int facilityFailures = 0;
        int contentFailures = 0;
        int environmentMissing = 0;
        List<String> labels = new ArrayList<String>();
        for (HeadlessRequest request : requests) {
            boolean ok;
            boolean environmentUnavailable = false;
            try (HeadlessSession session = HeadlessSession.open(request)) {
                HeadlessArtifact artifact = session.capture();
                System.out.println(artifact.describe());
                ok = artifact.selfCheck().ok();
            } catch (HeadlessFailure failure) {
                failure.printDiagnosis(System.err);
                // 环境 / 上下文 / 装配 / 帧 / 读回 / 编码失败属于「设施没能出图」，与「图出来了但内容可疑」分开报，
                // 否则 agent 无法按退出码区分「环境没准备好」和「UI 有问题」；「运行环境不具备」再单列一档，
                // 因为它的处置不是查设施而是换环境（见 EXIT_ENVIRONMENT_UNAVAILABLE）。
                environmentUnavailable = failure.isEnvironmentUnavailable();
                if (environmentUnavailable) {
                    environmentMissing++;
                } else {
                    facilityFailures++;
                }
                ok = false;
            }
            if (ok) {
                okCount++;
            } else if (!environmentUnavailable) {
                contentFailures++;
            }
            labels.add(labelOf(request) + "=" + (ok ? "ok" : "FAILED"));
        }

        if (multi) {
            System.out.println("[headless] batch: " + okCount + "/" + total + " ok (同进程) — "
                    + String.join(" ", labels));
        }
        if (facilityFailures > 0) {
            return 3;
        }
        if (contentFailures > 0) {
            return 4;
        }
        return environmentMissing > 0 ? EXIT_ENVIRONMENT_UNAVAILABLE : 0;
    }

    /**
     * 逐档独立进程出图：多档默认走这里。
     *
     * <h3>为什么默认隔离（实测依据）</h3>
     * <p>字体 atlas 是<b>进程级按需资源</b>：字形落在 atlas 的哪个位置取决于「此前生成过哪些字形」。
     * 于是同一 JVM 内渲染第 N 档时，atlas 里已有前面各档的字形 ⇒ 本档字形的 UV 与冷启动不同 ⇒
     * 边缘双线性采样出现 ±1~±5 的微差。实测：批量 {@code [640x360,2560x1440]} 的 2560 档与单跑差
     * 31534/3686400 像素，而把前置档从 640 换成 1280 又得到第三个结果（31724 差异）——
     * <b>产物取决于它在进程内的渲染次序，而不是只取决于请求</b>。</p>
     *
     * <p>这与设施不变量 3（「不读全局单例的隐藏状态」，产物是请求的函数）直接冲突，而「改动一行 →
     * 出图对拍」正是本设施的主用途：批量下把 atlas 微差当成代码改动的影响会直接误判。故默认隔离，
     * 每档从冷状态起算。代价是每档 +1.6 s 左右的 JVM 启动（7 档矩阵 2.4 s → ~12 s）。</p>
     *
     * <p>{@code --share-context} 可换回同进程复用（快，但产物带上述历史依赖），仅建议用于扫观感。</p>
     *
     * @param requests 已展开并校验的请求列表
     * @return 退出码（0 全绿 / 3 有档位设施失败 / 4 有档位自检未过 / 5 运行环境不具备 /
     *         6 类路径缺件 —— 6 就地终止并原样上报，不参与聚合）
     */
    private static int runIsolated(List<HeadlessRequest> requests) {
        int facilityFailures = 0;
        int contentFailures = 0;
        int environmentMissing = 0;
        List<String> labels = new ArrayList<String>();
        for (HeadlessRequest request : requests) {
            int code = spawnIsolated(argsOf(request));
            if (code == 0) {
                labels.add(labelOf(request) + "=ok");
                continue;
            }
            labels.add(labelOf(request) + "=FAILED");
            // 子进程退出码即契约（见类注释）：3 设施失败 / 4 自检未过 / 5 运行环境不具备 /
            // 6 类路径缺件。6 必须在这里就地终止并原样上报：它是请求级不可恢复的事实
            // （换档不会变好），落进下面的 else 会被算成「内容可疑(4)」，把配置错误伪装成 UI 问题。
            if (code == EXIT_CLASSPATH_INSUFFICIENT) {
                System.out.println("[headless] batch: 因类路径缺件终止，后续档未执行（"
                        + labelOf(request) + "）");
                return EXIT_CLASSPATH_INSUFFICIENT;
            }
            if (code == EXIT_ENVIRONMENT_UNAVAILABLE) {
                environmentMissing++;
            } else if (code == 3) {
                facilityFailures++;
            } else {
                contentFailures++;
            }
        }
        System.out.println("[headless] batch: " + (requests.size() - facilityFailures - contentFailures
                - environmentMissing) + "/" + requests.size() + " ok (逐档独立进程) — "
                + String.join(" ", labels));
        if (facilityFailures > 0) {
            return 3;
        }
        if (contentFailures > 0) {
            return 4;
        }
        return environmentMissing > 0 ? EXIT_ENVIRONMENT_UNAVAILABLE : 0;
    }

    /**
     * 请求 → 子进程命令行参数（单档等价形式）。
     *
     * <p>从请求反推而非从原始参数转发：请求是校验后的单一事实源，且这份参数会被子进程再解析一次，
     * 必须与主进程解析出的请求<b>逐字段等价</b>（含 chat/hud 页的默认文本与帧数替换 —— 那些替换
     * 作用在已替换值上是幂等的）。</p>
     *
     * @param request 请求
     * @return 参数列表（不含 java 与主类）
     */
    private static List<String> argsOf(HeadlessRequest request) {
        List<String> args = new ArrayList<String>();
        args.add("--page=" + request.pageId());
        if (request.pageIndex() >= 0) {
            args.add("--page-index=" + request.pageIndex());
        }
        args.add("--size=" + request.width() + "x" + request.height());
        args.add("--frames=" + request.frames());
        args.add("--settle=" + request.settleFrames());
        args.add("--max-frames=" + request.maxFrames());
        int background = request.background();
        args.add("--bg=" + ((background >>> 24) == 0 ? "transparent"
                : String.format("%06X", Integer.valueOf(background & 0xFFFFFF))));
        args.add("--text=" + request.text());
        if (!request.script().isEmpty()) {
            args.add("--actions=" + request.script());
        }
        args.add("--clock=" + request.clockMillis());
        args.add("--font-scale=" + request.fontScalePercent());
        // 子进程是独立进程，其环境端口由 --debug 经请求重建；此处是「请求 → 子进程参数」的投影，
        // 读请求是正确口径（会话内的诊断判断走 environment.diagnostics()，见 HeadlessSession）。
        if (request.diagnostics()) {
            args.add("--debug");
        }
        if (request.theme() != null) {
            args.add("--theme=" + request.theme());
        }
        if (request.backdropQuality() != null) {
            args.add("--backdrop-quality=" + request.backdropQuality());
        }
        args.add("--out=" + request.output());
        return args;
    }

    /**
     * 启动一个只出一档的子进程并等它结束（输出直通，便于逐档读诊断）。
     *
     * @param requestArgs 单档参数
     * @return 子进程退出码；启动失败按「没能出图」（3）计
     */
    private static int spawnIsolated(List<String> requestArgs) {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        String classpath = System.getProperty("java.class.path");
        if (classpath != null && !classpath.isEmpty()) {
            command.add("-cp");
            command.add(classpath);
        }
        String libraryPath = System.getProperty("java.library.path");
        if (libraryPath != null && !libraryPath.isEmpty()) {
            command.add("-Djava.library.path=" + libraryPath);
        }
        command.add(HeadlessShotMain.class.getName());
        command.addAll(requestArgs);
        try {
            return new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (IOException e) {
            System.err.println("[headless] 隔离档启动失败：" + e.getMessage());
            return 3;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 3;
        }
    }

    /**
     * 页面的默认文本：聊天系页面（chat / chat-input / hud）未显式给 {@code --text} 时用演示消息集
     * （省得每次出图都拼长参数串）。
     *
     * <p>按页计算而非全局替换：多页面轴下一次调用会同时出多个页面，全局替换会把聊天页的演示消息集
     * 串给 playground。</p>
     *
     * @param page 页面标识
     * @param text 命令行给出的文本
     * @return 该页实际使用的文本
     */
    private static String defaultTextFor(String page, String text) {
        if (HeadlessRequest.CHAT_PAGE.equals(page) || HeadlessRequest.HUD_PAGE.equals(page)
                || HeadlessRequest.CHAT_INPUT_PAGE.equals(page)) {
            return HeadlessRequest.DEFAULT_PROBE_TEXT.equals(text) ? HeadlessRequest.CHAT_DEFAULT_TEXT : text;
        }
        return text;
    }

    /**
     * 页面的默认帧数：聊天系页面与配置页未显式给 {@code --frames} 时提到 20 ——
     * 消息组首次合成有 180 ms 入场动画（16 ms/帧 → 12 帧），动画期间整树 opacity=0 且像素逐帧不变，
     * 稳定判据会把这段误判成「已收敛」而提前停帧出空图；配置页同理——它的标题与字段 presentation
     * shell 在完整布局发布后有 opacity 级联进入。
     *
     * @param page 页面标识
     * @param frames 命令行给出的最小帧数
     * @param framesGiven 命令行是否显式给过 {@code --frames}
     * @return 该页实际使用的最小帧数
     */
    private static int defaultFramesFor(String page, int frames, boolean framesGiven) {
        if (!framesGiven && (HeadlessRequest.CHAT_PAGE.equals(page) || HeadlessRequest.HUD_PAGE.equals(page)
                || HeadlessRequest.CHAT_INPUT_PAGE.equals(page) || HeadlessRequest.CONFIG_PAGE.equals(page))) {
            return 20;
        }
        return frames;
    }

    /** @return 当前 JVM 的 java 可执行文件路径（子进程与父进程同 JDK、同 natives） */
    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win")
                ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", executable).toString();
    }

    /**
     * 汇总行标签：从请求派生（页 + 尺寸 + 偏离缺省的环境维度）。
     *
     * <p>从请求而不是从原始参数派生：隔离路径与同进程路径共用它，参数一旦在这里再推一遍就会漂移。</p>
     *
     * @param request 请求
     * @return 标签
     */
    private static String labelOf(HeadlessRequest request) {
        return (request.pageIndex() >= 0 ? request.pageId() + "#" + request.pageIndex() : request.pageId())
                + "@" + request.width() + "x" + request.height()
                + (request.fontScalePercent() == SceneRuntime.FONT_SCALE_NONE_PERCENT ? ""
                        : " fs" + request.fontScalePercent() + "%")
                + (request.theme() == null ? "" : " theme=" + request.theme())
                + (request.backdropQuality() == null ? "" : " bq=" + request.backdropQuality());
    }

    /**
     * 解析 {@code WxH} 尺寸。
     *
     * @param value 尺寸文本
     * @return {宽, 高}
     */
    private static int[] parseSize(String value) {
        String[] parts = value.split("[xX]");
        if (parts.length != 2) {
            throw new IllegalArgumentException("尺寸需要 WxH 形式：" + value);
        }
        return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    /**
     * 解析输出路径：批量时在文件名里插入各轴后缀，避免互相覆盖。
     *
     * <p>后缀的组成规则是「<b>偏离缺省取值的维度</b> + 目标尺寸」：尺寸恒进（同一页面的不同尺寸是
     * 不同产物，没有「缺省尺寸」可言），页下标在指定时进，字号倍率只在非缺省水位时进 —— 于是既有
     * 命令的产物路径逐字不变，新增维度也不会与既有命名撞车。</p>
     *
     * @param out         命令行给出的输出路径；null 表示用默认路径
     * @param page        页面标识（默认路径的文件名前缀）
     * @param suffixParts 后缀段（调用方按「哪些维度偏离缺省」拼好；尺寸段恒在其中）
     * @param multi       是否批量
     * @return 目标路径
     */
    private static Path resolveOutput(String out, String page, List<String> suffixParts, boolean multi) {
        String suffix = String.join("", suffixParts);
        if (out == null) {
            return Paths.get("build", "reports", "headless", page + suffix + ".png");
        }
        Path base = Paths.get(out);
        if (!multi) {
            return base;
        }
        String name = base.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        String renamed = stem + suffix + extension;
        return base.getParent() == null ? Paths.get(renamed) : base.getParent().resolve(renamed);
    }

    /**
     * 解析宿主背景：{@code RRGGBB} / {@code #RRGGBB} / {@code transparent}。
     *
     * @param value 参数值
     * @return ARGB 颜色
     */
    private static int parseBackground(String value) {
        if ("transparent".equalsIgnoreCase(value)) {
            return 0x00000000;
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            throw new IllegalArgumentException("--bg 需要 RRGGBB 或 transparent：" + value);
        }
        return 0xFF000000 | (int) Long.parseLong(hex, 16);
    }

    /**
     * 读取脚本文件（IO 失败转成参数错误，交由参数解析路径统一处理）。
     *
     * @param path 文件路径
     * @return 文件文本
     */
    private static String readScriptFile(String path) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("脚本文件读取失败：" + path + "（" + e.getMessage() + "）");
        }
    }

    /**
     * 目标寻址：投影当前树的节点事实，或按文本 / 地址查询。
     *
     * <p>先推进一帧再查询：布局结果由帧管线写回节点的 {@code cachedLayout}，未布局的树坐标全 0。</p>
     *
     * @param request       请求（决定页面 / 尺寸 / 环境）
     * @param nodes         是否打印树投影
     * @param nodesAll      true = 打印完整树（含不可命中的容器与装饰叶）
     * @param find          按可见文本匹配的片段；null = 不查
     * @param center        要解析的节点地址；null = 不查
     * @return 退出码（0 成功 / 3 设施失败 / 4 查询无结果 / 5 运行环境不具备）
     */
    private static int reportTargets(HeadlessRequest request, boolean nodes, boolean nodesAll,
            String find, String center) {
        try (HeadlessSession session = HeadlessSession.open(request)) {
            // 推进一帧只为拿到布局结果，不写 PNG：寻址是查询，不该有产物副作用。
            session.advanceFramesForQuery();
            List<HeadlessTreeProjection.Row> rows = session.projectTree(!nodesAll);
            System.out.println("[headless] targets: roots=" + session.rootCount()
                    + " rows=" + rows.size()
                    + (nodesAll ? " (完整树)" : " (仅可命中节点)"));
            if (nodes) {
                for (HeadlessTreeProjection.Row row : rows) {
                    System.out.println("[headless]   " + row.describe());
                }
            }
            if (find != null) {
                List<HeadlessTreeProjection.Row> matched = session.findByText(find);
                // 按「可点目标优先」排序：命中里常有容器与遮罩（它们从子树聚合到同一段文案），
                // 直接按 DFS 顺序输出会让 agent 点中整屏遮罩。排序在同一质量档内保持 DFS 序（稳定）。
                List<HeadlessTreeProjection.Row> ordered = new ArrayList<HeadlessTreeProjection.Row>();
                for (HeadlessTreeProjection.Row row : matched) {
                    if (row.actionableTarget()) {
                        ordered.add(row);
                    }
                }
                for (HeadlessTreeProjection.Row row : matched) {
                    if (!row.actionableTarget()) {
                        ordered.add(row);
                    }
                }
                int actionable = countActionable(matched);
                System.out.println("[headless] find \"" + find + "\": " + matched.size()
                        + " 个命中（其中可点目标 " + actionable + " 个）");
                for (HeadlessTreeProjection.Row row : ordered) {
                    System.out.println("[headless]   " + row.describe()
                            + " center=" + row.centerX() + "," + row.centerY());
                }
                if (actionable == 0 && !ordered.isEmpty()) {
                    // 提示必须说清「为什么点不到」，而不是笼统一句「多半落在遮罩上」：
                    // [blocked] 的成因至少三种（被裁掉 / 被别的东西接走 / 命中的是自己的后代），
                    // agent 据此采取的动作完全不同。逐行已给出 hit=…，这里只汇总一句。
                    System.out.println("[headless]   提示：没有中心点可命中的目标。"
                            + "[container] 的中心落到了自己的后代（点下去事件仍冒泡经过它）；"
                            + "[blocked] 见各行 hit=：hit=NONE 表示该坐标没有任何节点接住（多半被滚动容器裁掉），"
                            + "hit=<地址> 表示被该节点接走（浮层遮罩、更深的兄弟节点），可先处理它");
                }
                if (matched.isEmpty()) {
                    return 4;
                }
            }
            if (center != null) {
                int[] point = session.centerOf(center);
                System.out.println("[headless] center " + center + " = " + point[0] + "," + point[1]);
            }
            return 0;
        } catch (HeadlessFailure failure) {
            failure.printDiagnosis(System.err);
            return exitCodeOf(failure);
        }
    }

    /**
     * 失败 → 退出码：运行环境不具备 5，其余设施失败 3。
     *
     * <p>映射只此一处：三个登出点（{@code --probe} / 目标寻址 / 单档出图）各判一次必然漂移。</p>
     *
     * @param failure 捕获到的失败
     * @return {@link #EXIT_ENVIRONMENT_UNAVAILABLE} 或 3
     */
    private static int exitCodeOf(HeadlessFailure failure) {
        // 刻意不在这里复用 missingTypeOf：HeadlessFailure 是「设施语义」的失败（带阶段标签），
        // 目前没有任何构造点把类路径缺件包进来 —— chat 路径上 HeadlessSession.open 的
        // catch (RuntimeException | Error) 是原样 rethrow，缺件会冒泡到进程边界归类为 6（实测）。
        // 若将来新增出口把 NoClassDefFoundError 包进 HeadlessFailure，此处必须同步判 missingTypeOf，
        // 否则同一条缺件会静默降级成 3。
        return failure.isEnvironmentUnavailable() ? EXIT_ENVIRONMENT_UNAVAILABLE : 3;
    }

    /**
     * 未捕获错误 → 退出码与诊断：缺类型报 {@link #EXIT_CLASSPATH_INSUFFICIENT}，其余报设施失败 3。
     *
     * <p>判据是<b>类型 + 报错形态</b>（沿 cause 链找 {@code NoClassDefFoundError} /
     * {@code ClassNotFoundException}，并排除 {@code Could not initialize class …} 这一 erroneous
     * 类形态，见 {@link #missingTypeOf}）—— 与 {@code isEnvironmentUnavailable()} 同一取向：
     * 让「这是配置事实」由可判定的结构承载，而不是靠人读日志。<b>类型本身不足以证明缺件</b>：
     * {@code NoClassDefFoundError} 也用于「类在但静态初始化失败」，两者处置相反。</p>
     *
     * @param unexpected 未捕获的错误
     * @return 6（类路径缺件）或 3（其余未预期失败）
     */
    static int diagnoseUnexpected(Throwable unexpected) {
        String missingType = missingTypeOf(unexpected);
        if (missingType != null) {
            System.err.println("[headless] CLASSPATH-INSUFFICIENT：当前类路径缺少类型 " + missingType);
            System.err.println("[headless] 处置：若该页面触及 Minecraft 类型（chat / chat-input / hud），"
                    + "换用完整开发类路径启动器 build\\headless\\modernui-shot-full.bat（Linux/macOS 为 .sh）；"
                    + "否则检查 exportHeadlessClasspath 的导出是否完整");
            System.err.println("[headless] cause: " + unexpected.getClass().getName() + ": "
                    + HeadlessFailure.brief(unexpected));
            // 这里也打完整栈：判据含「报错消息形态」，一旦漏网（消息退化、包装层改变），没有栈就只剩
            // 一行伪类型名，无从定位。独立审核实测过两种退化：无消息的 NoClassDefFoundError 会打印出
            // 「缺少类型 NoClassDefFoundError」，带后缀的消息会打印出含括号的伪类型名。
            unexpected.printStackTrace(System.err);
            return EXIT_CLASSPATH_INSUFFICIENT;
        }
        System.err.println("[headless] UNEXPECTED-ERROR：未捕获的 " + unexpected.getClass().getName()
                + ": " + HeadlessFailure.brief(unexpected));
        unexpected.printStackTrace(System.err);
        return 3;
    }

    /**
     * JVM 对「类在、但 {@code <clinit>} 已失败」的 erroneous 类所报 {@code NoClassDefFoundError} 的前缀。
     */
    private static final String ERRONEOUS_CLASS_PREFIX = "Could not initialize class ";

    /**
     * 沿 cause 链找「类路径缺件」的根因。
     *
     * <p><b>为什么必须排除 {@code Could not initialize class …}</b>：{@code NoClassDefFoundError}
     * 有两种成因，处置相反 —— 一是类<b>不在</b>类路径上（换启动器有效）；二是类在、但静态初始化
     * 已经失败过（JVM 把该类型标成 erroneous，此后任何触碰都抛这个错，消息形如
     * {@code Could not initialize class X}）。后者换启动器无效，属环境 / 初始化缺陷，必须落到 3
     * 并带完整栈。<b>形态本身不可靠</b>正是排除的充分理由：同一个异常类型既可能是缺件、也可能不是，
     * 判据不能只看类型。仓内同形态的记载见 {@code GlOffscreenSurface}（CI 的 Zulu 下 {@code Sys.<clinit>}
     * 失败后 {@code Display} 成为 erroneous 类）—— 该路径在那里已被 {@code catch (Throwable)} 收口成 5，
     * 故不作为「可达性」证据，只说明这种形态在本仓确实出现过。</p>
     *
     * @param failure 未捕获的错误
     * @return 缺失类型名（点分形式）；不是类路径缺件时为 null
     */
    static String missingTypeOf(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof NoClassDefFoundError) {
                String message = current.getMessage();
                if (message == null || !message.startsWith(ERRONEOUS_CLASS_PREFIX)) {
                    return message == null ? current.getClass().getSimpleName() : message.replace('/', '.');
                }
                // erroneous 类：跳过这一层继续沿链找更深的真实缺件；找不到就照 3 报（带完整栈）。
            } else if (current instanceof ClassNotFoundException) {
                String message = current.getMessage();
                return message == null ? current.getClass().getSimpleName() : message.replace('/', '.');
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * @return 命中的可点目标数（可命中、有可见尺寸、且<b>中心点命中自己</b>）。
     *
     * <p>判据即 {@link HeadlessTreeProjection.Row#actionableTarget()} —— 事实来自真实命中链，
     * 不是几何推断。此处只做计数，不重复判据。</p>
     */
    private static int countActionable(List<HeadlessTreeProjection.Row> rows) {
        int count = 0;
        for (HeadlessTreeProjection.Row row : rows) {
            if (row.actionableTarget()) {
                count++;
            }
        }
        return count;
    }

    private static void printUsage(PrintStream out) {
        out.println("用法: HeadlessShotMain [--page=playground|text-probe|chat|chat-input|hud|config|glass|picker |"
                + " --pages=NAME,NAME,…] [--page-index=N | --page-indexes=N,N,…]"
                + " [--size=WxH | --sizes=WxH,WxH,…] [--out=path] [--frames=N] [--settle=N] [--max-frames=N]"
                + " [--bg=RRGGBB|transparent] [--text=…] [--actions=\"…\"|--script=file]"
                + " [--clock=epochMillis]"
                + " [--theme=NAME | --themes=NAME,…] [--font-scale=P | --font-scales=P,P,…]"
                + " [--backdrop-quality=full|eco | --backdrop-qualities=full,eco] [--debug]"
                + " [--share-context] [--probe]"
                + " [--nodes[=all]] [--find=TEXT] [--center=PATH]");
        out.println("批量默认逐档独立进程（产物只依赖请求）；--share-context 同进程复用（快，但产物带 atlas 历史依赖）");
        out.println("主题档: " + HeadlessThemes.names() + "（不给 = 各页面用自己的默认外观）");
        out.println("目标寻址: --nodes 打印可命中节点（--nodes=all 打印完整树）；--find=TEXT 按可见文本找节点"
                + "（给出地址与中心点）；--center=r0/3/1 解地址取中心点。三者都先推进一帧拿布局，不产出 PNG");
        out.println("页面: playground / text-probe / chat / chat-input / hud / config / glass / picker；--pages 给多页面矩阵，"
                + "--page-index 的含义随页面而变（playground = 演示页下标，hud = 锚点，config = section 下标，"
                + "picker = 演示状态 0 全部/1 过滤/2 空态，chat/text-probe/glass 忽略）");
        out.println("glass 页：磨玻璃实验室（backdrop-filter 观感验收）；诊断卡上的「backdrop 路径」是本帧实际走的"
                + "渲染路径（shader / fixed-pipeline / tint-fallback），--nodes=all 可读到该文本");
        out.println("config 页：生产配置页 UI（字段定制与游戏内同一入口）；配置真源落随会话删除的临时目录，"
                + "文件初始不存在 ⇒ 出图是默认配置下的配置页；不接收 --theme（主题对它是配置内容）");
    }
}
