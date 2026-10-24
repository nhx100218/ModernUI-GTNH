package blocamlimb.nhx100218.modernui.uilib.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;

/**
 * attrib 帧与 GL 状态的<b>排错插桩</b>：只记日志，不改语义、不发会置错误码的查询。
 *
 * <p>用途（GL 自净审查 N13 / R11）：N13 的触发前提是 Angelica/lwjgl3ify 的 Core Profile——那一档下
 * attrib 栈是否可用、最先在哪一处失败，离线判不了；R11 的「push 成功却报错」也需要现场证据才能定性。
 * 本类在失败发生点立刻写日志，使真机问题能按「站点 + mask + 栈深度差 + 异常文本」一眼定位。</p>
 *
 * <p><b>纪律</b>：① 只做 {@code GlAttribDepth} 的反射读（它不发 GL 查询、不置错误码）与
 * {@code glGetString} / LWJGL 已解析的能力位；<b>绝不读 {@code glGetError}</b>——那会排空错误队列，
 * 破坏仓内「入口 GL error 必须为空」与字符页闸门的契约；② 四个 push/pop 包装失败时一律<b>原样重抛</b>
 * （同一异常实例），插桩不改控制流；③ 同一 (类别, 站点) 只 WARN 一次，首次带堆栈，避免每帧刷屏
 * （与 {@link GlAttribDepth} 的 warn-once 同口径）；④ 环境指纹每进程只尝试一次。</p>
 *
 * <p><b>排查入口</b>：真机出现状态污染或上传失败时，按日志标签定位——
 * {@code glPushAttrib(...) 失败于 <站点>}（N13，含栈深度差与 cause）、
 * {@code glPopAttrib 失败于 <站点>}（栈失衡）、
 * {@code 字符页事务闸门失败：phase=… glError=0x… entry=…}（R11，含进入时排空到的错误对照）、
 * {@code GL 环境指纹（…）}（环境登记，含 profile mask 与能力档位）。</p>
 *
 * <p><b>明确不在插桩范围内</b>：① 矩阵栈（{@code glPushMatrix/glPopMatrix}，约 15 处）——它与 attrib 栈
 * 同属 Core Profile 移除项，但失败形态不同（实测为 {@code GL_STACK_OVERFLOW}），需要时按同一模式另包；
 * ② 「调用不抛异常、只在队列里置错误码」的形态——检测它必须读 {@code glGetError}，与纪律①冲突；
 * 该形态目前只在字符页事务里有闸门（见 {@code GlyphPage.requireNoGlError}），其它站点无锚点，
 * 真机上表现为后续 entry 相位的排空日志。</p>
 */
public final class GlStateDiagnostics {

