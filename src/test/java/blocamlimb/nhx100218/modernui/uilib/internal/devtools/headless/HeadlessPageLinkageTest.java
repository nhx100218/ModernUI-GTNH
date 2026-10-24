package blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * headless 页面装配的<b>端到端</b>门禁：能在最小集类路径上装配的页面就应当真的出图，
 * 且本类交付的每一项能力都要有一条会红的判据。
 *
 * <h3>它守的回归（实测发生过一次）</h3>
 * <p>配置页接入时，它的装配入口与 MC 宿主包装在<b>同一个类</b>里。那个类在最小集类路径
 * （不含 {@code net.minecraft.*}）下<b>根本加载不了</b>——实测
 * {@code Class.forName("…ModernConfigEntry")} 抛
 * {@code NoClassDefFoundError: net/minecraft/client/gui/GuiScreen}，于是出图直接非 0 退出。
 * 修法是按「一个类要么是宿主、要么是装配」拆开（{@code ModernConfigEntry} /
 * {@code ModernConfigAssembly}）。</p>
 *
 * <p>触发点（独立复核的合成实验 + 单行变异定位）：<b>不是</b>「方法签名引用了 MC 类型」——
 * 仅出现在方法签名 / 字段类型 / {@code checkcast} / {@code instanceof} 里的 MC 类型都不触发；
 * 真实原因是校验期的可赋值性检查迫使 JVM 解析缺失的父类型（{@code return new ModernConfigScreen(…)}
 * 要证明它可赋给 {@code GuiScreen}）。故本类只钉端到端事实，不把机制推断写进断言。</p>
 *
 * <h3>为什么 {@code chat} / {@code hud} 的<b>渲染</b>不在覆盖内</h3>
 * <p>它们的探针宿主在<b>方法体</b>里使用 {@code net.minecraft.*}（{@code ChatComponentText} 等），
 * 本来就只在 {@code classpath-full.txt} 上可跑（规划 F21 记录）。那是注入面的事实，不是被测行为；
 * 把「页面需要哪份 classpath」固化成<b>渲染判据</b>只会变成假契约。但「缺件时<b>怎么报</b>」是契约
 * 本身：见 {@code missingMinecraftDependencyReportsContractExitCode}，它刻意用最小集跑 chat 当触发手段。</p>
 *
 * <h3>为什么 playground 也要测</h3>
 * <p>它不是回归对象，是<b>正锚</b>：若最小集本身缺件（natives 未解压、classpath 未重建），
 * 只测配置页会红得看不出成因。playground 通过即排除「环境没搭好」，剩下的红就是页面自己的问题。</p>
 *
 * <h3>为什么 section 轴与临时目录也各占一条</h3>
 * <p>独立复核用变异实测过：把 {@code ConfigScreen.showSection} 改成空操作、或摘掉
 * {@code HeadlessSession.close()} 里的 {@code hostCleanup.run()}，本类其余用例<b>全绿</b>
 * ——即「参数收下了但没接线」「痕迹没清掉」这两类静默降级没有被守住。三条判据各钉一项交付。</p>
 */
public class HeadlessPageLinkageTest {

    private static final Pattern COMMANDS = Pattern.compile("commands=(\\d+)");
    private static final Pattern COLORS = Pattern.compile("colors=(\\d+)");
    private static final Pattern SEGMENTS = Pattern.compile("segments=(\\d+)");
    private static final Pattern DISPATCHED = Pattern.compile("dispatched=(\\d+)");
    private static final Pattern SEARCH_RESULTS = Pattern.compile("Search results \\((\\d+)\\)");
    private static final Pattern NODE_LINE = Pattern.compile("(r[0-9/]+) SceneNode(?: \"[^\"]*\")? @\\d+,(-?\\d+)");
    private static final Pattern FRAMES_PLAN = Pattern.compile("frames: (\\d+)/(\\d+)");
    private static final Pattern SCROLL_STATE = Pattern.compile("scroll: offset=(\\d+) max=(\\d+) visible=(\\d+)");
    private static final String TEMP_DIR_PREFIX = "modernui-headless-config-";

    /** 配置页在最小集上必须真的出图（本轮回归的守卫）。 */
    @Test
    public void configPageRendersOnTheMinimalClasspath() throws Exception {
        String output = render("linkage-config", "--page=config");
        Assert.assertTrue("配置页必须下发绘制命令（否则是装配失败而非「页面没内容」）：\n" + output,
                commandsOf(output) > 0);
        Assert.assertTrue("配置页自检必须通过：\n" + output, output.contains("self-check: ok"));
    }

    /** 正锚：最小集本身是好的（含 natives 与 classpath 注入）。 */
    @Test
    public void playgroundPageRendersOnTheMinimalClasspath() throws Exception {
        String output = render("linkage-playground", "--page=playground");
        Assert.assertTrue("playground 也必须出图；它失败说明最小集/注入面坏了，而不是被测页面：\n" + output,
                commandsOf(output) > 0);
    }

    /** section 轴必须真的切换内容（否则 {@code --page-index} 对 config 页是空话）。 */
    @Test
    public void configSectionAxisSwitchesContent() throws Exception {
        String output = render("linkage-section", "--page=config", "--page-indexes=0,1,2");
        Set<String> colors = new LinkedHashSet<String>();
        Matcher matcher = COLORS.matcher(output);
        while (matcher.find()) {
            colors.add(matcher.group(1));
        }
        // 三档各一条 self-check 行；三档颜色数必须两两不同（实测 1156 / 1066 / 1117）。
        Assert.assertEquals("三档 section 必须画出不同内容 —— 三档颜色数全同时说明切换没接线：\n" + output,
                3, colors.size());
    }

    /**
     * 归类判据必须区分 {@code NoClassDefFoundError} 的<b>两种成因</b>：类不在类路径上（换启动器有效）
     * vs 类在、但静态初始化已失败（erroneous 类，换启动器无效）。
     *
     * <p>后者是独立审核给出的 JVM 级反例：{@code <clinit>} 首次失败后，第二次触碰抛的
     * {@code NoClassDefFoundError} 消息是 {@code Could not initialize class X} 而不是类名。若不排除，
     * 那类<b>环境 / 初始化</b>缺陷会被误报成「启动器选错」，把处置指向错误的动作（同一异常类型既可能是
     * 缺件、也可能不是 —— 判据不能只看类型）。本判据不需要 GL 环境与进程外直启：直接构造异常实例。</p>
     */
    @Test
    public void erroneousClassIsNotClassifiedAsMissingClasspath() {
        Assert.assertEquals("erroneous 类不是类路径缺件，应归设施失败 3（带栈）：",
                3, HeadlessShotMain.diagnoseUnexpected(
                        new NoClassDefFoundError("Could not initialize class foo.Bar")));
        Assert.assertEquals("真正的缺件仍归 6：",
                HeadlessShotMain.EXIT_CLASSPATH_INSUFFICIENT,
                HeadlessShotMain.diagnoseUnexpected(
                        new NoClassDefFoundError("net/minecraft/util/IChatComponent")));
        Assert.assertEquals("ClassNotFoundException 同样归 6：",
                HeadlessShotMain.EXIT_CLASSPATH_INSUFFICIENT,
                HeadlessShotMain.diagnoseUnexpected(new ClassNotFoundException("foo.Bar")));
    }

    /**
     * 用最小集启动器跑需要完整类路径的页面（chat）：必须在**契约内**报「类路径缺件」。
     *
     * <p>它守的回归（实测发生过）：装配期抛出的 {@code NoClassDefFoundError} 此前直接冒泡出
     * {@code main}，JVM 以退出码 <b>1</b> 收场；更糟的是<b>逐档独立进程批量</b> —— 子进程各以 1 退出、
     * 父进程把它聚合算成「内容可疑(4)」并继续跑完剩余档。两者都不是「启动器选错」该有的表现。现由
     * {@code HeadlessShotMain.run} 的进程边界兜底归类为
     * {@link HeadlessShotMain#EXIT_CLASSPATH_INSUFFICIENT}，并给出可操作指引。</p>
     *
     * <p>这条判据刻意用 <b>最小集</b> 跑 chat（它触及 {@code net.minecraft.util.IChatComponent}）：
     * 「页面需要哪份 classpath」是注入面事实，此处只拿它当<b>触发手段</b>，钉的是退出码契约与指引 ——
     * 与类头「chat / hud 不在覆盖内」不矛盾：那条讲<b>渲染判据</b>不在最小集上跑，本判据钉的是
     * 「装配失败怎么报」。</p>
     */
    @Test
    public void missingMinecraftDependencyReportsContractExitCode() throws Exception {
        Shot shot = runShot("linkage-classpath", "--page=chat");
        HeadlessShotGate.assumeEnvironmentAvailable("linkage-classpath", shot.exit, shot.output);
        Assert.assertEquals("类路径缺件必须报契约内的 6（此前是未捕获错误 → 退出码 1，契约外）：\n"
                + shot.output, HeadlessShotMain.EXIT_CLASSPATH_INSUFFICIENT, shot.exit);
        Assert.assertTrue("诊断必须自证缺的是哪个类型：\n" + shot.output,
                shot.output.contains("CLASSPATH-INSUFFICIENT"));
        Assert.assertTrue("诊断必须给出可操作指引（换完整集启动器）：\n" + shot.output,
                shot.output.contains("modernui-shot-full"));
    }

