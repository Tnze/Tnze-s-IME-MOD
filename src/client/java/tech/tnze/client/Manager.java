package tech.tnze.client;

import net.minecraft.client.gui.components.Renderable;
import tech.tnze.msctf.*;
import windows.win32.system.com.Apis;
import windows.win32.system.com.CLSCTX;
import windows.win32.system.com.IUnknown;
import windows.win32.ui.textservices.*;

import static java.lang.foreign.ValueLayout.*;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.WindowsException.checkResult;
import static windows.win32.ui.textservices.Constants.CLSID_TF_ThreadMgr;
import static windows.win32.ui.textservices.Constants.TF_TMAE_UIELEMENTENABLEDONLY;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;

public class Manager {
    public static final TreeMap<Integer, Renderable> uiElements = new TreeMap<>();

    private static Manager instance = null;

    public synchronized static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    private Manager() {
    }

    private long hwnd;
    private ITfThreadMgrEx threadManager;
    private int clientId;
    private ITfUIElementMgr uiElementMgr;
    private ITfDocumentMgr documentMgr;
    private MemorySegment documentMgrPtr;
    private ITfContext context;
    private MemorySegment contextPtr;

    public void init(long hwnd) {
        this.hwnd = hwnd;
        // Create ITfThreadMgr
        try (var arena = Arena.ofConfined()) {
            var threadMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfThreadMgrEx.addressLayout()));
            checkResult(Apis.CoCreateInstance(CLSID_TF_ThreadMgr(), MemorySegment.NULL, CLSCTX.INPROC_SERVER, ITfThreadMgrEx.iid(), threadMgrHolder));
            threadManager = ITfThreadMgrEx.wrap(threadMgrHolder.get(ITfThreadMgr.addressLayout(), 0));
        }

        // Activate Text Service
        try (var arena = Arena.ofConfined()) {
            var clientIdHolder = arena.allocate(JAVA_INT);
            checkResult(threadManager.ActivateEx(clientIdHolder, TF_TMAE_UIELEMENTENABLEDONLY));
            clientId = clientIdHolder.get(JAVA_INT, 0);
        }

        // Query ITfUIElementMgr
        try (var arena = Arena.ofConfined()) {
            var uiElementMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfUIElementMgr.addressLayout()));
            checkResult(threadManager.QueryInterface(ITfUIElementMgr.iid(), uiElementMgrHolder));
            uiElementMgr = ITfUIElementMgr.wrap(uiElementMgrHolder.get(ITfUIElementMgr.addressLayout(), 0));
        }

        // Register ITfUIElementSink
        try (var arena = Arena.ofConfined()) {
            var sourceHolder = arena.allocate(ADDRESS.withTargetLayout(ITfSource.addressLayout()));
            checkResult(uiElementMgr.QueryInterface(ITfSource.iid(), sourceHolder));
            var source = ITfSource.wrap(sourceHolder.get(ITfSource.addressLayout(), 0));

            var sink = new UIElementSink();
            var sinkUpcallWrapper = ITfUIElementSink.create(sink, Arena.global()); // TODO: Do we need global?
            sink.setThisPointer(sinkUpcallWrapper);

            var cookieHolder = arena.allocate(JAVA_INT);
            checkResult(source.AdviseSink(ITfUIElementSink.iid(), sinkUpcallWrapper, cookieHolder));
            source.Release();

            LOGGER.info("UIElementSink cookie={}", cookieHolder.get(JAVA_INT, 0));
        }

        // Create ITfDocumentMgr
        try (var arena = Arena.ofConfined()) {
            var documentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            checkResult(threadManager.CreateDocumentMgr(documentMgrHolder));
            documentMgrPtr = documentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            documentMgr = ITfDocumentMgr.wrap(documentMgrPtr);
        }

        // Create Context
        try (var arena = Arena.ofConfined()) {
            var sink = new TextStore();
            var sinkUpcallWrapper = ITfContextOwnerCompositionSink.create(sink, Arena.global());
            sink.setThisPointer(sinkUpcallWrapper);

            var contextHolder = arena.allocate(ADDRESS.withTargetLayout(ITfContext.addressLayout()));
            var editCookieHolder = arena.allocate(JAVA_INT);
            checkResult(documentMgr.CreateContext(clientId, 0, sinkUpcallWrapper, contextHolder, editCookieHolder));

            contextPtr = contextHolder.get(ITfContext.addressLayout(), 0);
            context = ITfContext.wrap(contextPtr);
            var editCookie = editCookieHolder.get(JAVA_INT, 0);
            LOGGER.info("Edit cookie={}", editCookie);

            // Push Context
            checkResult(documentMgr.Push(contextPtr));
        }
    }

    public void setFocus(boolean isFocused) {
        try (var arena = Arena.ofConfined()) {
            var prevDocumentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            threadManager.AssociateFocus(MemorySegment.ofAddress(hwnd), isFocused ? documentMgrPtr : MemorySegment.NULL, prevDocumentMgrHolder);
            var prevDocumentMgr = prevDocumentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            LOGGER.info("Set focused: previous ITfDocumentMgr={}", prevDocumentMgr);
        }
    }

    private class UIElementSink extends ComObject implements ITfUIElementSink {
        private final static MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfUIElementSink.iid()};

        public UIElementSink() {
            super(implementedIIDs);
        }

        @Override
        public int BeginUIElement(int dwUIElementId, MemorySegment pbShow) {
            LOGGER.info("BeginUIElement");
            return 0;
        }

        @Override
        public int UpdateUIElement(int dwUIElementId) {
            LOGGER.info("UpdateUIElement");
            return 0;
        }

        @Override
        public int EndUIElement(int dwUIElementId) {
            LOGGER.info("EndUIElement");
            return 0;
        }
    }

    private class TextStore extends ComObject implements ITfContextOwnerCompositionSink {
        private final static MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfUIElementSink.iid()};

        public TextStore() {
            super(implementedIIDs);
        }

        @Override
        public int OnStartComposition(MemorySegment pComposition, MemorySegment pfOk) {
            LOGGER.debug("OnStartComposition");
            pfOk.set(JAVA_BOOLEAN, 0, true);
            return 0;
        }

        @Override
        public int OnUpdateComposition(MemorySegment pComposition, MemorySegment pRangeNew) {
            LOGGER.debug("OnUpdateComposition");
            return 0;
        }

        @Override
        public int OnEndComposition(MemorySegment pComposition) {
            LOGGER.debug("OnEndComposition");
            return 0;
        }
    }
}