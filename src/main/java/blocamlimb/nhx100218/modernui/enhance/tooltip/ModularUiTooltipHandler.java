package blocamlimb.nhx100218.modernui.enhance.tooltip;

import com.cleanroommc.modularui.screen.RichTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * 放宽 ModularUI2（格雷等）tooltip 的折行宽度。
 *
 * <p>内置 Inter/思源黑体较系统字体偏宽，ModularUI 的默认 maxWidth 会触发意外换行；这里把折行宽度
 * 放宽到接近屏幕宽，避免误换行。</p>
 */
public final class ModularUiTooltipHandler {

    private ModularUiTooltipHandler() {}

    public static void install() {
        try {
            Class.forName("com.cleanroommc.modularui.screen.RichTooltipEvent$Pre");
        } catch (ClassNotFoundException absent) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new ModularUiTooltipHandler());
    }

    @SubscribeEvent
    public void onPre(RichTooltipEvent.Pre event) {
        if (!TooltipConfig.enabled || event == null) {
            return;
        }
        // 直接把折行阈值拉到很大，尽量避免任何换行。
        int max = TooltipConfig.modularUiMaxWidth > 0 ? TooltipConfig.modularUiMaxWidth : 100000;
        event.setMaxWidth(Math.max(event.getMaxWidth(), max));
    }
}
