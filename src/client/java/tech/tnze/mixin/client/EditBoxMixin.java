package tech.tnze.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.Manager;

import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(EditBox.class)
public class EditBoxMixin {
//    @Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
//    public void tnze$registerDocumentMgr(Font font, int x, int y, int w, int h, EditBox editBox, Component message, CallbackInfo ci) {
//
//    }

    @Inject(method = "setFocused(Z)V", at = @At("TAIL"))
    public void tnze$setFocused(boolean focused, CallbackInfo ci) {
        LOGGER.info("EditBox focused state changed: {}", focused);
        Manager.getInstance().setFocus(focused);
//        if (focused) {
//            return;
//        }
//        Context ctx = IMEClient.mContexts.remove((EditBox) (Object) this);
//        if (ctx != null) {
//            try {
//                mDocumentManager.pop(DocumentManager.TF_POPF_ALL);
//                ctx.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
}
