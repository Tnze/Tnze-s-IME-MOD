package tech.tnze.msctf;

public class DocumentManager implements AutoCloseable {
    private final long pointer;
    public final static int TF_POPF_ALL = 0x1;

    DocumentManager(long p) {
        pointer = p;
    }

    public native Context createContext(int clientId, int flags, ContextOwnerCompositionSink sink);
    public native void push(Context ctx);
    public native void pop(int flags);

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfDocumentMgr@" + Long.toHexString(pointer);
    }
}
