package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import java.lang.reflect.Field;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatScrollSmooth;
import blocamlimb.nhx100218.modernui.enhance.vanilla.HudCache;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;

/** Applies the original chat opening offset while the vanilla chat is drawn. */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChatScroll {

    @Unique
    private static Field modernui$scrollField;

    @Unique
    private boolean modernui$pushed;

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void modernui$head(int counter, CallbackInfo ci) {
        modernui$pushed = false;
        float d = 0.0F;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            boolean open = mc.currentScreen instanceof GuiChat && SmoothScrollConfig.enabled;
            d = ChatScrollSmooth.update(modernui$read(), open);
        } catch (Throwable ignored) {
        }
        if (d != 0.0F) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, d, 0.0F);
            modernui$pushed = true;
            HudCache.markDirty();
        }
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void modernui$ret(int counter, CallbackInfo ci) {
        if (modernui$pushed) {
            GL11.glPopMatrix();
            modernui$pushed = false;
        }
    }

    @Unique
    private int modernui$read() {
        try {
            if (modernui$scrollField == null) {
                modernui$scrollField = GuiNewChat.class.getDeclaredField("field_146250_j");
                modernui$scrollField.setAccessible(true);
            }
            Object value = modernui$scrollField.get(this);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