    /**
     * 玻璃质量档必须真的接到渲染上，且降档后**仍走 shader**。
     *
     * <p>eco（9 抽头）此前从未在设施里执行过：它走 {@code #if UIB_TAP_BUDGET >= 13} 的 {@code #else}
     * 分支，是 F38 修好 shader 编译后仍未被任何出图覆盖的代码路径。判据钉两件事 —— 档位真的生效
     * （读数带 {@code quality=eco taps=9}），且该档的 shader 变体编译/链接通过（读数仍 {@code path=shader}；
     * 落到 {@code fixed-pipeline} 就说明这一档的程序不可用）。</p>
     */
    @Test
    public void backdropQualityAxisSelectsTheKernelVariant() throws Exception {
        String output = render("linkage-backdrop-quality", "--page=glass", "--backdrop-quality=eco");
        Assert.assertTrue("eco 档必须真的生效（读数应带档位与抽头预算）：\n" + output,
                output.contains("backdrop: quality=eco taps=9"));
        Assert.assertTrue("eco 档仍必须走 shader —— 落到 fixed-pipeline 说明 9 抽头变体不可用：\n" + output,
                output.contains("path=shader"));
    }

    /**
     * 玻璃质量档必须覆盖 chat / hud 页（此前只在 glass 页有判据）。
     *
     * <p>边界登记里「{@code --backdrop-quality} 与 chat/hud 的组合」长期是未测项：chat 页的玻璃在气泡
     * 表面（{@code ChatMessageList} 的 bubbleSurface → {@code UiBackdrop}），hud 页是同一棵内容树走 HUD
     * 宿主装配。判据钉三件事：三档读数各自正确（full 13 抽头 / eco 9 抽头 / solid 禁用滤镜）、两个非
     * solid 档仍走 shader（编译降级会露出来）、三档产物两两不同（档位真的进了像素）。</p>
     *
     * <p>实测（1280x720，chat / hud 各三档）：chat 11563 / 11568 / 9501 B、hud 11841 / 11881 / 9866 B；
     * 玻璃请求计数 chat=22 / hud=21。它同时是这两页玻璃装配的回归锚 —— 之前没有任何判据跑过
     * 「chat/hud × 玻璃档」。</p>
     */
    @Test
    public void backdropQualityAxisAppliesToChatAndHud() throws Exception {
        for (String page : new String[] {"chat", "hud"}) {
            String name = "linkage-bq-" + page;
            String output = renderFull(name, "--page=" + page, "--text=Steve:glass probe",
                    "--backdrop-qualities=full,eco,solid");
            Assert.assertTrue(page + "：full 档必须报 13 抽头：\n" + output,
                    output.contains("backdrop: quality=full taps=13 path=shader"));
            Assert.assertTrue(page + "：eco 档必须报 9 抽头且走 shader：\n" + output,
                    output.contains("backdrop: quality=eco taps=9 path=shader"));
            Assert.assertTrue(page + "：solid 档必须禁用滤镜（读数 path=none）：\n" + output,
                    output.contains("backdrop: quality=solid taps=13 path=none"));
            Assert.assertEquals(page + "：两个非 solid 档都必须走 shader（降级会露出来）：\n" + output,
                    2, countOf(output, " path=shader"));
            List<Path> produced = sameStem(Paths.get("build", "reports", "headless", name + ".png")
                    .toAbsolutePath());
            Assert.assertEquals(page + "：三档产物必须两两不同（档位没进像素）：\n" + output,
                    3, digestsOf(produced).size());
        }
    }

    /**
     * picker 页必须接收 {@code --theme}：面板表面经主题配方派生，换档应改变像素。
     *
     * <p>补这条的理由与 glass 的 {@code glassThemeAxisSwitchesContent} 同型 —— 独立复核指出
     * 「picker 实际吃外观档，但环境矩阵页面表与 {@code HeadlessThemes} 类注释都没补行、也没有判据」。
     * 实测 dark colors=1410 / light colors=847（字节 356569 / 240753）。</p>
     */
    @Test
    public void pickerThemeAxisSwitchesContent() throws Exception {
        String output = render("linkage-picker-theme", "--page=picker",
                "--themes=liquid-glass-dark,liquid-glass-light");
        Set<String> colors = new LinkedHashSet<String>();
        Matcher matcher = COLORS.matcher(output);
        while (matcher.find()) {
            colors.add(matcher.group(1));
        }
        Assert.assertEquals("两档外观必须画出不同内容（颜色数全同即主题没接到 picker 面板）：\n" + output,
                2, colors.size());
    }

    /**
     * picker 页三态都要出图，且状态切换**真的**改变了内容。
     *
     * <p>它守的交付：picker（库内体量最大的控件族）此前没有出图入口 —— 生产配置页 schema 里没有字段
     * 挂 {@code SearchPickerSpec}，从 {@code --page=config} 也看不到它。本判据用「全部态 > 过滤态 >
     * 空态」的命令面递减钉「切换真的生效」，用三档 colors 两两不同钉「画出来的东西不同」：
     * 只测「exit=0 有产物」的话，把 {@code showState} 改成空操作也能全绿。</p>
     */
    @Test
    public void pickerPageRendersAllThreeStates() throws Exception {
        String all = render("linkage-picker-all", "--page=picker");
        String filtered = render("linkage-picker-filtered", "--page=picker", "--page-index=1");
        String empty = render("linkage-picker-empty", "--page=picker", "--page-index=2");
        Assert.assertTrue("全部候选态必须有绘制命令（面板 + 网格）：\n" + all, commandsOf(all) > 0);
        Assert.assertTrue("过滤态的命令面应小于全部态（结果真的收缩了）：\n" + all + "\n" + filtered,
                commandsOf(filtered) < commandsOf(all));
        Assert.assertTrue("空结果态的命令面应最小：\n" + filtered + "\n" + empty,
                commandsOf(empty) < commandsOf(filtered));
        Set<String> colors = new LinkedHashSet<String>();
        for (String output : new String[] { all, filtered, empty }) {
            Matcher matcher = COLORS.matcher(output);
            if (matcher.find()) {
                colors.add(matcher.group(1));
            }
        }
        Assert.assertEquals("三态颜色数必须两两不同（画出来的内容不同）：\n" + all + "\n" + filtered + "\n" + empty,
                3, colors.size());
    }

    /** 磨玻璃实验室在最小集上必须真的出图（页面覆盖：它此前只能靠开游戏看）。 */
    @Test
    public void glassPageRendersOnTheMinimalClasspath() throws Exception {
        String output = render("linkage-glass", "--page=glass");
        Assert.assertTrue("磨玻璃实验室必须下发绘制命令（否则是装配失败而非「页面没内容」）：\n" + output,
                commandsOf(output) > 0);
        Assert.assertTrue("磨玻璃实验室自检必须通过：\n" + output, output.contains("self-check: ok"));
    }

    /**
     * 玻璃必须走 shader 路径 —— 钉住 <b>2026-09-02 至 09-18 的静默降级回归</b>。
     *
     * <p>那段时期 {@code uiBackdropF.frag} 的注释里混入了中文，NVIDIA 编译器直接报
     * {@code error C0000: syntax error, unexpected $undefined}，程序不可用 ⇒ 玻璃一路降级到固定管线
     * （仅模糊、无 vibrancy / 亮边 / 噪点 / 液态折射），日志不报错、画面只是"没那么好看"。
     * 本判据钉的是端到端事实（本帧真的用了 shader），不是源字符集检查 —— 后者是另一条独立门禁
     * （{@code GlslSourceAsciiGuardTest}）。</p>
     */
    @Test
    public void glassBackdropUsesTheShaderPath() throws Exception {
        String output = render("linkage-glass-shader", "--page=glass");
        // 断言取 " path=shader" 而非 "backdrop: path=shader"：读数行首是档位段（quality=… taps=…），
        // 把它写进断言会让「加一个读数段」变成假红。
        Assert.assertTrue("玻璃未走 shader 路径（先查 GLSL 资源是否含非 ASCII / 着色器是否编译失败）：\n" + output,
                output.contains(" path=shader"));
    }

    /**
     * glass 页必须真的接收 {@code --theme}：外观档注入是它的一项交付，不能只停在「参数收下了」。
     *
     * <p>独立复核用变异指出过这个覆盖缺口：把 {@code createHost} 里 glass 分支的 theme 传 {@code null} 时，
     * 本类其余 7 条 + {@code GlassLabHostThemeTest} 8 条 + {@code GlassLabHostTest} 5 条<b>全绿</b>
     * —— 即「glass 接收外观档」这项能力当时没有任何判据守着。本判据按颜色数两两不同来钉端到端事实
     * （实测 dark 16336 / light 14373；与 config 的 section 轴判据同型）。</p>
     */
    @Test
    public void glassThemeAxisSwitchesContent() throws Exception {
        String output = render("linkage-glass-theme", "--page=glass",
                "--themes=liquid-glass-dark,liquid-glass-light");
        Set<String> colors = new LinkedHashSet<String>();
        Matcher matcher = COLORS.matcher(output);
        while (matcher.find()) {
            colors.add(matcher.group(1));
        }
        Assert.assertEquals("两档外观必须画出不同内容（颜色数全同即主题没接到 glass 装配上）：\n" + output,
                2, colors.size());
    }

    /** 玻璃路径读数必须按「本次渲染窗口的请求计数」判定，不得把上一档的残留报成本档事实。 */
    @Test
    public void backdropPathIsNotInheritedAcrossPagesInOneProcess() throws Exception {
        String output = render("linkage-backdrop-shared", "--share-context", "--pages=glass,text-probe");
        Assert.assertEquals("glass 档应报 shader、text-probe 档应报 none（同进程复用时会暴露残留误报）：\n" + output,
                1, countOf(output, " path=shader"));
        Assert.assertEquals("无玻璃请求的页面必须如实报 none：\n" + output,
                1, countOf(output, " none（本次渲染窗口内未发起"));
    }

