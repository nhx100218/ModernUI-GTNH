package blocamlimb.nhx100218.modernui.enhance.tooltip;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Expands ModularUI2 tooltip width when ModularUI2 is installed.
 *
 * <p>The event class lives in a nested listener so this outer class can load
 * without ModularUI2 on the classpath.</p>
 */
public final class ModularUiTooltipHandler {

    private static final String EVENT_CLASS = "com.cleanroommc.modularui.screen.RichTooltipEvent$Pre";

    private ModularUiTooltipHandler() {}

    public static void install() {
        try {
            Class.forName(EVENT_CLASS);
        } catch (ClassNotFoundException absent) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Listener());
    }

    public static final class Listener {

        @SubscribeEvent
        public void onPre(com.cleanroommc.modularui.screen.RichTooltipEvent.Pre event) {
            if (!TooltipConfig.enabled || event == null) {
                return;
            }
            int max = TooltipConfig.modularUiMaxWidth > 0 ? TooltipConfig.modularUiMaxWidth : 100000;
            event.setMaxWidth(Math.max(event.getMaxWidth(), max));
        }
    }
}
