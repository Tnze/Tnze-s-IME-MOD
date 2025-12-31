package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfContext;
import windows.win32.ui.textservices.ITfDocumentMgr;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static tech.tnze.msctf.WindowsException.checkResult;

public class DocumentManager implements AutoCloseable {
    ITfDocumentMgr inner;
    public final static int TF_POPF_ALL = 0x1;

    DocumentManager(ITfDocumentMgr inner) {
        this.inner = inner;
    }

    public Context createContext(int clientId, int flags, ContextOwnerCompositionSink sink) {
        try (var arena = Arena.ofConfined()) {
            var contextHolder = arena.allocate(ADDRESS.withTargetLayout(ITfContext.addressLayout()));
            var editCookieHolder = arena.allocate(JAVA_INT);

            checkResult(inner.CreateContext(clientId, flags, sink.toComObject(), contextHolder, editCookieHolder));

            var context = ITfContext.wrap(contextHolder.get(ITfContext.addressLayout(), 0));
            return new Context(context, editCookieHolder.get(JAVA_INT, 0));
        }
    }

    public native void push(Context ctx);

    public native void pop(int flags);

    @Override
    public void close() throws Exception {
        inner.Release();
        inner = null; // Avoid double free
    }

}