    /** 出图不得在进程外留痕：临时配置目录跑完必须消失。 */
    @Test
    public void configPageLeavesNoTempDirectory() throws Exception {
        Set<String> before = tempConfigDirs();
        render("linkage-tempdir", "--page=config");
        Set<String> after = tempConfigDirs();
        after.removeAll(before);
        Assert.assertTrue("出图后残留了临时配置目录（会话清理没执行）：" + after, after.isEmpty());
    }

    /**
     * 输入脚本必须真的驱动交互 —— 这条钉 headless 的「交互证据能力」。
     *
     * <p>此前 {@code --actions} / {@code --script} 在测试里**零覆盖**：使用文档有整章示例，
     * 但没有判据守着「脚本真的派发事件、点击真的改变画面」。本判据用 playground 首页点「Markdown 渲染」
     * 导航项：基线帧没有段流（{@code segments=0}），点击后落到 Markdown 页（段流非零），
     * 且读数报告指针落在脚本声明的坐标上。</p>
     */
    @Test
    public void actionScriptDrivesAClickThatChangesTheShot() throws Exception {
        String base = render("linkage-input-base", "--page=playground");
        // 前提（不是被测行为）：playground 首页正文是零段流的形状事实，差分靠它成立。
        Assert.assertEquals("基线（未点击）不该有段流：\n" + base, 0, segmentsOf(base));
        String clicked = render("linkage-input-click", "--page=playground",
                "--actions=move 953 84; frame; click; wait 8");
        // 必须解析数字：子串 "dispatched=" 在基线里也存在（值为 0），纯 contains 无判别力
        // （独立审核实测：把 click 的 press/release 去掉后 dispatched=1 仍能通过 contains）。
        Assert.assertTrue("输入脚本必须真的派发事件（dispatched 应为正）：\n" + clicked,
                dispatchedOf(clicked) > 0);
        Assert.assertTrue("指针必须落在脚本声明的坐标（953,84）：\n" + clicked,
                clicked.contains("pointer=953,84"));
        Assert.assertTrue("点击后必须切到 Markdown 页（段流出现）：\n" + clicked, segmentsOf(clicked) > 0);
    }

    /**
     * 目标寻址链路必须自洽：{@code --find} 报出的地址，{@code --center} 解出的中心点必须逐字相同；
     * 越界地址必须按契约失败（退出码 3），不得静默回落到 0,0。
     *
     * <p>它是 agent 用「地址 → 中心点 → 输入脚本」这条链路的守卫：地址来自布局，写死坐标会在
     * 换字号 / 换尺寸后失效（使用文档已写明「跨字号脚本请按路径重新取坐标」）。</p>
     */
    @Test
    public void findAddressResolvesToTheSameCenterAndOutOfRangeFails() throws Exception {
        String find = renderQuery("linkage-find", "--page=playground", "--find=Markdown");
        Matcher matcher = Pattern.compile("(r[0-9/]+) SceneNode .*center=(\\d+,\\d+)").matcher(find);
        Assert.assertTrue("--find 必须给出地址与中心点：\n" + find, matcher.find());
        String address = matcher.group(1);
        String center = matcher.group(2);
        String resolved = renderQuery("linkage-center", "--page=playground", "--center=" + address);
        Assert.assertTrue("--center 解同一地址必须得到同一中心点 " + center + "：\n" + resolved,
                resolved.contains("center " + address + " = " + center));
        // 越界地址取「真实父路径 + 越界下标」：父节点确实有子节点，于是它只能靠边界检查失败 ——
        // 用凭空虚构的深路径会走到异常分支、同样非 0，钉不住「静默回落到某个存在的节点」这种变异。
        String parent = address.indexOf('/') > 0 ? address.substring(0, address.lastIndexOf('/')) : "r0";
        Shot bad = runShot("linkage-center-bad", "--page=playground", "--center=" + parent + "/99");
        Assert.assertEquals("越界地址必须按契约失败（而不是静默回落到某个存在的节点）：\n" + bad.output,
                3, bad.exit);
        // 退出码 3 是「未捕获的未预期错误」与「全部能力失败」的公共码：只钉退出码挡不住
        // 「夹取到一个未布局的节点」这类静默回落，成因必须自证。
        Assert.assertTrue("失败必须自证成因是「地址越界」：\n" + bad.output,
                bad.output.contains("地址越界"));
    }

    /**
     * picker 的查询框必须吃输入脚本：{@code type}（逐字符）与 {@code compose}（整串提交）都要真的过滤候选。
     *
     * <p>它守的是一条真实缺口：探针宿主此前<b>没有</b>订阅查询变更 —— {@code --page-index=1} 的过滤态是
     * 手动同时写 {@code query} 与 {@code results} 两个信号绕出来的，而输入脚本键入的查询只改面板文本、
     * 候选集不动（实测键入 `stone` 后仍是 24 条）。修法是把「查询变更 ⇒ 重算候选」接回装配层
     * （与真机「装配层持候选、面板只渲染」的分工一致），此后两条路径同源。</p>
     */
    @Test
    public void pickerQueryInputFiltersResults() throws Exception {
        String base = renderQuery("linkage-picker-input-base", "--page=picker", "--nodes");
        Assert.assertEquals("基线候选数：\n" + base, 24, searchResultsOf(base));

        String typed = renderQuery("linkage-picker-input-type", "--page=picker", "--nodes",
                "--actions=move 446 138; frame; click; frame; type stone; wait 12");
        Assert.assertEquals("type 必须真的过滤候选（24 → 5）：\n" + typed, 5, searchResultsOf(typed));

        String composed = renderQuery("linkage-picker-input-compose", "--page=picker", "--nodes",
                "--actions=move 446 138; frame; click; frame; compose stone; wait 12");
        Assert.assertEquals("compose（整串提交）必须与 type 同结果：\n" + composed, 5,
                searchResultsOf(composed));

        String noMatch = renderQuery("linkage-picker-input-nomatch", "--page=picker", "--nodes",
                "--actions=move 446 138; frame; click; frame; type zzzz; wait 12");
        Assert.assertEquals("无命中查询必须落到空态（0 条）：\n" + noMatch, 0, searchResultsOf(noMatch));

        // 前缀敏感用例：glass_ 只命中 glass_pane（1），而它的真前缀 glass 命中 2 ——
        // 少了这条，「type 少派发末尾字符」会伪装成通过（独立审核实测变异存活：ston 与 stone 同为 5）。
        String prefixSensitive = renderQuery("linkage-picker-input-prefix", "--page=picker", "--nodes",
                "--actions=move 446 138; frame; click; frame; type glass_; wait 12");
        Assert.assertEquals("查询 glass_ 必须只命中 glass_pane（少派发末尾字符会退化成 glass=2）：\n"
                + prefixSensitive, 1, searchResultsOf(prefixSensitive));
    }

    /**
     * 键盘编辑路径（{@code key BACKSPACE}）必须真的改查询：键入 `stone` 后五次退格回到全量。
     *
     * <p>它与 {@link #pickerQueryInputFiltersResults()} 合起来覆盖「键入 → 编辑 → 结果」整条链；
     * 只钉键入的话，退格不生效（或退格只动光标不动查询）不会有任何判据变红。</p>
     */
    @Test
    public void keyStatementEditsTheQuery() throws Exception {
        String output = renderQuery("linkage-picker-key", "--page=picker", "--nodes",
                "--actions=move 446 138; frame; click; frame; type stone; wait 6;"
                        + " key BACKSPACE; key BACKSPACE; key BACKSPACE; key BACKSPACE; key BACKSPACE; wait 12");
        Assert.assertEquals("退格必须逐字符缩短查询（stone → 空 ⇒ 回到 24 条）：\n" + output,
                24, searchResultsOf(output));
    }

    /**
     * 滚轮语句必须真的滚内容：config 页内容高于视口，**负 wheelDelta**（向下滚内容）应让内容区节点上移。
     *
     * <p>主证据取 {@code --nodes} 投影里的节点 y 位移，而不是像素哈希 —— 位移量是滚动的直接语义，
     * 跨 GL 后端稳定（独立审核建议）。注意方向语义：{@code scroll 10}（内容已在顶部）不产生位移，
     * 早期把「正向无效」误记成「语句无效」是错的（同一份记录里 {@code scroll -5} 实测位移 5px）。</p>
     */
    @Test
    public void scrollStatementMovesContent() throws Exception {
        String before = renderQuery("linkage-scroll-before", "--page=config", "--nodes",
                "--actions=move 700 350; frame; wait 11");
        String after = renderQuery("linkage-scroll-after", "--page=config", "--nodes",
                "--actions=move 700 350; frame; scroll -5; wait 10");
        Map<String, Integer> beforeYs = nodeYs(before);
        Map<String, Integer> afterYs = nodeYs(after);
        int moved = 0;
        for (Map.Entry<String, Integer> entry : beforeYs.entrySet()) {
            Integer now = afterYs.get(entry.getKey());
            if (now != null && entry.getValue().intValue() - now.intValue() == 5) {
                moved++;
            }
        }
        Assert.assertTrue("负 wheelDelta 必须让内容上移 5px（实测位移节点数 " + moved + "）：\n" + after,
                moved > 0);
    }

