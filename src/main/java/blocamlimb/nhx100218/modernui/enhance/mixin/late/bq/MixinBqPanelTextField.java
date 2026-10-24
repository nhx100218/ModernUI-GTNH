package blocamlimb.nhx100218.modernui.enhance.mixin.late.bq;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blocamlimb.nhx100218.modernui.enhance.client.BqImeBridge;
import blocamlimb.nhx100218.modernui.enhance.vanilla.UiConfig;

import betterquesting.api2.client.gui.controls.PanelTextField;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;

/**
 * 任务书搜索框等 BQ 文本框（{@code PanelTextField}，自绘，不继承原版 {@code GuiTextField}）
 * 适配本模组体验：
 *
 * <ul>
 *   <li>选中/光标高亮（{@code colHighlight}，默认取 BQ 主题 {@code TEXT_HIGHLIGHT}）改用
 *       {@link UiConfig#selectionColor} 定义的柔和蓝，与其它界面一致；</li>
 *   <li>绘制时上报聚焦状态给 {@link BqImeBridge}，使其成为 lwjgl3ify 输入法文本的落点（中文输入）。</li>
 * </ul>
 */
@Mixin(targets = "betterquesting.api2.client.gui.controls.PanelTextField")
public class MixinBqPanelTextField {

    @Shadow(remap = false)
    private IGuiColor colHighlight;

    /** 构造体内会调用 {@code setAuxColors} 设默认高亮色，故在构造末尾覆盖为本模组选中色。 */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void modernui$selectionColor(CallbackInfo ci) {
        if (UiConfig.selectionEnabled) {
            this.colHighlight = new GuiColorStatic(
                    (UiConfig.selectionAlpha << 24) | (UiConfig.selectionColor & 0xFFFFFF));
        }
    }

    /** 每帧绘制时把聚焦文本框登记到输入法桥，供 IME 文本投递。 */
    @Inject(method = "drawPanel", at = @At("HEAD"), remap = false)
    private void modernui$trackFocus(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        BqImeBridge.trackFocus((PanelTextField<?>) (Object) this);
    }

    /**
     * 丢弃 lwjgl3ify 为每个字符额外入队的「虚拟按键」事件（{@code keyCode == 0}）。
     *
     * <p>lwjgl3ify 在字符回调里会用 {@code addCharEvent} 再入队一次 {@code keyCode=0} 的按键；
     * 原版 {@code GuiTextField} 由 lwjgl3ify 的输入缓冲去重，BQ 的自绘文本框没有这层去重，
     * 于是英文会被写两遍（"a"→"aa"）。</p>
     *
     * <p><b>只去重 ASCII</b>：1.7.10 的 {@code isAllowedCharacter} 实际是「≥32 且非 §/127」，
     * 中文也返回 true，且中文只有这一条 {@code keyCode=0} 的事件（IME 不产生真实字符按键），
     * 若一并吞掉会导致中文完全打不出来。</p>
     */
    @Inject(method = "onKeyTyped", at = @At("HEAD"), cancellable = true, remap = false)
    private void modernui$dedupeCharEvent(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == 0 && typedChar >= 32 && typedChar < 128
                && net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
