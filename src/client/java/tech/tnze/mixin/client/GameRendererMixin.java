package tech.tnze.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.Manager;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderDeferredSubtitles()V"))
    public void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci, @Local GuiGraphics guiGraphics, @Local(ordinal = 0) int mouseX, @Local(ordinal = 1) int mouseY) {
        synchronized (Manager.uiElements) {
            Manager.uiElements.forEach((id, elem) -> elem.render(guiGraphics, mouseX, mouseY, deltaTracker.getGameTimeDeltaTicks()));
        }
    }
}
