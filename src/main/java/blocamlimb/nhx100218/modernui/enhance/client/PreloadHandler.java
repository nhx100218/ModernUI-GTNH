package blocamlimb.nhx100218.modernui.enhance.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** 在渲染帧末驱动预热（加载界面期间即可触发）。 */
public class PreloadHandler {

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PreloadService.onRenderTick();
        }
    }
}
