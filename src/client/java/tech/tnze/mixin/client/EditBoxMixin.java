package tech.tnze.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.IMEClient;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Inject(method = "setFocused(Z)V", at = @At("TAIL"))
    public void tnze$setFocused(boolean focused, CallbackInfo ci) {
        IMEClient.LOGGER.info("EditBox focused state changed: {}", focused);
    }
}