    private static final Logger LOG = LogManager.getLogger("ModernUI/GlStateDiagnostics");
    /** warn-once 记账；同步集合，跨线程调用也不会破坏它。 */
    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<String>());
    private static boolean environmentLogged;
    /** 帧序号：由帧围栏 capture 推进，日志里带 frame=N 便于把同一帧内的多条记录串起来（渲染单线程）。 */
    private static int frameSequence;

    private GlStateDiagnostics() {
    }

    /** 记录一次 attrib 栈压入；失败时留痕（栈深度差 + cause + 首次堆栈）并原样重抛。 */
    public static void pushAttrib(int mask, String site) {
        int depthBefore = GlAttribDepth.current();
        try {
            GL11.glPushAttrib(mask);
        } catch (RuntimeException failure) {
            warnOnce("push-attrib@" + site, describePush("glPushAttrib", mask, site, depthBefore, failure), failure);
            throw failure;
        } catch (Error failure) {
            warnOnce("push-attrib@" + site, describePush("glPushAttrib", mask, site, depthBefore, failure), failure);
            throw failure;
        }
    }

    /** 记录一次 client attrib 栈压入；失败时留痕并原样重抛。 */
    public static void pushClientAttrib(int mask, String site) {
        int depthBefore = GlAttribDepth.current();
        try {
            GL11.glPushClientAttrib(mask);
        } catch (RuntimeException failure) {
            warnOnce("push-client-attrib@" + site,
                    describePush("glPushClientAttrib", mask, site, depthBefore, failure), failure);
            throw failure;
        } catch (Error failure) {
            warnOnce("push-client-attrib@" + site,
                    describePush("glPushClientAttrib", mask, site, depthBefore, failure), failure);
            throw failure;
        }
    }

    /** 记录一次 attrib 栈弹出；失败时留痕（深度差 + cause）并原样重抛。 */
    public static void popAttrib(String site) {
        int depthBefore = GlAttribDepth.current();
        try {
            GL11.glPopAttrib();
        } catch (RuntimeException failure) {
            warnOnce("pop-attrib@" + site, describePop("glPopAttrib", site, depthBefore, failure), failure);
            throw failure;
        } catch (Error failure) {
            warnOnce("pop-attrib@" + site, describePop("glPopAttrib", site, depthBefore, failure), failure);
            throw failure;
        }
    }

    /** 记录一次 client attrib 栈弹出；失败时留痕并原样重抛。 */
    public static void popClientAttrib(String site) {
        int depthBefore = GlAttribDepth.current();
        try {
            GL11.glPopClientAttrib();
        } catch (RuntimeException failure) {
            warnOnce("pop-client-attrib@" + site, describePop("glPopClientAttrib", site, depthBefore, failure), failure);
            throw failure;
        } catch (Error failure) {
            warnOnce("pop-client-attrib@" + site, describePop("glPopClientAttrib", site, depthBefore, failure), failure);
            throw failure;
        }
    }

    /**
     * R11 诊断：字符页事务内相位的 GL 错误闸门失败。
     *
     * <p>{@code phase} 以 {@code _attrib_push} / {@code _client_attrib_push} 结尾时，这就是「push 调用成功、
     * 队列里却仍有错误」的现场。日志带三项对照，使来源可判：① 进入该事务时 drain 到的首个错误码与个数
     * （{@code entry=0x0/0} 即"进入时干净"⇒ 错误产生于本事务内，含本次 push）；② 本次读到的错误码；
     * ③ phase 名（区分 batch/upload/rollback/texture_init 四个事务）。</p>
     *
     * @param phase 相位名（事务内唯一）
     * @param glError 该相位读到的 GL 错误码
     * @param entryError 进入本事务时 drainEntryGlError 排空到的首个错误码（0 表示进入时干净）
     * @param entryDrained 进入本事务时排空的错误个数
     */
    public static void warnGlyphGateFailure(String phase, int glError, int entryError, int entryDrained) {
        warnOnce("glyph-gate@" + phase, "字符页事务闸门失败：frame=" + frameSequence + " phase=" + phase + " glError=0x"
                + Integer.toHexString(glError) + " entry=0x" + Integer.toHexString(entryError) + "/" + entryDrained
                + entryHint(entryError, entryDrained) + pushPhaseHint(phase), null);
    }

    /**
     * 环境指纹：每进程一次，真机排错的第一手信息。
     *
     * <p>字段：{@code GL_VERSION/GL_RENDERER/GL_VENDOR}（用 {@code glGetString}，Core Profile 下仍合法）、
     * 能力档位（OpenGL13/15/20/30/32 + 供围栏门控用的 ARB 位）、profile mask（仅在有 GL3.2 档位时查询）、
     * 以及 {@code GlAttribDepth} 是否反射到 Angelica 的深度入口。注意 {@code attribDepthReadable=true}
     * 只证明反射到了入口，<b>不</b>等于真实 GL attrib 栈可用。</p>
     */
    public static void logEnvironmentOnce(String site) {
        if (environmentLogged) {
            return;
        }
        try {
            ContextCapabilities capabilities = GLContext.getCapabilities();
            LOG.info("GL 环境指纹（{}）：version={} renderer={} vendor={} profileMask={} caps[GL13={} GL15={} GL20={}"
                            + " GL30={} GL32={} ARB_framebuffer_object={} ARB_vertex_array_object={}"
                            + " ARB_vertex_buffer_object={} ARB_shader_objects={}] attribDepthReadable={}",
                    site, glString(GL11.GL_VERSION), glString(GL11.GL_RENDERER), glString(GL11.GL_VENDOR),
                    profileMask(capabilities), Boolean.toString(capabilities.OpenGL13),
                    Boolean.toString(capabilities.OpenGL15), Boolean.toString(capabilities.OpenGL20),
                    Boolean.toString(capabilities.OpenGL30), Boolean.toString(capabilities.OpenGL32),
                    Boolean.toString(capabilities.GL_ARB_framebuffer_object),
                    Boolean.toString(capabilities.GL_ARB_vertex_array_object),
                    Boolean.toString(capabilities.GL_ARB_vertex_buffer_object),
                    Boolean.toString(capabilities.GL_ARB_shader_objects),
                    Boolean.toString(GlAttribDepth.current() >= 0));
            // 采集成功后才记账：首次因"无 context / shim 缺类"失败时，下一次捕获还能再试一次。
            environmentLogged = true;
        } catch (RuntimeException failure) {
            warnOnce("environment@" + site, "GL 环境指纹采集失败（" + site + "）：" + failure, failure);
        } catch (Error failure) {
            warnOnce("environment@" + site, "GL 环境指纹采集失败（" + site + "）：" + failure, failure);
        }
    }

    /** 帧序号推进（帧围栏 capture 调用一次/帧）；日志里的 {@code frame=N} 用于同帧记录关联。 */
    public static void beginFrame() {
        frameSequence++;
    }

    /** 测试可见的 warn-once 记账（诊断缝，不参与生产逻辑；返回内部可变集合，仅供测试清理与断言）。 */
    static Set<String> __warnedKinds() {
        return WARNED;
    }

    /** profile mask（0x9126）；无 GL3.2 档位时不查询——core/profile 未知时该 pname 可能置错误码。 */
    private static String profileMask(ContextCapabilities capabilities) {
        if (!capabilities.OpenGL32) {
            return "(unknown:no-gl32)";
        }
        try {
            return "0x" + Integer.toHexString(GL11.glGetInteger(GL32.GL_CONTEXT_PROFILE_MASK));
        } catch (RuntimeException failure) {
            return "(unavailable)";
        } catch (Error failure) {
            return "(unavailable)";
        }
    }

    private static String describePush(String call, int mask, String site, int depthBefore, Throwable failure) {
        return call + "(mask=0x" + Integer.toHexString(mask) + ") 失败于 " + site + " frame=" + frameSequence
                + " depth=" + depthBefore + "->" + GlAttribDepth.current()
                + depthDeltaHint(depthBefore) + " cause=" + failure
                + "（GL 自净审查 N13：Core Profile 下 attrib 栈不可用会在首次调用处暴露；"
                + "调用方随后按各自契约回滚或失败累积）";
    }

    private static String describePop(String call, String site, int depthBefore, Throwable failure) {
        return call + " 失败于 " + site + " frame=" + frameSequence
                + " depth=" + depthBefore + "->" + GlAttribDepth.current()
                + " cause=" + failure
                + "（栈可能已失衡：后续 attrib 帧会连带失败，请对照同一帧内更早的 push 日志）";
    }

    private static String depthDeltaHint(int depthBefore) {
        int depthAfter = GlAttribDepth.current();
        if (depthBefore < 0 || depthAfter < 0) {
            return "";
        }
        if (depthAfter > depthBefore) {
            return "（深度已增加 ⇒ 调用在抛异常前已生效：本帧会多出一层，属 R11 一类「生效却报错」现场）";
        }
        if (depthAfter < depthBefore) {
            return "（深度反而减少 ⇒ 栈已失衡，需回看本帧更早的 push/pop）";
        }
        return "（深度未变 ⇒ 调用未生效，调用方可安全按「未压入」处理）";
    }

    private static String entryHint(int entryError, int entryDrained) {
        if (entryError == 0 && entryDrained == 0) {
            return "（entry=clean：错误产生于本事务之内，含本次 push 自身或本事务的其它 GL 调用）";
        }
        return "（entry 已有污染：错误可能只是进入前第三方遗留，需与本次 push 的 mask 对照）";
    }

    private static String pushPhaseHint(String phase) {
        if (phase.endsWith("_attrib_push") || phase.endsWith("_client_attrib_push")) {
            return "（push 相位的闸门失败 ⇒ R11 现场：结合 entry 对照与深度差判断是第三方遗留还是本次 push）";
        }
        return "";
    }

    private static String glString(int name) {
        try {
            String value = GL11.glGetString(name);
            return value == null ? "(unknown)" : value;
        } catch (RuntimeException failure) {
            return "(unavailable)";
        } catch (Error failure) {
            return "(unavailable)";
        }
    }

    private static void warnOnce(String key, String message, Throwable failure) {
        if (!WARNED.add(key)) {
            return;
        }
        // 插桩绝不顶替原异常：日志系统本身抛错时静默放弃这条日志，调用方的异常照常抛出。
        try {
            if (failure == null) {
                LOG.warn(message);
            } else {
                // 首次带堆栈：真机排错要的就是"哪一行先炸"；消息里另带 cause= 文本，
                // 不依赖 appender 是否渲染 throwable。
                LOG.warn(message, failure);
            }
        } catch (Throwable ignored) {
            // 见上：诊断失败不改变业务语义。
        }
    }
}
