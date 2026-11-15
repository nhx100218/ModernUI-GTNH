package blocamlimb.nhx100218.modernui.uilib.mixin.early.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSnooper;
import net.minecraft.server.integrated.IntegratedServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevents the anonymous-feedback screen from blocking the client while it
 * collects integrated-server usage statistics in a loaded save.
 *
 * <p>The client-side statistics are still shown. Only the expensive server
 * snapshot is skipped in-world; opening the screen from the title menu keeps
 * the vanilla path.</p>
 */
@Mixin(GuiSnooper.class)
public abstract class MixinGuiSnooperServerStats {

    @Redirect(method = "initGui",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getIntegratedServer()Lnet/minecraft/server/integrated/IntegratedServer;"),
            require = 0)
    private IntegratedServer modernui$skipServerSnooperInWorld(Minecraft minecraft) {
        return minecraft.theWorld != null ? null : minecraft.getIntegratedServer();
    }
}
