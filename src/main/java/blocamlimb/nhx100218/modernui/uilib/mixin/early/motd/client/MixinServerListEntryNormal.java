package blocamlimb.nhx100218.modernui.uilib.mixin.early.motd.client;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blocamlimb.nhx100218.modernui.server.LegacyMotdText;
import blocamlimb.nhx100218.modernui.uilib.client.motd.MotdGradientRenderer;

/**
 * Replaces the vanilla two-line MOTD with ModernUI's per-glyph gradient when
 * the current renderer supports it. SDL GPU keeps the legacy fallback.
 */
@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal {

    @Shadow
    @Final
    private Minecraft field_148300_d;

    @Shadow
    @Final
    private ServerData field_148301_e;

    @Redirect(
            method = "drawEntry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;listFormattedStringToWidth(Ljava/lang/String;I)Ljava/util/List;"))
    private List<String> modernui$suppressVanillaMotd(FontRenderer font, String text, int width) {
        if (MotdGradientRenderer.shouldReplace(text)) {
            return Collections.emptyList();
        }
        return font.listFormattedStringToWidth(text, width);
    }

    @Inject(method = "drawEntry", at = @At("TAIL"))
    private void modernui$drawGradientMotd(int id, int x, int y, int width, int height, Tessellator tessellator,
            int mouseX, int mouseY, boolean hovered, CallbackInfo callback) {
        String motd = field_148301_e == null ? null : field_148301_e.serverMOTD;
        if (!MotdGradientRenderer.shouldReplace(motd)) {
            return;
        }
        if (!MotdGradientRenderer.draw(field_148300_d.fontRenderer, motd, x + 32 + 3, y + 12, width - 48 - 2, 2)) {
            drawPlain(field_148300_d.fontRenderer, motd, x + 32 + 3, y + 12, width - 48 - 2);
        }
    }

    private static void drawPlain(FontRenderer font, String motd, int x, int y, int width) {
        String text = motd.startsWith(LegacyMotdText.MARKER)
                ? motd.substring(LegacyMotdText.MARKER.length())
                : motd;
        List<String> lines = font.listFormattedStringToWidth(text, width);
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            font.drawString(lines.get(i), x, y + font.FONT_HEIGHT * i, 0x808080, false);
        }
    }
}
