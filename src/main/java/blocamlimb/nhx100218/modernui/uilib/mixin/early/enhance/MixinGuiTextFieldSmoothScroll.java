package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/**
 * 文本框水平滚动的像素级平滑（对齐 Minecraft-Smooth-Scrolling 的 Text Input Field）。
 *
 * <p>原版 {@code drawTextBox} 按 {@code lineScrollOffset} 整字符跳过。这里在绘制期间把显示起点
 * 换成由缓动像素位置推出的字符下标，并平移分数像素；绘制结束恢复真实 {@code lineScrollOffset}，
 * 不污染原版光标/选择逻辑。文本框始终使用标准字体，故直接取 {@code Minecraft.fontRenderer}。</p>
 */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldSmoothScroll {

    @Shadow
    private int lineScrollOffset;

    @Shadow
    private String text;

    @Unique
    private final SmoothScrollModel modernui$model = new SmoothScrollModel();
    @Unique
    private int modernui$savedOffset;
    @Unique
    private boolean modernui$pushed;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void modernui$head(CallbackInfo ci) {
        modernui$savedOffset = lineScrollOffset;
        modernui$pushed = false;
        if (modernui$isXaeroScreen()) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.textSmoothness == 0 || text == null || font == null) {
            return;
        }
        if (lineScrollOffset <= 0) {
            // No horizontal scrolling is active. Reset the model so a field reused after
            // long text cannot scroll short input out of view.
            modernui$model.snap(0.0D);
            return;
        }
        double target = font.getStringWidth(modernui$prefix(text, lineScrollOffset));
        // 光标向右移动（输入）时立即到位，避免缓动落后把正在输入的字符挤出可视区；
        // 向左回退时才缓动。
        if (target > modernui$model.current()) {
            modernui$model.snap(target);
        } else {
            modernui$model.setTarget(target);
        }
        double smoothPixels = modernui$model.update(
                SmoothScrollConfig.smoothness(SmoothScrollConfig.textSmoothness));
        int index = modernui$indexFor(font, text, smoothPixels);
        double prefixWidth = font.getStringWidth(modernui$prefix(text, index));
        lineScrollOffset = index;
        float shift = (float) (prefixWidth - smoothPixels);
        if (shift != 0.0F) {
            GL11.glPushMatrix();
            GL11.glTranslatef(shift, 0.0F, 0.0F);
            modernui$pushed = true;
        }
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void modernui$return(CallbackInfo ci) {
        lineScrollOffset = modernui$savedOffset;
        if (modernui$pushed) {
            GL11.glPopMatrix();
            modernui$pushed = false;
        }
    }

    @Unique
    private static boolean modernui$isXaeroScreen() {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft == null ? null : minecraft.currentScreen;
        if (screen == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return name.startsWith("xaero.") || name.startsWith("com.gtnh.xaeroport.");
    }

    @Unique
    private static String modernui$prefix(String value, int length) {
        int end = length < 0 ? 0 : (length > value.length() ? value.length() : length);
        return value.substring(0, end);
    }

    /** 返回宽度不超过 {@code pixels} 的最大字符前缀长度。 */
    @Unique
    private static int modernui$indexFor(FontRenderer font, String value, double pixels) {
        double width = 0.0D;
        int index = 0;
        while (index < value.length()) {
            width += font.getCharWidth(value.charAt(index));
            if (width > pixels) {
                break;
            }
            index++;
        }
        return index;
    }
}
