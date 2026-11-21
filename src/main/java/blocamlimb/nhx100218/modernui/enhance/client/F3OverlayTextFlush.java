package blocamlimb.nhx100218.modernui.enhance.client;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import blocamlimb.nhx100218.modernui.uilib.font.F3FontBridge;

/**
 * Flushes the queued F3 SDF text after the HUD text pass.
 *
 * <p>This runs with the vanilla GUI orthographic projection; flushing inside the
 * F3 overlay draw left world-space matrices, which made ModernUI's font renderer
 * apply lighting and output dim grey glyphs.</p>
 */
public final class F3OverlayTextFlush {

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            F3FontBridge.flushFramePost();
        }
    }
}
