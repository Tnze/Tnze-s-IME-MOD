package tech.tnze.msctf;

import windows.win32.system.com.Apis;
import windows.win32.system.com.CLSCTX;
import windows.win32.ui.textservices.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

import static windows.win32.ui.textservices.Constants.CLSID_TF_ThreadMgr;

public class ThreadManager implements AutoCloseable {
    private final Arena arena;
    private final ITfThreadMgrEx inner;

    public static final int TF_TMAE_UIELEMENTENABLEDONLY = Constants.TF_TMAE_UIELEMENTENABLEDONLY;

    public ThreadManager(Arena a) throws WindowsException {
        this.arena = a;
        try (var arena = Arena.ofConfined()) {
            var threadMgrHolder = arena.allocate(ValueLayout.ADDRESS.withTargetLayout(ITfThreadMgrEx.addressLayout()));
            WindowsException.checkResult(Apis.CoCreateInstance(
                    CLSID_TF_ThreadMgr(),
                    MemorySegment.NULL,
                    CLSCTX.INPROC_SERVER,
                    ITfThreadMgrEx.iid(),
                    threadMgrHolder
            ));

            var threadMgr = threadMgrHolder.get(ITfThreadMgr.addressLayout(), 0);
            inner = ITfThreadMgrEx.wrap(threadMgr);
        }
    }

    public int activateEx(int flags) throws WindowsException {
        try (var arena = Arena.ofConfined()) {
            var clientIdHolder = arena.allocate(ValueLayout.JAVA_INT);
            WindowsException.checkResult(this.inner.ActivateEx(clientIdHolder, flags));
            return clientIdHolder.get(ValueLayout.JAVA_INT, 0);
        }
    }

    public DocumentManager associateFocus(long hwnd, DocumentManager documentManager) throws WindowsException {
        try (var arena = Arena.ofConfined()) {
            var handle = arena.allocate(ValueLayout.JAVA_LONG, hwnd);
            var docMgrHolder = arena.allocate(ITfDocumentMgr.addressLayout());
            // 无法从 ITfDocumentMgr 转换为 MemorySegment，才采取该办法
            WindowsException.checkResult(documentManager.inner.QueryInterface(ITfDocumentMgr.iid(), docMgrHolder));
            var prevHolder = this.arena.allocate(ITfDocumentMgr.addressLayout());
            WindowsException.checkResult(inner.AssociateFocus(handle, docMgrHolder, prevHolder));
            return new DocumentManager(ITfDocumentMgr.wrap(prevHolder));
        }
    }

    public void deactivate() {
        int ret = inner.Deactivate();
    }

    public Source getSource() {
        var sourceHolder = arena.allocate(ITfSource.addressLayout());
        int ret = inner.QueryInterface(ITfSource.iid(), sourceHolder);
        return new Source(ITfSource.wrap(sourceHolder));
    }

    public DocumentManager createDocumentManager() {
        var documentMgrHolder = arena.allocate(ITfDocumentMgr.addressLayout());
//        inner.CreateDocumentMgr()
        return null;
    }

    public native UIElementManager getUIElementManager();

    public native void enumDocumentManagers(Consumer<DocumentManager> consumer);

    @Override
    public void close() {
        inner.Release();
    }
}
