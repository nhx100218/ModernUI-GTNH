package blocamlimb.nhx100218.modernui.enhance.client;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatScrollSmooth;
import blocamlimb.nhx100218.modernui.enhance.vanilla.HotbarSmoothScroll;
import blocamlimb.nhx100218.modernui.enhance.vanilla.HudCache;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

/**
 * 逐帧驱动 HUD 缓存重建：当快捷栏选中框或聊天栏缓动未收敛时，把 Angelica 的 HUD 缓存标记为脏，
 * 否则被缓存的 HUD 不会重绘，缓动不可见。
 */
public class SmoothScrollHudTicker {

    private String modernui$lastState = "";

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }
            boolean chatOpen = mc.currentScreen instanceof GuiChat;
            boolean hotbar = mc.thePlayer != null
                    && HotbarSmoothScroll.isAnimating(mc.thePlayer.inventory.currentItem);
            boolean chat = ChatScrollSmooth.isAnimating();
            boolean active = chatOpen || hotbar || chat;
            if (active) {
                HudCache.markDirty();
            }
            String state = chatOpen + "," + hotbar + "," + chat;
            if (!state.equals(modernui$lastState)) {
                modernui$lastState = state;
                MyMod.LOG.info("[SmoothScrollHud] chatOpen={}, hotbar={}, chat={}, cache={}",
                        Boolean.valueOf(chatOpen), Boolean.valueOf(hotbar), Boolean.valueOf(chat),
                        Boolean.valueOf(HudCache.available()));
            }
        } catch (Throwable ignored) {
            // 防御性：驱动失败不影响渲染
        }
    }
}
