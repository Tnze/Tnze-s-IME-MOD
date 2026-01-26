package tech.tnze.mixin.client;

import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import tech.tnze.client.Manager;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        Manager.getInstance().onScreenFocusedChange((Screen)(Object) this);
    }
}
