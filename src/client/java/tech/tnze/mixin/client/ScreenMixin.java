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

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void renderCandidateList(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Manager.uiElements.forEach((id, elem) -> elem.render(guiGraphics, mouseX, mouseY, delta));
    }

}
