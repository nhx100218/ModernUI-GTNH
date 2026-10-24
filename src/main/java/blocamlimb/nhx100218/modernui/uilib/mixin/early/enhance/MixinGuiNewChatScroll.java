package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import java.lang.reflect.Field;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.MyMod;
import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatScrollSmooth;
import blocamlimb.nhx100218.modernui.enhance.vanilla.HudCache;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;

/** 聊天栏滚轮平滑：整体按缓动残差做纵向位移。 */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChatScroll {

    @Unique
    private static Field modernui$scrollField;
    @Unique
    private static Field modernui$scrollField2;

    @Unique
    private boolean modernui$pushed;
    @Unique
    private int modernui$logTick;

    @Inject(method = "scroll", at = @At("HEAD"))
    private void modernui$onScroll(int amount, CallbackInfo ci) {
        MyMod.LOG.info("[ChatScroll] scroll amount={}", Integer.valueOf(amount));
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void modernui$head(int counter, CallbackInfo ci) {
        modernui$pushed = false;
        float d = 0.0F;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            boolean open = mc.currentScreen instanceof GuiChat && SmoothScrollConfig.enabled;
            int s1 = modernui$read(1);
            int s2 = modernui$read(2);
            if (open && (modernui$logTick++ % 40) == 0) {
                MyMod.LOG.info("[ChatField] f146250_j={} f146251_k={}", Integer.valueOf(s1), Integer.valueOf(s2));
            }
            d = ChatScrollSmooth.update(s1, open);
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
    private int modernui$read(int which) {
        try {
            Field field;
            if (which == 1) {
                if (modernui$scrollField == null) {
                    modernui$scrollField = GuiNewChat.class.getDeclaredField("field_146250_j");
                    modernui$scrollField.setAccessible(true);
                }
                field = modernui$scrollField;
            } else {
                if (modernui$scrollField2 == null) {
                    modernui$scrollField2 = GuiNewChat.class.getDeclaredField("field_146251_k");
                    modernui$scrollField2.setAccessible(true);
                }
                field = modernui$scrollField2;
            }
            Object value = field.get(this);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue() ? 1 : 0;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
