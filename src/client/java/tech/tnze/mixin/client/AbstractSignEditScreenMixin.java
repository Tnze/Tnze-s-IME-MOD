package tech.tnze.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.tnze.client.ACPSinkRegister;
import tech.tnze.client.Manager;
import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;

import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(AbstractSignEditScreen.class)
public class AbstractSignEditScreenMixin implements ACPSinkRegister {
    @Shadow
    private int line;

    @Inject(method = "keyPressed", at = @At(value = "RETURN"))
    public void tnze$keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (keyEvent.isUp() || keyEvent.isDown()) {
            LOGGER.info("SignEditScreen line shifted to {}", this.line);
            Manager.getInstance().onScreenFocusedChange((AbstractSignEditScreen) (Object) this);
        }
    }

    @Unique
    @Nullable
    private ITextStoreACPSink textStoreSink;

    @Unique
    private boolean sinkEnabled = true;

    @Override
    public void tnze$registerACPSink(ITextStoreACPSink sink) {
        textStoreSink = sink;
    }

    @Override
    public void tnze$unregisterACPSink(ITextStoreACPSink sink) {
        assert sink.equals(textStoreSink);
        textStoreSink = null;
    }

    @Override
    public void tnze$setSinkEnabled(boolean enabled) {
        sinkEnabled = enabled;
    }

}
