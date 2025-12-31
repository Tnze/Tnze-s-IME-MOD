package tech.tnze.msctf;

import windows.win32.system.com.Apis;
import windows.win32.system.com.CLSCTX;
import windows.win32.ui.textservices.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static tech.tnze.msctf.WindowsException.checkResult;
import static windows.win32.ui.textservices.Constants.CLSID_TF_ThreadMgr;

public class ThreadManager implements AutoCloseable {
    private final Arena arena;
    private ITfThreadMgrEx inner;

    public static final int TF_TMAE_UIELEMENTENABLEDONLY = Constants.TF_TMAE_UIELEMENTENABLEDONLY;

    public ThreadManager() throws WindowsException {
        this.arena = Arena.ofAuto();
        try (var arena = Arena.ofConfined()) {
            var threadMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfThreadMgrEx.addressLayout()));
            checkResult(Apis.CoCreateInstance(
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
            checkResult(this.inner.ActivateEx(clientIdHolder, flags));
            return clientIdHolder.get(ValueLayout.JAVA_INT, 0);
        }
    }

    public DocumentManager associateFocus(long hwnd, DocumentManager documentManager) throws WindowsException {
        try (var arena = Arena.ofConfined()) {
            var handle = arena.allocate(ValueLayout.JAVA_LONG, hwnd);
            // 将 ITfDocumentMgr 转换为 MemorySegment
            var docMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            checkResult(documentManager.inner.QueryInterface(ITfDocumentMgr.iid(), docMgrHolder));
            var docMgr = docMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);

            var prevHolder = this.arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            checkResult(inner.AssociateFocus(handle, docMgr, prevHolder));
            return new DocumentManager(ITfDocumentMgr.wrap(prevHolder.get(ITfDocumentMgr.addressLayout(), 0)));
        }
    }

    public void deactivate() {
        checkResult(inner.Deactivate());
    }

    public Source getSource() {
        try (var arena = Arena.ofConfined()) {
            var sourceHolder = arena.allocate(ADDRESS.withTargetLayout(ITfSource.addressLayout()));
            checkResult(inner.QueryInterface(ITfSource.iid(), sourceHolder));
            return new Source(ITfSource.wrap(sourceHolder.get(ITfSource.addressLayout(), 0)));
        }
    }

    public DocumentManager createDocumentManager() {
        try (var arena = Arena.ofConfined()) {
            var documentMgrHolder = arena.allocate(ITfDocumentMgr.addressLayout());
            checkResult(inner.CreateDocumentMgr(documentMgrHolder));
            var documentMgr = documentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            return new DocumentManager(ITfDocumentMgr.wrap(documentMgr));
        }
    }

    public UIElementManager getUIElementManager() {
        try (var arena = Arena.ofConfined()) {
            var uiElementMgrHolder = arena.allocate(ITfUIElementMgr.addressLayout());
            checkResult(inner.QueryInterface(ITfUIElementMgr.iid(), uiElementMgrHolder));
            var uiElementMgr = uiElementMgrHolder.get(ITfUIElementMgr.addressLayout(), 0);
            return new UIElementManager(ITfUIElementMgr.wrap(uiElementMgr));
        }
    }

    @Override
    public void close() {
        inner.Release();
        inner = null; // Avoid double free
    }
}
