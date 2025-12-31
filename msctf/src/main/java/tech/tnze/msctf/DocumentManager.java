package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfDocumentMgr;

public class DocumentManager implements AutoCloseable {
    final ITfDocumentMgr inner;
    public final static int TF_POPF_ALL = 0x1;

    DocumentManager(ITfDocumentMgr inner) {
        this.inner = inner;
    }

    public native Context createContext(int clientId, int flags, ContextOwnerCompositionSink sink);
    public native void push(Context ctx);
    public native void pop(int flags);

    @Override
    public native void close() throws Exception;

}
