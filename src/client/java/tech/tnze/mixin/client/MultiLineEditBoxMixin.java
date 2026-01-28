package tech.tnze.mixin.client;

import net.minecraft.client.gui.components.MultiLineEditBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tech.tnze.client.ACPSinkRegister;
import tech.tnze.client.AbstractEditBoxACP;
import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;

@Mixin(MultiLineEditBox.class)
public class MultiLineEditBoxMixin implements ACPSinkRegister {

    @Unique
    @Nullable
    private ITextStoreACPSink textStoreSink;

    @Unique
    private boolean sinkEnabled = true;

    @Override
    public void tnze$registerACPSink(AbstractEditBoxACP acpImpl, ITextStoreACPSink sink) {
        textStoreSink = sink;
    }

    @Override
    public void tnze$unregisterACPSink(AbstractEditBoxACP acpImpl, ITextStoreACPSink sink) {
        assert sink.equals(textStoreSink);
        textStoreSink = null;
    }

    @Override
    public void tnze$setSinkEnabled(boolean enabled) {
        sinkEnabled = enabled;
    }

}