    /**
     * 修饰键的**端到端**语义：Shift+点击在文本控件里扩展选区，Ctrl+点击不扩展。
     *
     * <p>判据用「**等价物**」而不是「像素变了」：Shift+点击必须与**拖拽到同一点**逐字节相同，
     * Ctrl+点击必须与**无修饰的两次点击**逐字节相同。两个方向都能被钉住 —— 修饰位若互换，Shift 会
     * 退化成普通点击、Ctrl 会变成扩展选区，两条断言同时红。</p>
     *
     * <p><b>它补的是设备层判据看不到的那一层</b>：{@code HeadlessInputDeviceTest} 只钉「事件载荷里的
     * 修饰位正确」，控件侧若压根不读（或读错）修饰位，事件层仍全绿。第三条断言是反空跑自检 ——
     * 先确认「拖拽 vs 普通点击」本身可区分，否则前两条毫无意义。</p>
     *
     * <p>本条在 F47（帧时钟虚拟化）之前**写不出来**：那时带输入脚本的出图不逐字节确定（同一命令
     * 连跑 8 次得 4 种产物，差异是 caret 竖线的相位族），等价物对拍必然假红。F47 落地后实测四种脚本
     * 各自 3/3 稳定：{@code plain = ctrl}、{@code drag = shift}、{@code plain != drag}。</p>
     *
     * <p><b>两页同型</b>：单行文本（{@code --page-index=1}）与多行文本（{@code --page-index=2}）
     * 各钉一遍 —— 它们是两个 primitive（{@code SceneTextInputPrimitive} / {@code SceneTextAreaPrimitive}），
     * 独立审核指出「文本域在最小集可达、属可覆盖而未做」，故一并覆盖。</p>
     */
    @Test
    public void shiftClickExtendsSelectionAndCtrlClickDoesNot() throws Exception {
        assertModifierEquivalence(1, "single");
        assertModifierEquivalence(2, "multiline");
    }

    /** 端到端修饰键等价关系（给定 playground 页下标）。 */
    private static void assertModifierEquivalence(int pageIndex, String label) throws Exception {
        String page = "--page-index=" + pageIndex;
        String plain = renderDigest("linkage-mod-" + label + "-plain", "--page=playground", page,
                "--actions=move 300 180; frame; down; frame; up; frame; move 500 180; frame; down;"
                        + " frame; up; wait 6");
        String drag = renderDigest("linkage-mod-" + label + "-drag", "--page=playground", page,
                "--actions=move 300 180; frame; down; frame; move 500 180; frame; up; wait 6");
        String shift = renderDigest("linkage-mod-" + label + "-shift", "--page=playground", page,
                "--actions=move 300 180; frame; down; frame; up; frame; keydown SHIFT_LEFT; frame;"
                        + " move 500 180; frame; down; frame; up; wait 6");
        String ctrl = renderDigest("linkage-mod-" + label + "-ctrl", "--page=playground", page,
                "--actions=move 300 180; frame; down; frame; up; frame; keydown CONTROL_LEFT; frame;"
                        + " move 500 180; frame; down; frame; up; wait 6");

        String hint = "（若两个摘要都随机变化、彼此都不同，先查帧时钟 F47 是否被回退 —— 那是假红不是语义错）";
        Assert.assertNotEquals("自检：拖拽与普通点击必须本来就不同（否则下面的等价断言是空的）："
                + plain + " vs " + drag + hint, plain, drag);
        Assert.assertEquals("Shift+点击必须与「拖拽到同一点」等价（扩展选区）：\n" + shift + hint,
                drag, shift);
        Assert.assertEquals("Ctrl+点击不得扩展选区（应与无修饰的两次点击等价）：\n" + ctrl + hint,
                plain, ctrl);
    }

    /**
     * 点击计数决定选区粒度（双击选词 / 三击选整行）与指针取消的**端到端**判据
     * （playground 单行页 / 多行页各一遍）。
     *
     * <p>守的交付（覆盖事实按实测口径写，不夸大）：</p>
     * <ul>
     *   <li>{@code dblclick} <b>语句</b>此前在设备层与页面级都零覆盖 ——
     *       {@code HeadlessInputDeviceTest} 全类无 {@code dblclick} 用例；</li>
     *   <li>{@code cancel} <b>语句</b>在设备层只在修饰键用例里顺带钉过载荷
     *       （{@code pointerModifiersFollowHeldKeysWithoutSwapping} 的四条指针路径），
     *       而文本控件的 {@code POINTER_CANCEL} 分支在控件层此前**零覆盖**
     *       （{@code SceneTextInputTest} / {@code SceneTextAreaTest} 均无该事件）；</li>
     *   <li>控件层的 {@code clickCount == 2 / >= 3} 分支**已有**单点单元判据
     *       （{@code SceneTextInputTest.doubleClickSelectsWord} / {@code tripleClickSelectsWholeLine}、
     *       {@code SceneTextAreaTest.doubleClickSelectsWord} / {@code tripleClickSelectsLine}）——
     *       本用例补的是「真实页面栈 + 真实指针合成链路 + 像素产物」这一层，不是唯一防线
     *       （独立审查实测：把 {@code == 2} 改成 {@code == 99} 会同时红 5 条既有单元用例）；</li>
     *   <li>真正只有端到端能发现的是<b>指针取消的语义</b>（cancel 后移动不扩展、cancel 不得丢弃
     *       已拖出的选区）与词 / 逻辑行粒度在真实合成链路上的表现（与
     *       {@link #shiftClickExtendsSelectionAndCtrlClickDoesNot()} 同族）。</li>
     * </ul>
     *
     * <h4>判据一：双击按<b>词</b>粒度（等价物对拍）</h4>
     * <p>同一个词内的两个不同落点必须给出**逐字节相同**的产物；而同点的**单击**必须彼此不同
     * （否则同词等价是空跑：两下都没命中控件也会相等）。跨词必须不同。另有反空跑自检：
     * 双击产物不得等于同点单击产物 —— 双击退化成两次独立单击时它会红。</p>
     *
     * <p>实测（1280x720、fs=16、最小集）：单行页 {@code 240..270} 同落 {@code Hello} 词内、
     * {@code 280} 落在空格、{@code 290..300} 落在 {@code Qz}；多行页 {@code 250..270} 同落
     * {@code 第一行}、{@code 290} 落在词间分隔符、{@code 310} 落在 {@code 欢迎使用}。
     * 落点是从「双击产物随 x 变化的分段恒定结构」里量出来的，不是按字宽推算的。</p>
     *
     * <h4>判据二：落分隔符时折叠（不得误选相邻词）</h4>
     * <p>双击产物必须等于**同一点单击**（折叠为插入符），且不得等于左右任一词的产物 ——
     * 「折叠」与「误选邻居」在像素上完全不同，后者会红。</p>
     *
     * <h4>判据三：三击按<b>整行</b>粒度（oracle 分页取）</h4>
     * <p>两页都出一次 <b>Ctrl+A</b>（完全独立的键盘路径）：单行控件上「整行」就是「全选」，
     * 于是它当**等价物** oracle（两条路径的判定分支互不共用，等价物对拍因此有判别力）；
     * 多行控件上三击是**逻辑行**、Ctrl+A 是全选，两者在产物上可区分 —— 于是它反过来当
     * **排除式** oracle（三击不得等于全选），粒度再由**同逻辑行、不同词**的另一个落点钉住。
     * 两个 oracle 合起来才排得掉「三击退化成全选」与「三击退化成选词」（后者是独立审查
     * 指出的强度缺口：只取同词两点时，选词回归在两条相等式上都为真）。两页都另有反空跑自检：
     * 三击产物不得等于同点双击产物。</p>
     *
     * <h4>判据四：{@code cancel} 终止拖选、且不丢弃已拖出的选区</h4>
     * <p>三条：①「拖到 A → cancel」必须与<b>正常松开</b>（{@code up}）逐字节相同 —— cancel 是
     * 「结束手势」不是「撤销选择」，把 cancel 改成「清锚 + 折叠到 caret 0」的回归会在这里红
     * （独立审查实测：缺这一条时该变异整套断言全绿）；②「拖到 A → cancel → 移到 B」必须与
     * 「拖到 A → cancel」相同（cancel 之后的指针移动不再扩展选区）；③「拖到 A → 移到 B」
     * （不 cancel）必须与之不同（反空跑）。四条脚本取**相同帧数**（各 12 次推进）：这是防御性
     * 写法，不是必要条件 —— 实测收尾空帧数并不改变产物（审查者实测 {@code click} 后 {@code wait}
     * 0/6/12/18/24/32/40 同摘要、{@code cancel} 后 0/9/15/24/40 同摘要），即插入符闪烁相位不参与渲染。</p>
     *
     * <p>实测摘要（每个脚本 2/2 次逐字节相同；单行页 / 多行页）：同词双击 {@code 5c7a9c51d7ff} /
     * {@code a38154d23f13}、跨词 {@code 2163691c990a} / {@code e16e9078867c}、分隔符折叠
     * {@code 930397454983} / {@code ff9494dcf548}、三击 {@code 4fb09e354db8} / {@code 66ae97fa4659}、
     * {@code Ctrl+A} 全选 {@code 4fb09e354db8} / {@code f3cf6565bd64}（单行页三击 = 全选；
     * 多行页两者必须不同，否则「三击退化成全选」会在相等式下静默通过）；「拖 280→300 → cancel」
     * 与「拖 280→300 → 正常松开」同为 {@code 12b3305e2f9d} / {@code 66258b21291d}，
     * 不 cancel 的对照 {@code 52dbc11df0d2} / {@code 8a19c4e6395f}。单用例耗时约 75 s
     * （作者一次 74.3 s；审查者三次 73.3 / 76.0 / 77.7 s，属方差）。图像侧另有独立验证：
     * 审查者用纯标准库 PNG 解码做逐像素差分 —— 双击画出整词高亮带（单行页 {@code x=242..284}
     * 即 {@code Hello}）、分隔符落点与同点单击 {@code diff_px=0}、三击高亮只落在第一条逻辑行
     * （{@code y=176..191}），而 Ctrl+A 还覆盖到 {@code y=287}。</p>
     */
    @Test
    public void doubleClickSelectsWordsTripleClickSelectsLinesAndCancelAbortsDrag() throws Exception {
        assertWordAndCancelSemantics(1, "single", 250, 270, 300, 280);
        assertWordAndCancelSemantics(2, "multiline", 250, 270, 310, 290);
    }

