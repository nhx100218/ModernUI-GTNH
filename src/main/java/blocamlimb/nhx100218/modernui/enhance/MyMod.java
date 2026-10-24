package blocamlimb.nhx100218.modernui.enhance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 原附属增强模组的常量/日志持有类。
 *
 * <p>合并进 ModernUI-GTNH 后不再单独作为 {@code @Mod} 入口；初始化由
 * {@code blocamlimb.nhx100218.modernui.uilib.MyMod} 统一驱动（见其 {@code enhanceProxy}）。</p>
 */
public final class MyMod {

    /** 旧附属模组 id，仅保留给内部常量引用。 */
    public static final String MODID = "modernui";
    public static final Logger LOG = LogManager.getLogger("ModernUI-GTNH/Enhance");

    private MyMod() {}
}
