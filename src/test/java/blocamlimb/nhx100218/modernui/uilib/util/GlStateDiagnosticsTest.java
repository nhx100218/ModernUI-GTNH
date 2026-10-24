package blocamlimb.nhx100218.modernui.uilib.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.opengl.GL11;

/**
 * attrib 帧排错插桩（N13/R11）的行为测试。
 *
 * <p>三条主张各有一条判据：① 失败<b>原样重抛</b>；② 同一站点只 WARN 一次，且首条日志带
 * 站点 + mask + cause 文本；③ 闸门日志按 phase 记账（供 R11 来源对照）。</p>
 *
 * <p>环境前提：测试 JVM 不带 LWJGL2（build.gradle.kts 的进程外直启设计）。判据写成「直调 GL11 vs 经插桩」
 * 的对比而不是硬编码异常类型——测试 classpath 变化时不会以无关原因变红。</p>
 */
public class GlStateDiagnosticsTest {

    private static final String LOGGER_NAME = "ModernUI/GlStateDiagnostics";

    @Test
    public void pushFailureIsRethrownUnchanged() {
        Throwable raw = captureRawPushFailure();
        Throwable wrapped = captureWrappedPushFailure("unit-test-rethrow");

        if (raw == null) {
            Assert.assertNull("直调成功时插桩也不得失败", wrapped);
            return;
        }
        // 实例不能比：底层失败（此处是类解析失败）每次调用都会重新产生一个实例。
        // 可观测的等价判据是"类型与消息都不得被插桩改写"——包装成别的异常类型即红。
        Assert.assertEquals("插桩不得把原失败换成别的类型", raw.getClass(), wrapped.getClass());
        Assert.assertEquals("插桩不得改写失败消息", raw.getMessage(), wrapped.getMessage());
    }

    @Test
    public void popFailureIsRethrownUnchanged() {
        Throwable raw = captureRawPopFailure();
        Throwable wrapped = null;
        try {
            GlStateDiagnostics.popAttrib("unit-test-rethrow-pop");
        } catch (Throwable failure) {
            wrapped = failure;
        }

        if (raw == null) {
            Assert.assertNull(wrapped);
            return;
        }
        Assert.assertEquals("插桩不得把原失败换成别的类型", raw.getClass(), wrapped.getClass());
        Assert.assertEquals("插桩不得改写失败消息", raw.getMessage(), wrapped.getMessage());
    }

    @Test
    public void failureIsWarnedOncePerSiteAndCarriesSiteMaskAndCause() {
        CollectingAppender appender = new CollectingAppender("GlStateDiagnosticsTest");
        LoggerConfig loggerConfig = attach(appender);
        try {
            GlStateDiagnostics.__warnedKinds().clear();
            captureWrappedPushFailure("unit-test-log");
            captureWrappedPushFailure("unit-test-log");
            captureWrappedPushFailure("unit-test-log");

            // 按 logger 名过滤：getLoggerConfig 无精确匹配时返回的是 root LoggerConfig，
            // 不过滤会把同期其它 logger（如 GlAttribDepth 的 warn-once）一起收进来。
            Assert.assertEquals("同一站点只 WARN 一次（warn-once 防刷屏）", 1, appender.warnCount(LOGGER_NAME));
            String message = appender.firstMessage(LOGGER_NAME);
            Assert.assertTrue("日志必须带站点：" + message, message.contains("unit-test-log"));
            Assert.assertTrue("日志必须带 mask：" + message, message.contains("mask=0x"));
            Assert.assertTrue("日志必须带深度读数（N13 栈状态）：" + message, message.contains("depth="));
            Assert.assertTrue("日志必须带 cause 文本（不依赖 appender 渲染 throwable）：" + message,
                    message.contains("cause="));
        } finally {
            loggerConfig.removeAppender(appender.getName());
            appender.stop();
        }
    }

    @Test
    public void glyphGateFailureIsRecordedPerPhase() {
        GlStateDiagnostics.__warnedKinds().clear();

        // R11：闸门失败必须按 phase 记账，且 entry 对照数据是日志参数的一部分（签名即契约）。
        GlStateDiagnostics.warnGlyphGateFailure("texture_init_attrib_push", 1280, 1286, 1);

        Assert.assertTrue("R11 现场必须按 phase 记账",
                GlStateDiagnostics.__warnedKinds().contains("glyph-gate@texture_init_attrib_push"));
    }

    private static Throwable captureRawPushFailure() {
        try {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private static Throwable captureRawPopFailure() {
        try {
            GL11.glPopAttrib();
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private static Throwable captureWrappedPushFailure(String site) {
        try {
            GlStateDiagnostics.pushAttrib(GL11.GL_ALL_ATTRIB_BITS, site);
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    /** 把 appender 挂到本工具 logger 的 LoggerConfig（样板同 ConstraintResolverWarnTest）。 */
    private static LoggerConfig attach(CollectingAppender appender) {
        appender.start();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig loggerConfig = configuration.getLoggerConfig(LOGGER_NAME);
        loggerConfig.addAppender(appender, Level.WARN, null);
        loggerConfig.setLevel(Level.WARN);
        context.updateLoggers();
        return loggerConfig;
    }

    /** 最简 collecting appender（log4j-core beta9 API；样板同 ConstraintResolverWarnTest）。 */
    private static final class CollectingAppender implements Appender {

        private final String name;
        private final List<LogEvent> events = new ArrayList<LogEvent>();
        private boolean started;
        private org.apache.logging.log4j.core.ErrorHandler handler;

        private CollectingAppender(String name) {
            this.name = name;
        }

        @Override
        public void append(LogEvent event) {
            events.add(event);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public org.apache.logging.log4j.core.Layout<? extends java.io.Serializable> getLayout() {
            return null;
        }

        @Override
        public boolean ignoreExceptions() {
            return true;
        }

        @Override
        public org.apache.logging.log4j.core.ErrorHandler getHandler() {
            return handler;
        }

        @Override
        public void setHandler(org.apache.logging.log4j.core.ErrorHandler handler) {
            this.handler = handler;
        }

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void stop() {
            started = false;
        }

        @Override
        public boolean isStarted() {
            return started;
        }

        private int warnCount(String loggerName) {
            int count = 0;
            for (LogEvent event : events) {
                if (event.getLevel() == Level.WARN && loggerName.equals(event.getLoggerName())) {
                    count++;
                }
            }
            return count;
        }

        private String firstMessage(String loggerName) {
            for (LogEvent event : events) {
                if (loggerName.equals(event.getLoggerName())) {
                    return String.valueOf(event.getMessage());
                }
            }
            return "";
        }
    }
}
