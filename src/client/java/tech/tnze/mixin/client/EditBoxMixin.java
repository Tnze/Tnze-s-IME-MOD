package tech.tnze.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.Manager;
import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACP2;

import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Inject(method = "setFocused(Z)V", at = @At("TAIL"))
    public void tnze$setFocused(boolean focused, CallbackInfo ci) {
//        LOGGER.info("EditBox focused state changed: {}", focused);
//        Manager.getInstance().setFocus(focused);
    }
}
