package tech.tnze.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tech.tnze.client.Manager;
import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {

    @Inject(method = "renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void tnze$renderCandidateList(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        synchronized (Manager.uiElements) {
            Manager.uiElements.forEach((id, elem) -> elem.render(guiGraphics, mouseX, mouseY, delta));
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        LOGGER.info("{}.setFocused({})", this, guiEventListener);
        Manager.getInstance().onScreenFocusedOnWidget((Screen)(Object) this, guiEventListener);
    }
}
