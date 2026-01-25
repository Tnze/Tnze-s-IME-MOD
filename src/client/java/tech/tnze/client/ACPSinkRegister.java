package tech.tnze.client;

import tech.tnze.msctf.windows.win32.ui.textservices.ITextStoreACPSink;

public interface ACPSinkRegister {
    default void tnze$registerACPSink(ITextStoreACPSink sink) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    default void tnze$unregisterACPSink(ITextStoreACPSink sink) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    default void tnze$setSinkEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
