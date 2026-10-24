package blocamlimb.nhx100218.modernui.uilib.gl.shader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * GLSL 资源源字符集门禁：<b>着色器源必须是纯 ASCII 且不带 BOM</b>。
 *
 * <h3>为什么这条门禁存在（一次真实的 16 天静默降级）</h3>
 * <p>GLSL 1.20 的源字符集是 ASCII（ISO/IEC 646）。中文注释在多数驱动上"看起来没事"，
 * 但 NVIDIA 编译器直接报语法错误：</p>
 *
 * <pre>
 * 0(19) : error C0000: syntax error, unexpected $undefined at token "&lt;undefined&gt;"
 * </pre>
 *
 * <p>实测（2026-09-18，headless 出图设施定位）：<b>程序级</b>探针（编译 + 链接 + {@code glValidateProgram}）
 * 下 {@code uiBackdropF.frag} 注入抽头预算宏后不可用；把源里的非 ASCII 逐字符换成 {@code ?} 后
 * <b>立刻恢复可用</b> —— 即失败与着色器逻辑无关，只与注释里的多字节字符有关。回溯 git 发现
 * {@code 2026-09-02} 的 {@code ac1582a2} 首次把中文注释写进该 frag（此后 16 个版本里非 ASCII 行数
 * 从 5 涨到 127），也就是说：<b>从那一天起 backdrop shader 从未编译成功过</b>，玻璃一直静默降级到
 * 固定管线（仅模糊、无 vibrancy / 亮边 / 噪点 / 液态折射），而这段代码在此期间被反复"调参验收"。</p>
 *
 * <p><b>非 ASCII 落在哪个阶段都致命</b>（独立复核的程序级实测，推翻了作者初版"vert 中文无害"的推断）：
 * 中文 vert + ASCII frag 时，两个 stage <b>各自</b> {@code glCompileShader} 都返回成功，但整个 program
 * <b>链接失败</b>（{@code LINK-FAIL Vertex info}）—— NVIDIA 把完整编译推迟到 link 阶段。故判据必须覆盖
 * <b>整份源</b>：按阶段分别"看起来没事"不能作为放行依据，本类扫源文件全量字符正是这个理由。</p>
 *
 * <p>失败之所以能藏 16 天，是因为降级是<b>设计内</b>行为（{@code shader-unavailable -> fixed-pipeline}），
 * 日志不报错、画面只是"没那么好看"。所以判据不能靠出图观感，只能靠源字符集这一硬事实。</p>
 *
 * <h3>为什么判据放在源目录而不是类路径资源</h3>
 * <p>类路径上的资源可能来自缓存或旧构建产物，扫描源目录才能在下一次提交前拦住问题。
 * 构建产物是源目录的镜像，扫源即扫产物。</p>
 */
public class GlslSourceAsciiGuardTest {

    /** 着色器源目录（相对工程根，Gradle test 的工作目录即工程根）。 */
    private static final Path SHADER_DIR = Paths.get("src", "main", "resources", "shader");

    /** UTF-8 BOM：会让 {@code #version} 不再是首个 token，同样表现为语法错误。 */
    private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /** 着色器源必须是纯 ASCII（含 BOM 检查）。 */
    @Test
    public void shaderSourcesArePureAsciiWithoutBom() throws IOException {
        List<Path> sources = shaderSources();
        Assert.assertFalse("未在 " + SHADER_DIR.toAbsolutePath() + " 下找到任何 .frag/.vert —— "
                + "目录改名会让本门禁静默空转，故这里直接判失败", sources.isEmpty());

        List<String> violations = new ArrayList<String>();
        for (Path source : sources) {
            byte[] bytes = Files.readAllBytes(source);
            if (startsWith(bytes, UTF8_BOM)) {
                violations.add(source.getFileName() + ": 以 UTF-8 BOM 开头");
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                for (int c = 0; c < line.length(); c++) {
                    char ch = line.charAt(c);
                    if (ch > 127) {
                        violations.add(String.format(Locale.ROOT, "%s:%d: 非 ASCII 字符 U+%04X —— %s",
                                source.getFileName(), i + 1, (int) ch, line.trim()));
                        break;
                    }
                }
            }
        }
        Assert.assertTrue("GLSL 源含非 ASCII 字符或 BOM：NVIDIA 编译器会直接报 "
                + "\"error C0000: syntax error, unexpected $undefined\" 并让整个程序不可用，"
                + "玻璃静默降级到固定管线（实测发生过 16 天，见类注释）。注释请用英文：\n"
                + String.join("\n", violations), violations.isEmpty());
    }

    /** 目录下的 {@code .frag} / {@code .vert} 源文件（按文件名排序，便于稳定报错）。 */
    private static List<Path> shaderSources() throws IOException {
        if (!Files.isDirectory(SHADER_DIR)) {
            return new ArrayList<Path>();
        }
        List<Path> found = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(SHADER_DIR)) {
            for (Path path : (Iterable<Path>) stream.filter(candidate -> {
                String name = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".frag") || name.endsWith(".vert");
            })::iterator) {
                found.add(path);
            }
        }
        found.sort(null);
        return found;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