    /**
     * 端到端「双击选词 + cancel 终止拖选」（给定 playground 页下标与实测落点）。
     *
     * @param pageIndex   页下标（1=单行文本、2=多行文本）
     * @param label       产物名后缀
     * @param wordX       词内落点 A
     * @param sameWordX   词内落点 B（与 A 同词）
     * @param otherWordX  另一个词内的落点
     * @param separatorX  词间分隔符上的落点
     */
    private static void assertWordAndCancelSemantics(int pageIndex, String label,
                                                     int wordX, int sameWordX, int otherWordX, int separatorX)
            throws Exception {
        String stem = "linkage-word-" + label;
        // 失败消息带页标识：helper 被两页复用，page2 单独红时消息必须能自证是哪一页（独立审查 R4）。
        String where = label + "(page-index=" + pageIndex + ") ";
        String[] shared = {"--page=playground", "--page-index=" + pageIndex};

        // 脚本先落变量，再经 renderDistinct 成组出图 —— 自检在 helper 内部比**实际传入的脚本**：
        // 只比「落点参数不同」（第一版）或比「变量定义」（第二版）都堵不住调用点被改成同一变量的
        // 自伤，两种形态都实测过（独立审查 M6 与 Lead 复核）。
        String scriptClickA = clickAt(wordX);
        String scriptClickB = clickAt(sameWordX);
        String scriptWordA = doubleClickAt(wordX);
        String scriptWordB = doubleClickAt(sameWordX);
        String scriptWordOther = doubleClickAt(otherWordX);
        String scriptSeparator = doubleClickAt(separatorX);
        String scriptSeparatorClick = clickAt(separatorX);

        // 等价对拍：同词两点（词粒度）、分隔符双击与同点单击（折叠）—— 两条脚本必须真的不同。
        String[] wordShots = renderDistinct(new String[] {stem + "-dbl-a", stem + "-dbl-b"},
                new String[] {scriptWordA, scriptWordB}, shared);
        String wordA = wordShots[0];
        String wordB = wordShots[1];
        String[] separatorShots = renderDistinct(new String[] {stem + "-dbl-sep", stem + "-click-sep"},
                new String[] {scriptSeparator, scriptSeparatorClick}, shared);
        String separator = separatorShots[0];
        String separatorClick = separatorShots[1];

        // 其余三张是**反空跑对照**（不等价），单独出图。
        String clickA = renderDigest(stem + "-click-a", withScript(scriptClickA, shared));
        String clickB = renderDigest(stem + "-click-b", withScript(scriptClickB, shared));
        String wordOther = renderDigest(stem + "-dbl-other", withScript(scriptWordOther, shared));

        Assert.assertNotEquals(where + "自检：同词内的两个落点必须本来就不同 —— 否则下面的词粒度断言是空的："
                + clickA + " vs " + clickB, clickA, clickB);
        Assert.assertNotEquals(where + "自检：双击产物不得等于同点单击产物（双击退化成单击会伪装成通过）："
                + wordA + " vs " + clickA, wordA, clickA);
        Assert.assertEquals(where + "双击必须按词粒度 —— 同词内两个落点得同一产物：" + wordA + " vs " + wordB,
                wordA, wordB);
        Assert.assertNotEquals(where + "跨词双击必须给出不同产物：" + wordA + " vs " + wordOther,
                wordA, wordOther);
        Assert.assertEquals(where + "双击落在词间分隔符上必须折叠为单点（等于同点单击）："
                + separator + " vs " + separatorClick, separatorClick, separator);
        Assert.assertNotEquals(where + "分隔符上的双击不得误选左侧词：" + separator + " vs " + wordA,
                separator, wordA);
        Assert.assertNotEquals(where + "分隔符上的双击不得误选右侧词：" + separator + " vs " + wordOther,
                separator, wordOther);

        // 判据三：三击 = 选整行。两页都出 Ctrl+A：单行页当**等价物** oracle（整行 == 全选，成组出图
        // 并自检两条脚本不同）；多行页当**排除式** oracle（三击是逻辑行、不得退化成全文全选），
        // 粒度由「同逻辑行、不同词」的另一个落点成组钉住 —— 只取同词两点时选词回归仍为真。
        String scriptTriple = tripleClickAt(wordX);
        String scriptSelectAll = selectAllAt(wordX);
        String triple;
        String selectAll;
        String tripleOther = null;
        if (pageIndex == 1) {
            String[] lineShots = renderDistinct(new String[] {stem + "-triple", stem + "-select-all"},
                    new String[] {scriptTriple, scriptSelectAll}, shared);
            triple = lineShots[0];
            selectAll = lineShots[1];
        } else {
            String scriptTripleOther = tripleClickAt(otherWordX);
            String[] lineShots = renderDistinct(new String[] {stem + "-triple", stem + "-triple-other"},
                    new String[] {scriptTriple, scriptTripleOther}, shared);
            triple = lineShots[0];
            tripleOther = lineShots[1];
            selectAll = renderDigest(stem + "-select-all", withScript(scriptSelectAll, shared));
        }
        Assert.assertNotEquals(where + "自检：三击不得等于同点双击（否则下面的整行断言是空的）："
                + triple + " vs " + wordA, triple, wordA);
        if (pageIndex == 1) {
            Assert.assertEquals(where + "单行控件上三击必须等于 Ctrl+A 全选（两条独立路径的等价物对拍）："
                    + triple + " vs " + selectAll, selectAll, triple);
        } else {
            Assert.assertNotEquals(where + "多行控件上三击是逻辑行、不得退化为全文全选："
                    + triple + " vs " + selectAll, triple, selectAll);
            Assert.assertEquals(where + "三击必须按逻辑行粒度 —— 同逻辑行内两个落点得同一产物："
                    + triple + " vs " + tripleOther, triple, tripleOther);
        }

        // cancel 三条：帧数取齐（各 12 次推进）—— 防御性写法，实测收尾空帧数不影响产物。
        // dragUp = 「拖到 300 后正常松开」：它是「cancel 不丢选区」的 oracle（缺它时，把 cancel
        // 改成「清锚 + 折叠到 caret 0」会让整套断言全绿，见独立审查 M5）。
        String scriptUp = "--actions=move 280 180; frame; down; frame; move 300 180; frame; up; wait 9";
        String scriptCancelThenMove = "--actions=move 280 180; frame; down; frame; move 300 180;"
                + " frame; cancel; frame; move 330 180; wait 8";
        String scriptCancelStay = "--actions=move 280 180; frame; down; frame; move 300 180;"
                + " frame; cancel; wait 9";
        String scriptNoCancel = "--actions=move 280 180; frame; down; frame; move 300 180;"
                + " frame; move 330 180; wait 9";
        String[] dragShots = renderDistinct(
                new String[] {stem + "-up", stem + "-cancel-stay", stem + "-cancel-then-move"},
                new String[] {scriptUp, scriptCancelStay, scriptCancelThenMove}, shared);
        String dragUp = dragShots[0];
        String dragCancelStay = dragShots[1];
        String dragCancelThenMove = dragShots[2];
        String dragNoCancel = renderDigest(stem + "-no-cancel", withScript(scriptNoCancel, shared));
        Assert.assertEquals(where + "cancel 不得丢弃已拖出的选区 —— 必须与正常松开逐字节相同："
                + dragUp + " vs " + dragCancelStay, dragUp, dragCancelStay);
        Assert.assertEquals(where + "cancel 之后的指针移动不得再扩展选区 —— 应与「拖完即 cancel」逐字节相同："
                + dragCancelThenMove + " vs " + dragCancelStay, dragCancelStay, dragCancelThenMove);
        Assert.assertNotEquals(where + "自检：没有 cancel 时那次移动确实会扩展选区（否则上一条是空的）："
                + dragCancelStay + " vs " + dragNoCancel, dragCancelStay, dragNoCancel);
    }

    /**
     * 成组出图 + 「参与等价对拍的脚本两两不同」自检（等价对拍专用）。
     *
     * <p>自检必须比**实际传入的脚本**：只比「落点参数不同」（第一版）在调用点被改动时不红；
     * 只比「变量定义」（第二版）在实参被换成另一个变量时不红 —— 两种形态都实测过（独立审查 M6
     * 与 Lead 复核）。把自检放进 helper、与出图共用同一组实参，两种自伤都会红。</p>
     *
     * <p>反空跑对照（期望**不相等**的产物）不走本方法：它们不需要这条自检。</p>
     *
     * @param names      产物名（与 scripts 等长、同序）
     * @param scripts    脚本（逐条出图；两两必须不同）
     * @param sharedArgs 共用的其余命令行参数
     * @return 与 scripts 同序的产物摘要
     */
    private static String[] renderDistinct(String[] names, String[] scripts, String... sharedArgs)
            throws Exception {
        Assert.assertEquals("自检：产物名与脚本必须一一对应", names.length, scripts.length);
        for (int i = 0; i < scripts.length; i++) {
            for (int j = i + 1; j < scripts.length; j++) {
                Assert.assertNotEquals("自检：参与等价对拍的脚本必须两两不同，否则等价式恒真："
                        + scripts[i] + " vs " + scripts[j], scripts[i], scripts[j]);
            }
        }
        String[] digests = new String[scripts.length];
        for (int i = 0; i < scripts.length; i++) {
            digests[i] = renderDigest(names[i], withScript(scripts[i], sharedArgs));
        }
        return digests;
    }

