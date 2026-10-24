package blocamlimb.nhx100218.modernui.enhance.tooltip;

import java.util.List;
import java.util.function.Consumer;

import com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.common.MinecraftForge;

/**
 * 通过 GTNHLib 的 {@link RenderTooltipEvent} 接管 NEI 的 tooltip 渲染。
 *
 * <p>GTNH 里物品 tooltip 由 NEI 的 {@code GuiContainerManager.renderToolTips} 绘制，它不走
 * {@code GuiScreen.renderToolTip}/{@code drawHoveringText}。NEI 会 post
 * {@code com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent}（Forge 事件总线），并在
 * {@code alternativeRenderer != null} 时改用它绘制整块 tooltip——ChromaticTooltips 正是靠此
 * 覆盖原版样式。本处理器把 {@code alternativeRenderer} 设为 UILib 的 tooltip 渲染器。</p>
 */
public final class TooltipForgeHandler {

    private static final String EVENT_CLASS = "com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent";

    private TooltipForgeHandler() {}

    /** 注册到 Forge 事件总线；GTNHLib 缺席时安全跳过。 */
    public static void install() {
        try {
            Class.forName(EVENT_CLASS);
        } catch (ClassNotFoundException absent) {
            MyMod.LOG.info("未检测到 GTNHLib RenderTooltipEvent，跳过 tooltip 事件接管");
            return;
        }
        MinecraftForge.EVENT_BUS.register(new TooltipForgeHandler());
        MyMod.LOG.info("已注册 GTNHLib tooltip 事件接管");
    }

    @SubscribeEvent
    public void onRenderTooltip(RenderTooltipEvent event) {
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
