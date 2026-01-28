package tech.tnze.mixin.client;

import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.tnze.client.ACPSinkRegister;
import tech.tnze.client.AbstractEditBoxACP;
import tech.tnze.client.Manager;
import tech.tnze.client.SignEditLineACP;
import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;
import tech.tnze.msctf.windows.win32.ui.textservices.TS_TEXTCHANGE;

import java.lang.foreign.Arena;

import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.WindowsException.checkResult;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.TS_SD_READONLY;

@Mixin(AbstractSignEditScreen.class)
public class AbstractSignEditScreenMixin implements ACPSinkRegister {
    @Shadow
    public int line;

    @Shadow
    @Nullable
    public TextFieldHelper signField;

    @Shadow
    @Final
    public String[] messages;

    @Inject(method = "keyPressed", at = @At("RETURN"))
    public void tnze$keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (keyEvent.isUp() || keyEvent.isDown() || keyEvent.isConfirmation()) {
            LOGGER.info("SignEditScreen line shifted to {}", this.line);
            Manager.getInstance().onScreenFocusedChange((AbstractSignEditScreen) (Object) this);
        } else if (sinkEnabled) {
            var sink = textStoreSink[this.line];
            if (sink != null) {
                checkResult(sink.OnSelectionChange());
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    protected void tnze$init(CallbackInfo ci) {
        if (!sinkEnabled) {
            return;
        }
        for (int i = 0; i < textStoreSink.length; i++) {
            var sink = textStoreSink[i];
            if (sink != null) {
                checkResult(sink.OnStatusChange(this.line == i && this.signField != null ? 0 : TS_SD_READONLY));
                try (var arena = Arena.ofConfined()) {
                    var change = arena.allocate(TS_TEXTCHANGE.layout());
                    TS_TEXTCHANGE.acpStart(change, 0);
                    TS_TEXTCHANGE.acpOldEnd(change, 0);
                    TS_TEXTCHANGE.acpNewEnd(change, this.messages[i].length());
                    checkResult(sink.OnTextChange(0, change));
                }
                checkResult(sink.OnSelectionChange());
            }
        }
    }

    @Unique
    private int changeStart, changeEnd;

    @Inject(method = "setMessage", at = @At("HEAD"))
    protected void tnze$setMessage$head(String string, CallbackInfo ci) {
        changeStart = 0;
        changeEnd = this.messages[this.line].length();
    }

    @Inject(method = "setMessage", at = @At("TAIL"))
    protected void tnze$setMessage(String string, CallbackInfo ci) {
        if (!sinkEnabled) {
            return;
        }
        var sink = textStoreSink[this.line];
        if (sink != null) {
            try (var arena = Arena.ofConfined()) {
                var change = arena.allocate(TS_TEXTCHANGE.layout());
                TS_TEXTCHANGE.acpStart(change, changeStart);
                TS_TEXTCHANGE.acpOldEnd(change, changeEnd);
                TS_TEXTCHANGE.acpNewEnd(change, string.length());
                checkResult(sink.OnTextChange(0, change));
            }
        }
    }

    @Unique
    @Nullable
    private final ITextStoreACPSink[] textStoreSink = new ITextStoreACPSink[4];

    @Unique
    private boolean sinkEnabled = true;

    @Override
    public void tnze$registerACPSink(AbstractEditBoxACP acpImpl, ITextStoreACPSink sink) {
        int line = ((SignEditLineACP) acpImpl).getLine();
        textStoreSink[line] = sink;
    }

    @Override
    public void tnze$unregisterACPSink(AbstractEditBoxACP acpImpl, ITextStoreACPSink sink) {
        int line = ((SignEditLineACP) acpImpl).getLine();
        assert sink.equals(textStoreSink[line]);
        textStoreSink[line] = null;
    }

    @Override
    public void tnze$setSinkEnabled(boolean enabled) {
        sinkEnabled = enabled;
    }

}
