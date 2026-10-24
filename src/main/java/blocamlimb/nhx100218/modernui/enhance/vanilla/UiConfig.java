package blocamlimb.nhx100218.modernui.enhance.vanilla;

/** 通用界面增强开关。 */
public final class UiConfig {

    public static volatile boolean textUndoEnabled = true;
    public static volatile int textUndoLimit = 100;
    /** 文本框选中高亮（替代原版 XOR 蓝色）。 */
    public static volatile boolean selectionEnabled = true;
    public static volatile int selectionColor = 0x33B5E5;
    public static volatile int selectionAlpha = 0x38;
    /** 选中框整体像素偏移（用于对齐 ModernUI 替换后的字体字形；正值向右/向下，负值向左/向上；支持小数）。 */
    public static volatile float selectionOffsetX = 0.30F;
    public static volatile float selectionOffsetY = -1.75F;

    private UiConfig() {}
}