    /** 把脚本追加到共享参数之后（{@code renderDigest} 的 {@code (name, args...)} 契约不变）。 */
    private static String[] withScript(String script, String... sharedArgs) {
        String[] args = java.util.Arrays.copyOf(sharedArgs, sharedArgs.length + 1);
        args[sharedArgs.length] = script;
        return args;
    }

    /** 单击落点脚本（按下与抬起跨帧）。 */
    private static String clickAt(int x) {
        return "--actions=move " + x + " 180; frame; down; frame; up; wait 6";
    }

    /** 双击落点脚本。 */
    private static String doubleClickAt(int x) {
        return "--actions=move " + x + " 180; frame; dblclick; wait 6";
    }

    /** 三击落点脚本（脚本语法没有三击关键字，按三次跨帧 down/up 展开写）。 */
    private static String tripleClickAt(int x) {
        return "--actions=move " + x + " 180; frame; down; frame; up; frame; down; frame; up;"
                + " frame; down; frame; up; wait 6";
    }

    /** Ctrl+A 全选脚本：三击在单行控件上的独立 oracle（键盘路径，与指针路径无共用代码）。 */
    private static String selectAllAt(int x) {
        return "--actions=move " + x + " 180; frame; down; frame; up; frame; keydown CONTROL_LEFT;"
                + " frame; key A; frame; keyup CONTROL_LEFT; wait 6";
    }

    /**
     * 命令面契约：**批量轴的产物命名**与多档独立性。
     *
     * <p>守的交付：{@code --out} 的后缀规则是 agent 找产物的**唯一依据**（使用指南「参数」节承诺：
     * 矩阵出图按轴追加 {@code -pg<页面>} / {@code -p<下标>} / {@code -th<外观>} / {@code -bq<玻璃>} /
     * {@code -fs<百分比>} / {@code -WxH}，各段只在对应轴存在多档时出现）。它此前在 {@code src/test}
     * 零覆盖 —— 后缀改名、少加一段、或两档互相覆盖都不会红。</p>
     *
     * <h4>判据</h4>
     * <ul>
     *   <li>逐轴核对后缀段：尺寸段**恒进**（同一页面不同尺寸没有「缺省尺寸」可言）、字号段只在
     *       **偏离缺省水位**（100 = {@code FONT_SCALE_NONE_PERCENT}）时进、页面段只在多页面时进、
     *       下标 / 外观段在给出时进；</li>
     *   <li>单档**不加任何后缀**（既有命令的产物路径逐字不变）；</li>
     *   <li>各档产物摘要两两不同（档位真的改了像素，而不是同一请求出了两次）；</li>
     *   <li>批量汇总行报 {@code batch: N/N ok (逐档独立进程)} —— F31 的「产物只依赖请求」契约。</li>
     * </ul>
     */
    @Test
    public void matrixAxesNameArtifactsAndStayDistinct() throws Exception {
        assertArtifacts("linkage-matrix-sizes",
                new String[] {"--page=playground", "--page-index=1", "--sizes=640x360,1280x720"},
                new String[] {"linkage-matrix-sizes-p1-640x360.png", "linkage-matrix-sizes-p1-1280x720.png"});
        assertArtifacts("linkage-matrix-fs",
                new String[] {"--page=playground", "--page-index=1", "--font-scales=100,200"},
                new String[] {"linkage-matrix-fs-p1-1280x720.png", "linkage-matrix-fs-p1-fs200-1280x720.png"});
        assertArtifacts("linkage-matrix-theme",
                new String[] {"--page=playground", "--page-index=1", "--themes=liquid-glass-dark,solid-dark"},
                new String[] {"linkage-matrix-theme-p1-thliquid-glass-dark-1280x720.png",
                        "linkage-matrix-theme-p1-thsolid-dark-1280x720.png"});
        // 前缀刻意不取 linkage-matrix-page：sameStem 按 startsWith 匹配，会与下面的 linkage-matrix-pages
        // 互相命中（独立审查建议 4；当前靠用例顺序侥幸无事，改名后重排/并行都不再互删）。
        assertArtifacts("linkage-matrix-pidx",
                new String[] {"--page=playground", "--page-indexes=0,1"},
                new String[] {"linkage-matrix-pidx-p0-1280x720.png", "linkage-matrix-pidx-p1-1280x720.png"});
        assertArtifacts("linkage-matrix-pages",
                new String[] {"--pages=playground,text-probe"},
                new String[] {"linkage-matrix-pages-pgplayground-1280x720.png",
                        "linkage-matrix-pages-pgtext-probe-1280x720.png"});
        // 玻璃档轴：独立审查用变异实测「把 -bq 改成 -bqX 时本用例与 F40/F43 判据全绿」⇒ 指南
        // 「玻璃档后缀有门禁钉住」当时是空头承诺；本档把它补成真的（+2 次出图约 6 s）。
        assertArtifacts("linkage-matrix-bq",
                new String[] {"--page=glass", "--backdrop-qualities=full,eco"},
                new String[] {"linkage-matrix-bq-bqfull-1280x720.png", "linkage-matrix-bq-bqeco-1280x720.png"});
        assertArtifacts("linkage-matrix-single",
                new String[] {"--page=playground", "--page-index=1"},
                new String[] {"linkage-matrix-single.png"});
    }

    /**
     * 直启一次批量出图：核对**产物文件名集合**、**逐档摘要两两不同**与批量汇总行。
     *
     * @param name          用例内产物名前缀
     * @param args          命令行参数（不含 {@code --out}，由 {@link #runShot} 统一给出）
     * @param expectedNames 期望的产物文件名集合（顺序无关）
     */
    private static void assertArtifacts(String name, String[] args, String[] expectedNames) throws Exception {
        Shot shot = runShot(name, args);
        HeadlessShotGate.assumeEnvironmentAvailable(name, shot.exit, shot.output);
        Assert.assertEquals("headless 直启失败（" + name + " exit=" + shot.exit + "）：\n" + shot.output,
                0, shot.exit);
        List<Path> produced = sameStem(shot.outputPath);
        Set<String> names = new LinkedHashSet<String>();
        for (Path path : produced) {
            names.add(path.getFileName().toString());
        }
        Set<String> expected = new LinkedHashSet<String>(java.util.Arrays.asList(expectedNames));
        Assert.assertEquals("批量轴产物命名不符合契约（" + name + "）：\n" + shot.output, expected, names);
        Set<String> digests = digestsOf(produced);
        Assert.assertEquals("各档产物必须两两不同（同摘要 = 同一请求出了两次，或档位没接线）：" + digests
                + "\n" + shot.output, expectedNames.length, digests.size());
        if (expectedNames.length > 1) {
            Assert.assertTrue("批量必须逐档独立进程（F31：产物只依赖请求）：\n" + shot.output,
                    shot.output.contains("batch: " + expectedNames.length + "/" + expectedNames.length + " ok")
                            && shot.output.contains("逐档独立进程"));
        }
    }

    /**
     * 命令面契约：**帧计划三参数**与 **{@code --script=file}**。
     *
     * <p>守的交付：</p>
     * <ul>
     *   <li>{@code --frames=N} 是「最少帧数」而不是「固定帧数」—— 实际帧数必须 {@code ≥ N}，
     *       {@code --max-frames} 是读数分母；{@code maxFrames < frames} 属**参数错误**（exit 2 + 自证消息）；</li>
     *   <li>{@code --script=file} 与 {@code --actions} 同源：同一段脚本（含注释与分号分隔）从文件读进来
     *       必须与内联**同摘要**（比较口径是产物 sha256 前 8 字节，非逐字节全文件）；空文件等价于无脚本；
     *       文件读不到是**参数错误**而不是设施失败。</li>
     * </ul>
     */
    @Test
    public void framePlanAndScriptFileContracts() throws Exception {
        String framesOutput = render("linkage-frames", "--page=config", "--frames=30", "--max-frames=60");
        int[] plan = framesPlanOf(framesOutput);
        Assert.assertEquals("帧计划读数必须报 --max-frames 为分母：\n" + framesOutput, 60, plan[1]);
        Assert.assertTrue("--frames=30 是最少帧数，实际帧数必须 ≥ 30：\n" + framesOutput, plan[0] >= 30);

        // 「下界」而不是「恰好 N 帧」：--settle=1 时 config 页收敛在 5 与 40 之间（实测 28）——
        // 只断言 ≥30 区分不出「固定 30 帧」的实现（独立审查建议 1），故补这一档把下界语义与
        // --settle 参与收敛同时钉住（--settle= 此前在门禁里零使用）。
        String settleOutput = render("linkage-frames-settle", "--page=config", "--frames=5",
                "--settle=1", "--max-frames=40");
        int[] settled = framesPlanOf(settleOutput);
        Assert.assertEquals("帧计划读数必须报 --max-frames 为分母：\n" + settleOutput, 40, settled[1]);
        Assert.assertTrue("--frames=5 是下界：实际帧数必须 > 5（固定 5 帧的实现会红）：\n" + settleOutput,
                settled[0] > 5);

        Shot badPlan = runShot("linkage-frames-bad", "--page=playground", "--frames=9", "--max-frames=5");
        Assert.assertEquals("frames > max-frames 必须落参数错误（exit 2）：\n" + badPlan.output,
                2, badPlan.exit);
        Assert.assertTrue("参数错误必须自证成因：\n" + badPlan.output,
                badPlan.output.contains("maxFrames 不得小于 frames")
                        || badPlan.output.contains("maxFrames 不能小于 frames"));

        Path scriptFile = Paths.get("build", "reports", "headless", "linkage-script.qzscript").toAbsolutePath();
        Files.createDirectories(scriptFile.getParent());
        Files.write(scriptFile, "# 注释行\nmove 953 84; frame  # 行尾注释\nclick\nwait 8\n"
                .getBytes(StandardCharsets.UTF_8));
        try {
            String fromFile = renderDigest("linkage-script-file", "--page=playground",
                    "--script=" + scriptFile);
            String inline = renderDigest("linkage-script-inline", "--page=playground",
                    "--actions=move 953 84; frame; click; wait 8");
            Assert.assertEquals("--script=file 必须与内联 --actions 同摘要（sha256 前 8 字节）："
                    + fromFile + " vs " + inline, inline, fromFile);

            Path emptyFile = Paths.get("build", "reports", "headless", "linkage-script-empty.qzscript")
                    .toAbsolutePath();
            Files.write(emptyFile, new byte[0]);
            try {
                String emptyScript = renderDigest("linkage-script-empty", "--page=playground",
                        "--page-index=1", "--script=" + emptyFile);
                String noScript = renderDigest("linkage-script-none", "--page=playground", "--page-index=1");
                Assert.assertEquals("空脚本必须等价于无脚本：" + emptyScript + " vs " + noScript,
                        noScript, emptyScript);
            } finally {
                Files.deleteIfExists(emptyFile);
            }

            Shot missing = runShot("linkage-script-missing", "--page=playground",
                    "--script=" + scriptFile.resolveSibling("no-such-script.qzscript"));
            Assert.assertEquals("脚本文件不存在必须落参数错误（exit 2）：\n" + missing.output,
                    2, missing.exit);
            Assert.assertTrue("参数错误必须自证成因与路径：\n" + missing.output,
                    missing.output.contains("脚本文件读取失败")
                            && missing.output.contains("no-such-script"));
        } finally {
            Files.deleteIfExists(scriptFile);
        }
    }

