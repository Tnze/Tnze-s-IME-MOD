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

import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(EditBox.class)
public class EditBoxMixin {
    //    @Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    //    public void tnze$registerDocumentMgr(Font font, int x, int y, int w, int h, EditBox editBox, Component message, CallbackInfo ci) {
    //
    //    }
    @Shadow
    private String value;
    @Final
    @Shadow
    private Font font;
    @Shadow
    private int textX;
    @Shadow
    private int textY;

    @Shadow
    private int displayPos;
    @Shadow
    private int cursorPos;

    @Shadow
    private FormattedCharSequence applyFormat(String string, int i) {
        return null;
    }

    @Inject(method = "updateTextPosition", at = @At("TAIL"))
    public void tnze$updateTextPosition(CallbackInfo ci) {
        EditBox self = (EditBox) (Object) this;
        int frontCursor = cursorPos - displayPos;
        String string = font.plainSubstrByWidth(value.substring(displayPos), self.getInnerWidth());
        boolean bl = frontCursor >= 0 && frontCursor <= string.length();
        int cursorX = textX;
        if (!string.isEmpty()) {
            String string2 = bl ? string.substring(0, frontCursor) : string;
            FormattedCharSequence formattedCharSequence = applyFormat(string2, displayPos);
            cursorX += this.font.width(formattedCharSequence);
        }
        Manager.getInstance().setPosition(textX, textY, cursorX);
    }

    @Inject(method = "setFocused(Z)V", at = @At("TAIL"))
    public void tnze$setFocused(boolean focused, CallbackInfo ci) {
        LOGGER.debug("EditBox focused state changed: {}", focused);
        Manager.getInstance().setFocus(focused);
    }
}
