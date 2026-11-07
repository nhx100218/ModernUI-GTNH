package blocamlimb.nhx100218.modernui.enhance.tooltip;

import java.util.List;
import java.util.function.Consumer;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Hooks GTNHLib's RenderTooltipEvent when GTNHLib is installed.
 *
 * <p>The event class lives in a nested listener so this outer class can load
 * without GTNHLib on the classpath.</p>
 */
public final class TooltipForgeHandler {

    private static final String EVENT_CLASS = "com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent";

    private TooltipForgeHandler() {}

    /** Registers the listener only when GTNHLib is present. */
    public static void install() {
        try {
            Class.forName(EVENT_CLASS);
        } catch (ClassNotFoundException absent) {
            MyMod.LOG.info("GTNHLib RenderTooltipEvent was not found; skipping tooltip event hook");
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Listener());
        MyMod.LOG.info("Registered the GTNHLib tooltip event hook");
    }

    public static final class Listener {

        @SubscribeEvent
        public void onRenderTooltip(com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent event) {
            if (event == null || !TooltipConfig.enabled) {
                return;
            }
            final FontRenderer font = event.font;
            final int x = event.x;
            final int y = event.y;
            final int width = event.gui != null ? event.gui.width : 0;
            final int height = event.gui != null ? event.gui.height : 0;
            if (font == null || width <= 0 || height <= 0) {
                return;
            }
            event.alternativeRenderer = new Consumer<List<String>>() {
                @Override
                public void accept(List<String> lines) {
                    TooltipRenderer.draw(lines, x, y, font, width, height);
                }
            };
        }
    }
}
