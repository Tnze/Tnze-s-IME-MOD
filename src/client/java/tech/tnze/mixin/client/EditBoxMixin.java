package tech.tnze.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.ACPSinkRegister;
import tech.tnze.client.AbstractTextFieldACP;
import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;
import tech.tnze.msctf.windows.win32.ui.textservices.TS_TEXTCHANGE;
import tech.tnze.msctf.windows.win32.ui.textservices.TsLayoutCode;

import java.lang.foreign.Arena;

import static tech.tnze.msctf.WindowsException.checkResult;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.TS_SD_READONLY;

@Mixin(EditBox.class)
public abstract class EditBoxMixin implements ACPSinkRegister {

    @Shadow
    public abstract String getValue();

    @Shadow
    public abstract boolean isEditable();

    @Unique
    @Nullable
    private ITextStoreACPSink textStoreSink;

    @Unique
    private boolean sinkEnabled = true;

    @Override
    public void tnze$registerACPSink(AbstractTextFieldACP acpImpl, ITextStoreACPSink sink) {
        textStoreSink = sink;
    }

    @Override
    public void tnze$unregisterACPSink(AbstractTextFieldACP acpImpl, ITextStoreACPSink sink) {
        assert sink.equals(textStoreSink);
        textStoreSink = null;
    }

    @Override
    public void tnze$setSinkEnabled(boolean enabled) {
        sinkEnabled = enabled;
    }

    @Unique
    private int changeStart, changeEnd;

    @Inject(method = "setValue", at = @At("HEAD"))
    public void tnze$setValue$head(String string, CallbackInfo ci) {
        changeStart = 0;
        changeEnd = this.getValue().length();
    }

    @Inject(method = "setValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;onValueChange(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    public void tnze$setValue$onValueChange(String string, CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            try (var arena = Arena.ofConfined()) {
                var change = arena.allocate(TS_TEXTCHANGE.layout());
                TS_TEXTCHANGE.acpStart(change, changeStart);
                TS_TEXTCHANGE.acpOldEnd(change, changeEnd);
                TS_TEXTCHANGE.acpNewEnd(change, this.getValue().length());
                checkResult(textStoreSink.OnTextChange(0, change));
            }
        }
    }

    @Inject(method = "insertText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;onValueChange(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    public void tnze$insertText$onValueChange(String string, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
        if (textStoreSink != null && sinkEnabled) {
            try (var arena = Arena.ofConfined()) {
                var change = arena.allocate(TS_TEXTCHANGE.layout());
                TS_TEXTCHANGE.acpStart(change, i);
                TS_TEXTCHANGE.acpOldEnd(change, j);
                TS_TEXTCHANGE.acpNewEnd(change, this.getValue().length());
                checkResult(textStoreSink.OnTextChange(0, change));
            }
        }
    }

    @Inject(method = "setCursorPosition", at = @At("TAIL"))
    public void tnze$setCursorPosition(int i, CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            checkResult(textStoreSink.OnSelectionChange());
        }
    }

    @Inject(method = "setHighlightPos", at = @At("TAIL"))
    public void tnze$setHighlightPos(int i, CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            checkResult(textStoreSink.OnSelectionChange());
        }
    }

    @Inject(method = "updateTextPosition", at = @At("TAIL"))
    private void tnze$updateTextPosition(CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            checkResult(textStoreSink.OnLayoutChange(TsLayoutCode.TS_LC_CHANGE, 0));
        }
    }

    @Inject(method = "scrollTo", at = @At("TAIL"))
    private void tnze$scrollTo(CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            checkResult(textStoreSink.OnLayoutChange(TsLayoutCode.TS_LC_CHANGE, 0));
        }
    }

    @Inject(method = "setEditable", at = @At("TAIL"))
    private void tnze$setEditable(boolean bl, CallbackInfo ci) {
        if (textStoreSink != null && sinkEnabled) {
            checkResult(textStoreSink.OnStatusChange(this.isEditable() ? 0 : TS_SD_READONLY));
        }
    }
}
