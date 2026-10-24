package blocamlimb.nhx100218.modernui.enhance.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import blocamlimb.nhx100218.modernui.enhance.tooltip.TooltipShaderProgram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * 启动预热：在 Forge 加载界面（本模组加载完成之后）执行一次，把字体字形、tooltip 着色器预热掉，
 * 把「首次打开界面/首次渲染」的卡顿前移到加载阶段。
 *
 * <p>由 {@code ClientProxy.postInit} 布防，{@link #onRenderTick()} 在加载界面期间首次触发。
 * 字形预热通过解析并绘制一段覆盖 ASCII + 本模组本地化文本 + 常用界面/物品中文/符号的样本触发
 * ModernUI 的 SDF 字形生成；样本绘制在屏幕外，不显示。GL 状态用 attrib 栈整体恢复，不污染加载界面。</p>
 */
public final class PreloadService {

    /** 配置开关（配置页 performance.preload）。 */
    public static volatile boolean enabled = true;

    private static volatile boolean armed;
    private static boolean done;
    private static int ticks;
    private static String sample;

    private PreloadService() {}

    /** 本模组加载完成后布防（{@code ClientProxy.postInit}）。 */
    public static void arm() {
        armed = true;
    }

    /** 每帧（渲染 tick 结束）尝试执行一次。 */
    public static void onRenderTick() {
        if (!armed || done || !enabled) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) {
            return;
        }
        if (++ticks < 3) {
            return;
        }
        done = true;
        run(mc);
    }

    private static void run(Minecraft mc) {
        long start = System.currentTimeMillis();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            // 1) tooltip 圆角着色器：提前编译/链接。
            TooltipShaderProgram.get().isAvailable();
            // 2) 字形：先取宽度（触发字形度量/生成），再屏幕外绘制。
            FontRenderer font = mc.fontRenderer;
            String text = sample();
            font.getStringWidth(text);
            GL11.glTranslatef(0.0F, -10000.0F, 0.0F);
            for (int i = 0; i < text.length(); i += 512) {
                String chunk = text.substring(i, Math.min(text.length(), i + 512));
                font.drawStringWithShadow(chunk, 0, 0, 0xFFFFFF);
            }
        } catch (Throwable failure) {
            MyMod.LOG.warn("预热失败", failure);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        MyMod.LOG.info("界面/字体预热完成，用时 {} ms", Long.valueOf(System.currentTimeMillis() - start));
    }

    /** 样本：ASCII + 本模组 lang 资源里的字符 + 常用中文/符号。 */
    private static String sample() {
        if (sample != null) {
            return sample;
        }
        Set<Character> chars = new LinkedHashSet<Character>();
        for (char c = 0x20; c <= 0x7E; c++) {
            chars.add(Character.valueOf(c));
        }
        chars.add('\u00a7'); // 颜色码前缀
        for (char c : COMMON_CJK.toCharArray()) {
            chars.add(Character.valueOf(c));
        }
        loadLang(chars, "/assets/modernui/lang/zh_CN.lang");
        loadLang(chars, "/assets/modernui/lang/en_US.lang");
        StringBuilder builder = new StringBuilder(chars.size() + 8);
        for (Character c : chars) {
            builder.append(c.charValue());
        }
        sample = builder.toString();
        return sample;
    }

    private static void loadLang(Set<Character> chars, String resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(PreloadService.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) {
                    chars.add(Character.valueOf(line.charAt(i)));
                }
            }
        } catch (Throwable ignored) {
            // 资源缺失时忽略
        }
    }

    /** 常用界面/物品相关汉字（覆盖 GTNH 常见 UI）。 */
    private static final String COMMON_CJK = "物品方块合成任务进度设置返回关闭打开完成取消应用主界面世界服务器玩家生命魔法能量存储输入输出"
            + "数量耐久等级经验护甲伤害攻击防御速度范围半径温度压力电网流体液体气体固体机器设备工具装备武器防具"
            + "矿石金属锭板粒粉齿轮管道线缆电路芯片模块升级降级版本配置选项语言声音音量画面渲染光影材质模型动画平滑"
            + "滚轮鼠标键盘快捷栏聊天头像消息发送接收时间日期天气生物实体掉落拾取背包容器箱子漏斗反应堆锅炉发电";
}
