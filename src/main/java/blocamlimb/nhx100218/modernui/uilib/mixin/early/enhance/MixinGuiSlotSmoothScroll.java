package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollConfig;
import blocamlimb.nhx100218.modernui.enhance.vanilla.SmoothScrollModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;

/**
 * 原版可滚动列表 {@link GuiSlot} 的指数平滑滚动（对齐 Minecraft-Smooth-Scrolling 的 Entry List）。
 *
 * <p>滚轮/scrollBy 只累积目标值，每帧用 {@link SmoothScrollModel} 向目标逼近；拖动滚动条时直接吸附，
 * 避免缓动与拖拽打架。缓动强度与每次滚动量由配置页提供。</p>
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotSmoothScroll {

    @Shadow
    private float amountScrolled;

    @Shadow
    private float initialClickY;

    @Shadow
    public int top;

    @Shadow
    public int bottom;

    @Shadow
    public int slotHeight;

    @Shadow
    protected abstract int getContentHeight();

    private final SmoothScrollModel modernui$model = new SmoothScrollModel();
    private boolean modernui$tracking;
    private int modernui$lastContentHeight = -1;
    private float modernui$lastApplied;
    private long modernui$lastWheelTime;
    private float modernui$wheelMomentum;

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void modernui$animate(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.listSmoothness == 0 || !modernui$tracking) {
            return;
        }
        long now = Minecraft.getSystemTime();
        if (now - modernui$lastWheelTime > 180L && modernui$wheelMomentum > 0.0F) {
            modernui$wheelMomentum = Math.max(0.0F, modernui$wheelMomentum - 0.35F);
        }
        int contentHeight = getContentHeight();
        int max = Math.max(0, contentHeight - (bottom - top - 4));

        // Searching/filtering rebuilds the entries and shrinks the content
        // height. The old target can then sit beyond the new maximum, which
        // makes the list look stuck at an empty scroll range.
        if (modernui$lastContentHeight < 0) {
            modernui$lastContentHeight = contentHeight;
        } else if (contentHeight != modernui$lastContentHeight) {
            modernui$lastContentHeight = contentHeight;
            double clamped = Math.max(0.0D, Math.min(modernui$model.current(), max));
            modernui$model.snap(clamped);
            amountScrolled = (float) clamped;
            modernui$lastApplied = amountScrolled;
        }
        if (modernui$model.target() < 0.0D || modernui$model.target() > max) {
            modernui$model.setTarget(Math.max(0.0D, Math.min(modernui$model.target(), max)));
        }

        // Scroll-bar drags and search "scroll to key" write amountScrolled
        // directly. Re-sync the model instead of overwriting that position.
        if (initialClickY < 0.0F && Math.abs(amountScrolled - modernui$lastApplied) > 0.5F) {
            modernui$model.snap(amountScrolled);
        }

        // 拖动滚动条（initialClickY >= 0）时直接同步，避免缓动与拖动打架。
        if (initialClickY >= 0.0F) {
            modernui$model.snap(amountScrolled);
            modernui$lastApplied = amountScrolled;
            return;
        }
        amountScrolled = (float) modernui$model.update(
                SmoothScrollConfig.smoothness(SmoothScrollConfig.listSmoothness));
        modernui$lastApplied = amountScrolled;
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", remap = false,
            target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int modernui$onWheel() {
        int delta = Mouse.getEventDWheel();
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.listSmoothness == 0 || delta == 0) {
            return delta;
        }
        int step = delta > 0 ? -1 : 1;
        int base = SmoothScrollConfig.listAmount > 0 ? SmoothScrollConfig.listAmount : 40;
        long now = Minecraft.getSystemTime();
        long elapsed = modernui$lastWheelTime == 0L ? 1000L : now - modernui$lastWheelTime;
        modernui$lastWheelTime = now;
        // Fast repeated wheel events build momentum; one large OS wheel delta
        // also scrolls farther. This makes the wheel speed actually matter.
        float burst = elapsed < 90L ? 1.6F : 1.0F;
        if (elapsed < 35L) {
            burst = 2.4F;
        }
        float deltaScale = Math.max(1.0F, Math.min(3.0F, Math.abs(delta) / 120.0F));
        modernui$wheelMomentum = Math.min(4.0F, modernui$wheelMomentum + burst);
        int amount = Math.round(base * deltaScale * Math.min(3.0F, 1.0F + modernui$wheelMomentum * 0.25F));
        modernui$addTarget(step * amount);
        return 0;
    }

    @Inject(method = "scrollBy", at = @At("HEAD"), cancellable = true)
    private void modernui$scrollBy(int amount, CallbackInfo ci) {
        if (!SmoothScrollConfig.enabled || SmoothScrollConfig.listSmoothness == 0) {
            return;
        }
        modernui$addTarget(amount);
        ci.cancel();
    }

    private void modernui$addTarget(int amount) {
        if (!modernui$tracking) {
            modernui$model.snap(amountScrolled);
            modernui$tracking = true;
            modernui$lastContentHeight = getContentHeight();
            modernui$lastApplied = amountScrolled;
        }
        modernui$model.setTarget(modernui$model.target() + amount);
        int max = Math.max(0, getContentHeight() - (bottom - top - 4));
        if (modernui$model.target() < 0.0D) {
            modernui$model.setTarget(0.0D);
        }
        if (modernui$model.target() > max) {
            modernui$model.setTarget(max);
        }
    }
}
