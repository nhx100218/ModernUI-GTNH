package blocamlimb.nhx100218.modernui.enhance.mixin.early;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.ChatScrollSmooth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

/** 聊天平滑（按 ChatLine 年龄淡入+滑入）与聊天头像（复用 ServerUtilities 皮肤缓存）。 */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    private static final Pattern SENDER = Pattern.compile("^(?:<([A-Za-z0-9_]{1,16})>|([A-Za-z0-9_]{1,16}):)\\s?");
    private static final Pattern COLOR = Pattern.compile("\u00a7.");

    // 当前正在绘制的行（用于头像）。
    private static ChatLine modernui$line;

    // 按行缓存，避免每帧做正则/反射。
    private static final Map<ChatLine, String> modernui$names = new WeakHashMap<ChatLine, String>();
    private static final Map<ChatLine, ResourceLocation> modernui$skins = new WeakHashMap<ChatLine, ResourceLocation>();

    // ServerUtilities TabSkinCache 反射缓存（可选依赖）。
    private static boolean modernui$suResolved;
    private static Object modernui$suInstance;
    private static Method modernui$suGetSkin;

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/ChatLine;getUpdatedCounter()I"))
    private int modernui$captureLine(ChatLine line) {
        modernui$line = line;
        return line.getUpdatedCounter();
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"))
    private int modernui$drawLine(FontRenderer font, String text, int x, int y, int color) {
        // 用原版为每行算好的透明度（新消息淡入 / 旧消息淡出），文字与头像同步淡入淡出。
        int alphaBits = (color >>> 24) & 0xFF;
        float alpha = alphaBits == 0 ? 1.0F : alphaBits / 255.0F;
        ChatLine line = modernui$line;
        int drawX = x;
        if (ChatConfig.headsEnabled && line != null) {
            int headWidth = drawHead(line, text, x, y, alpha);
            if (headWidth > 0) {
                drawX += headWidth;
            }
        }
        String shown = blocamlimb.nhx100218.modernui.enhance.vanilla.EmojiConfig.enabled
                ? blocamlimb.nhx100218.modernui.enhance.emoji.EmojiShortcodes.replace(text)
                : text;
        return font.drawStringWithShadow(shown, drawX, y, color);
    }

    @Inject(method = "printChatMessage", at = @At("RETURN"))
    private void modernui$newMessage(net.minecraft.util.IChatComponent component, CallbackInfo ci) {
        ChatScrollSmooth.onNewMessage();
    }

    private static String modernui$sender(ChatLine line, String text) {
        String name = modernui$names.get(line);
        if (name != null) {
            return name.isEmpty() ? null : name;
        }
        String plain = COLOR.matcher(text).replaceAll("");
        Matcher matcher = SENDER.matcher(plain);
        name = matcher.find() ? (matcher.group(1) != null ? matcher.group(1) : matcher.group(2)) : "";
        modernui$names.put(line, name);
        return name.isEmpty() ? null : name;
    }

    /** 优先用 ServerUtilities 的皮肤缓存（含联网），失败再退回世界内的玩家实体。 */
    private static ResourceLocation modernui$skin(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!modernui$suResolved) {
                modernui$suResolved = true;
                Class<?> cache = Class.forName("serverutils.client.tab.TabSkinCache");
                Field instance = cache.getField("INSTANCE");
                modernui$suInstance = instance.get(null);
                modernui$suGetSkin = cache.getMethod("getOrLoadSkin", String.class);
            }
            if (modernui$suGetSkin != null && modernui$suInstance != null) {
                Object skin = modernui$suGetSkin.invoke(modernui$suInstance, name);
                if (skin instanceof ResourceLocation) {
                    return (ResourceLocation) skin;
                }
            }
        } catch (Throwable ignored) {
        }
        if (mc != null && mc.theWorld != null) {
            for (Object o : mc.theWorld.playerEntities) {
                if (o instanceof AbstractClientPlayer && ((AbstractClientPlayer) o).getCommandSenderName().equals(name)) {
                    return ((AbstractClientPlayer) o).getLocationSkin();
                }
            }
        }
        return null;
    }

    /** 在行首绘制皮肤头部，返回占用宽度（0 表示未绘制）。 */
    private static int drawHead(ChatLine line, String text, int x, int y, float alpha) {
        String name = modernui$sender(line, text);
        if (name == null) {
            return 0;
        }
        ResourceLocation skin = modernui$skins.get(line);
        if (skin == null) {
            skin = modernui$skin(name);
            if (skin == null) {
                return 0;
            }
            modernui$skins.put(line, skin);
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return 0;
        }
        float size = ChatConfig.headSize;
        float gap = ChatConfig.headGap;
        try {
            mc.getTextureManager().bindTexture(skin);
            // 脸部位于像素 (8,8)-(16,16)；宽度恒为 64，高度 32(旧版) 或 64(含帽子层)需运行时判定。
            int texH = 64;
            try {
                int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                if (h >= 16) {
                    texH = h;
                }
            } catch (Throwable ignored) {
            }
            float u0 = 8.0F / 64.0F;
            float u1 = 16.0F / 64.0F;
            float v0 = 8.0F / texH;
            float v1 = 16.0F / texH;
            float top = y - 1.0F;
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(x, top + size, 0.0D, u0, v1);
            tessellator.addVertexWithUV(x + size, top + size, 0.0D, u1, v1);
            tessellator.addVertexWithUV(x + size, top, 0.0D, u1, v0);
            tessellator.addVertexWithUV(x, top, 0.0D, u0, v0);
            tessellator.draw();
        } catch (Throwable ignored) {
            return 0;
        }
        return (int) (size + gap);
    }
}
