package tech.tnze.client;

import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;

public interface ACPSinkRegister {
    default void tnze$registerACPSink(AbstractTextFieldACP acpImpl, ITextStoreACPSink sink) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    default void tnze$unregisterACPSink(AbstractTextFieldACP acpImpl, ITextStoreACPSink sink) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    default void tnze$setSinkEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
