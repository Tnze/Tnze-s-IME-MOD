package tech.tnze.mixin.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tech.tnze.client.Manager;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "setFocused", at = @At("RETURN"))
    private void tnze$setFocused(@Nullable GuiEventListener guiEventListener, CallbackInfo ci) {
        Manager.getInstance().onScreenFocusedChange((Screen)(Object) this);
    }
}
