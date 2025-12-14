package tech.tnze.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tech.tnze.client.Manager;


@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void tnze$renderCandidateList(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        synchronized (Manager.uiElements) {
            Manager.uiElements.forEach((id, elem) -> elem.render(guiGraphics, mouseX, mouseY, delta));
        }
    }

}