    /**
     * 聊天**输入屏**形态的滚轮端到端判据：{@code ChatInputSurface} 的 SCROLL 路由真的驱动历史滚动，
     * 且 Shift 把幅度从「×7 行」降为「×1 行」（原版语义）。
     *
     * <p>守的交付：F48 边界登记把「{@code ChatInputSurface} 的 Shift+滚轮」列为「仅完整 classpath 可达、
     * 未覆盖」—— 设备层只钉事件载荷，控件不读修饰位时它仍全绿（与 F48 / F49 同族的盲区）。
     * 本用例装配的是**生产** {@code ChatInputSurface}（只注入输入源，不复制滚轮路由），故走完整集。</p>
     *
     * <p>为什么判据用读数而不是像素：本页 render 用墙钟驱动开合动画（生产语义），像素产物带相位；
     * {@code scroll: offset=N} 才是稳定的语义面。</p>
     *
     * <p>前置断言 {@code max >= 7} 不能省：内容不够长时 offset 会被 clamp 到 max，
     * 「×7 与 ×1」就退化成「max 与 1」，幅度语义静默丢失。</p>
     */
    @Test
    public void chatInputScrollHonoursShiftModifier() throws Exception {
        StringBuilder messages = new StringBuilder();
        for (int i = 1; i <= 16; i++) {
            if (i > 1) {
                messages.append(HeadlessRequest.CHAT_MESSAGE_SEPARATOR);
            }
            messages.append("Steve:滚动样本 ").append(i);
        }
        String[] base = {"--page=chat-input", "--size=1280x720", "--text=" + messages};

        String baseline = renderFull("linkage-chat-input-base", base);
        int[] baseState = scrollStateOf(baseline);
        Assert.assertTrue("chat-input 页必须打印滚动读数：\n" + baseline, baseState[0] >= 0);
        Assert.assertTrue("内容必须够长（否则 clamp 掩盖幅度语义）：max=" + baseState[1],
                baseState[1] >= 7);
        Assert.assertEquals("未滚动时 offset 必须是 0：\n" + baseline, 0, baseState[0]);

        String scrolled = renderFull("linkage-chat-input-scroll", withScript(
                "--actions=move 170 500; frame; scroll 10; wait 6", base));
        int[] scrolledState = scrollStateOf(scrolled);
        Assert.assertEquals("非 Shift 滚轮必须按 ×7 行滚动（clamp(±1) × scrollWheelLines）：\n" + scrolled,
                7, scrolledState[0]);

        String shifted = renderFull("linkage-chat-input-shift", withScript(
                "--actions=move 170 500; frame; keydown SHIFT_LEFT; frame; scroll 10; wait 6", base));
        int[] shiftedState = scrollStateOf(shifted);
        Assert.assertEquals("Shift 必须把幅度降为 ×1 行（原版语义）：\n" + shifted,
                1, shiftedState[0]);
        Assert.assertNotEquals("自检：Shift 档必须与非 Shift 档不同（否则修饰位没被读）："
                + scrolledState[0] + " vs " + shiftedState[0], scrolledState[0], shiftedState[0]);

        String backToBottom = renderFull("linkage-chat-input-back", withScript(
                "--actions=move 170 500; frame; scroll 10; frame; scroll -10; wait 6", base));
        Assert.assertEquals("反方向滚轮必须回到最新（offset 0）：\n" + backToBottom,
                0, scrollStateOf(backToBottom)[0]);
    }

    /** 从输出里取输入屏滚动读数 {@code {offset, max, visible}}；没有该行返回 {@code {-1, -1, -1}}。 */
    private static int[] scrollStateOf(String output) {
        Matcher matcher = SCROLL_STATE.matcher(output);
        if (!matcher.find()) {
            return new int[] {-1, -1, -1};
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))};
    }

    /** 从输出里取帧计划读数 {@code frames: N/M}；没有该行返回 {@code {-1, -1}}。 */
    private static int[] framesPlanOf(String output) {
        Matcher matcher = FRAMES_PLAN.matcher(output);
        if (!matcher.find()) {
            return new int[] {-1, -1};
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    /**
     * 出图必须逐字节确定（同一命令连跑三次）—— 设施的核心契约。
     *
     * <p>连跑**三次**而不是两次：独立审核用「宽度缓存预算改回 64」这一真实回归做剂量对照，
     * 测得基线在 24 次里只出现 9/9/2/4（基线值占比约 37%），两次采样的理论检出率偏低、三次更稳。</p>
     *
     * <p>帧时钟虚拟化（规划 F47）之前，这条判据只能覆盖「无输入脚本」的工况：带输入脚本时
     * 帧时间取 {@code System.nanoTime()}（真实耗时），动画 / caret 相位随机器负载漂，同一命令
     * 连跑 8 次得 4 种产物（差异是 16 px 的 caret 竖线相位族）。F47 把帧时间改成 runtime 端口后，
     * 带脚本工况也确定了 —— 见 {@link #scriptedShotIsByteIdenticalAcrossRuns()}。</p>
     */
    @Test
    public void scriptFreeShotIsByteIdenticalAcrossRuns() throws Exception {
        String first = renderDigest("linkage-determinism-a", "--page=playground", "--page-index=1");
        String second = renderDigest("linkage-determinism-b", "--page=playground", "--page-index=1");
        String third = renderDigest("linkage-determinism-c", "--page=playground", "--page-index=1");
        Assert.assertEquals("无脚本：连跑三次必须逐字节相同（第二 vs 第三）：", second, third);
        Assert.assertEquals("无脚本：连跑三次必须逐字节相同（第一 vs 第二）：", first, second);
    }

    /**
     * **带输入脚本**的出图同样必须逐字节确定 —— 这是 F47 帧时钟虚拟化的直接成果。
     *
     * <p>脚本驱动到焦点 / 插入符动画：修复前同一命令连跑 8 次得 4 种产物（差异恒为
     * {@code x=359, y=172..187} 的 16 px caret 竖线，值域是一族连续的 alpha 相位）；修复后
     * （{@code SceneRuntime.__useVirtualFrameClock}，headless 注入「{@code --clock} 基准 + 帧序号 × 16 ms」）
     * 实测 8/8 同一哈希。判据连跑三次钉住它 —— 插入符相位一旦重新回到真实时钟就会红。</p>
     */
    @Test
    public void scriptedShotIsByteIdenticalAcrossRuns() throws Exception {
        String actions = "--actions=move 300 180; frame; down; frame; up; frame; move 500 180;"
                + " frame; down; frame; up; wait 6";
        // 连跑五次而不是三次：修复前的产物分布是「多种相位」（实测 8 次 4 种，最大占比 3/8），
        // 三次采样仍有约 11% 的漏检率（本轮用它做变异时实测漏检过一次），五次把漏检压到约 1.5%。
        String reference = renderDigest("linkage-scripted-0", "--page=playground", "--page-index=1", actions);
        for (int run = 1; run <= 4; run++) {
            Assert.assertEquals("带脚本：连跑五次必须逐字节相同（第 " + run + " 次 vs 第 0 次）：",
                    reference, renderDigest("linkage-scripted-" + run, "--page=playground",
                            "--page-index=1", actions));
        }
    }

    /**
     * 查询路径必须**不产出 PNG**：它们只推进若干帧拿布局再投影，产物集合属于出图请求。
     *
     * <p>缺这条判据时，把查询路径改成顺手写一张图不会有任何测试变红（它自己的契约无人守），
     * 后果是批量出图的产物集合被污染。它是 {@link #queryPathsDoNotProduceAShot()} 之外
     * {@code renderQuery} 注释里那句「不产出 PNG」的判据化。</p>
     */
    @Test
    public void queryPathsDoNotProduceAShot() throws Exception {
        renderQuery("linkage-query-only", "--page=playground", "--find=Markdown");
        List<Path> produced = sameStem(Paths.get("build", "reports", "headless", "linkage-query-only.png")
                .toAbsolutePath());
        Assert.assertTrue("查询路径不得产出 PNG（实测产物：" + produced + "）", produced.isEmpty());
    }

    /**
     * 查询路径（{@code --find} / {@code --center} / {@code --nodes}）**不产出 PNG**：
     * 只判退出码，不要求产物存在（使用文档写明「不产出 PNG」是它们的契约）。
     */
    private static String renderQuery(String name, String... extraArgs) throws Exception {
        Shot shot = runShot(name, extraArgs);
        HeadlessShotGate.assumeEnvironmentAvailable(name, shot.exit, shot.output);
        Assert.assertEquals("headless 查询路径失败（" + name + " exit=" + shot.exit + "）：\n" + shot.output,
                0, shot.exit);
        return shot.output;
    }

    /**
     * 用<b>完整集</b> classpath 直启一次出图：chat / hud 触及 {@code net.minecraft.*}，
     * 最小集下装配即失败（那是判据 {@code missingMinecraftDependencyReportsContractExitCode} 的工况）。
     * 完整集未产出时跳过（环境不具备，不算失败）。
     */
    private static String renderFull(String name, String... extraArgs) throws Exception {
        Path full = fullClasspathFile();
        Assume.assumeTrue("完整集 classpath 未产出（chat/hud 需要 patchedMc 类），跳过："
                + System.getProperty("qz.headless.classpathFile", ""), full != null);
        Shot shot = runShotWith(full, name, extraArgs);
        HeadlessShotGate.assumeEnvironmentAvailable(name, shot.exit, shot.output);
        Assert.assertEquals("headless 直启失败（" + name + " exit=" + shot.exit + "）：\n" + shot.output,
                0, shot.exit);
        List<Path> produced = sameStem(shot.outputPath);
        Assert.assertFalse("未产出 PNG（" + name + "）：\n" + shot.output, produced.isEmpty());
        return shot.output;
    }

    /** 直启用的完整集 classpath 文件（与最小集同目录）；文件不存在返回 null。 */
    private static Path fullClasspathFile() {
        String classpathFile = System.getProperty("qz.headless.classpathFile", "");
        if (classpathFile.isEmpty()) {
            return null;
        }
        Path full = Paths.get(classpathFile).resolveSibling("classpath-full.txt");
        return Files.isRegularFile(full) ? full : null;
    }

    /** 产物集合的内容摘要（前 8 字节 sha256），用来钉「档位真的改了像素」。 */
    private static Set<String> digestsOf(List<Path> paths) throws Exception {
        Set<String> digests = new LinkedHashSet<String>();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        for (Path path : paths) {
            digest.reset();
            byte[] bytes = digest.digest(Files.readAllBytes(path));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
                hex.append(Character.forDigit(bytes[i] & 0xF, 16));
            }
            digests.add(hex.toString());
        }
        return digests;
    }

    /** 直启一次出图并返回进程输出；退出码非 0 即断言失败（附完整输出便于定因）。 */
    private static String render(String name, String... extraArgs) throws Exception {
        Shot shot = runShot(name, extraArgs);
        // 运行环境不具备（无 GL / natives 加载不了）时跳过，其余非 0 一律红。
        HeadlessShotGate.assumeEnvironmentAvailable(name, shot.exit, shot.output);
        Assert.assertEquals("headless 直启失败（" + name + " exit=" + shot.exit + "）：\n" + shot.output,
                0, shot.exit);
        // 单档落主名、多档落带轴后缀的名字，故只要求「同前缀至少一个产物」。
        List<Path> produced = sameStem(shot.outputPath);
        Assert.assertFalse("未产出 PNG（" + name + "）：\n" + shot.output, produced.isEmpty());
        return shot.output;
    }

    /**
     * 直启一次出图并返回结果，**不判定退出码** —— 供「预期失败」的判据使用。
     *
     * <p>与 {@link #render} 共用同一条直启链路（同 classpath 注入、同产物清理、同输出捕获），
     * 差别只有「谁来判退出码」：正常路径判 0，契约判据自己判期望值。</p>
     *
     * @param name      用例内产物名
     * @param extraArgs 追加参数
     * @return 退出码 + 完整输出 + 期望产物路径
     */
    private static Shot runShot(String name, String... extraArgs) throws Exception {
        String classpathFile = System.getProperty("qz.headless.classpathFile", "");
        Assume.assumeTrue("未注入 headless 直启 classpath，跳过页面可链接性门禁",
                !classpathFile.isEmpty() && new File(classpathFile).isFile());
        return runShotWith(Paths.get(classpathFile), name, extraArgs);
    }

    /** 直启实现：classpath 由调用方给（最小集 / 完整集）。 */
    private static Shot runShotWith(Path classpath, String name, String... extraArgs) throws Exception {
        String nativesDir = System.getProperty("qz.headless.nativesDir", "");

        Path out = Paths.get("build", "reports", "headless", name + ".png").toAbsolutePath();
        Files.createDirectories(out.getParent());
        for (Path stale : sameStem(out)) {
            Files.deleteIfExists(stale);
        }

        String javaExecutable = Paths.get(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java").toString();
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable);
        command.add("-Djava.library.path=" + nativesDir);
        command.add("-Xmx2g");
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dsun.stdout.encoding=UTF-8");
        command.add("-Dsun.stderr.encoding=UTF-8");
        command.add("-cp");
        command.add(new String(Files.readAllBytes(classpath), StandardCharsets.UTF_8).trim());
        command.add("blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless.HeadlessShotMain");
        for (String one : extraArgs) {
            command.add(one);
        }
        command.add("--size=1280x720");
        command.add("--out=" + out);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String text;
        InputStream stream = process.getInputStream();
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
        int exit = process.waitFor();
        return new Shot(exit, text, out);
    }

    /** 一次直启的结果：退出码 + 完整输出 + 期望产物路径。 */
    private static final class Shot {
        private final int exit;
        private final String output;
        private final Path outputPath;

        Shot(int exit, String output, Path outputPath) {
            this.exit = exit;
            this.output = output;
            this.outputPath = outputPath;
        }
    }

    /** 与给定产物同前缀的 PNG（多档矩阵下会有多个）。 */
    private static List<Path> sameStem(Path out) throws Exception {
        String fileName = out.getFileName().toString();
        final String prefix = fileName.endsWith(".png")
                ? fileName.substring(0, fileName.length() - 4) : fileName;
        List<Path> found = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(out.getParent())) {
            for (Path path : (Iterable<Path>) stream.filter(candidate -> {
                String candidateName = candidate.getFileName().toString();
                return candidateName.startsWith(prefix) && candidateName.endsWith(".png");
            })::iterator) {
                found.add(path);
            }
        }
        return found;
    }

    /** 当前 {@code java.io.tmpdir} 下的临时配置目录名（用前后差集判定，避免算进并发进程的目录）。 */
    private static Set<String> tempConfigDirs() throws Exception {
        Set<String> found = new LinkedHashSet<String>();
        Path root = Paths.get(System.getProperty("java.io.tmpdir", "."));
        if (!Files.isDirectory(root)) {
            return found;
        }
        try (Stream<Path> stream = Files.list(root)) {
            for (Path path : (Iterable<Path>) stream.filter(candidate -> candidate.getFileName().toString()
                    .startsWith(TEMP_DIR_PREFIX))::iterator) {
                found.add(path.getFileName().toString());
            }
        }
        return found;
    }

    /** 统计子串在输出里出现的次数。 */
    private static int countOf(String output, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = output.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    /** 从输出里取命令面摘要的段流条数；没有该行返回 -1。 */
    private static int segmentsOf(String output) {
        Matcher matcher = SEGMENTS.matcher(output);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /** 直启一次出图并返回**产物内容摘要**（单档时即该 PNG 的前 8 字节 sha256）。 */
    private static String renderDigest(String name, String... extraArgs) throws Exception {
        Shot shot = runShot(name, extraArgs);
        HeadlessShotGate.assumeEnvironmentAvailable(name, shot.exit, shot.output);
        Assert.assertEquals("headless 直启失败（" + name + " exit=" + shot.exit + "）：\n" + shot.output,
                0, shot.exit);
        Set<String> digests = digestsOf(sameStem(shot.outputPath));
        Assert.assertEquals("单档出图应恰好一个产物：\n" + shot.output, 1, digests.size());
        return digests.iterator().next();
    }

    /** 节点事实表里的「地址 → 绝对 y」（同一地址只出现一次）。 */
    private static Map<String, Integer> nodeYs(String output) {
        Map<String, Integer> ys = new LinkedHashMap<String, Integer>();
        Matcher matcher = NODE_LINE.matcher(output);
        while (matcher.find()) {
            ys.put(matcher.group(1), Integer.valueOf(matcher.group(2)));
        }
        return ys;
    }

    /** 从输出里取 picker 结果条数（节点事实表里的 "Search results (N)"）；没有该行返回 -1。 */
    private static int searchResultsOf(String output) {
        Matcher matcher = SEARCH_RESULTS.matcher(output);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /** 从输出里取输入读数里的派发事件数；没有该行返回 -1。 */
    private static int dispatchedOf(String output) {
        Matcher matcher = DISPATCHED.matcher(output);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /** 从输出里取命令面摘要的命令数；没有该行返回 -1。 */
    private static int commandsOf(String output) {
        Matcher matcher = COMMANDS.matcher(output);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
