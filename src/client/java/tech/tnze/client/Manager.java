package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import tech.tnze.msctf.*;
import tech.tnze.msctf.windows.win32.system.com.Apis;
import tech.tnze.msctf.windows.win32.system.com.CLSCTX;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import static java.lang.foreign.ValueLayout.*;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.WindowsException.checkResult;
import static tech.tnze.msctf.windows.win32.foundation.Apis.*;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_NOINTERFACE;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.*;

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

    private Window window;
    private long hwnd;
    private ITfThreadMgrEx threadManager;
    private int clientId, editCookie;
    private ITfUIElementMgr uiElementMgr;

    public void init(Window window, long hwnd) {
        this.window = window;
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
            LOGGER.debug("ITfThreadManager client id: {}", clientId);
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

            LOGGER.debug("UIElementSink cookie={}", cookieHolder.get(JAVA_INT, 0));
        }
    }

    public void onWindowFocusChanged(boolean focused) {
        try (var arena = Arena.ofConfined()) {
            var prevDocumentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            threadManager.AssociateFocus(MemorySegment.ofAddress(hwnd), focused ? getActiveDocument() : MemorySegment.NULL, prevDocumentMgrHolder);
            var prevDocumentMgr = prevDocumentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            LOGGER.debug("Set focused {}: previous ITfDocumentMgr={}", focused, prevDocumentMgr);
            if (prevDocumentMgr.address() != 0) {
                ITfDocumentMgr.wrap(prevDocumentMgr).Release();
            }
        }
    }

    public MemorySegment getActiveDocument() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return MemorySegment.NULL;
        }

        var entry = documents.get(screen);
        if (entry == null) {
            return MemorySegment.NULL;
        }

        var doc = entry.get(screen.getFocused());
        if (doc == null) {
            return MemorySegment.NULL;
        }

        return doc.documentMgrPtr;
    }

    private static class Document implements AutoCloseable {
        private final ITfDocumentMgr documentMgr;
        private final MemorySegment documentMgrPtr;
        private final Context mainContext;

        Document(ITfThreadMgrEx threadMgr, int clientId, Window window, EditBox editBox) {
            try (var arena = Arena.ofConfined()) {
                var documentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
                checkResult(threadMgr.CreateDocumentMgr(documentMgrHolder));
                documentMgrPtr = documentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
                documentMgr = ITfDocumentMgr.wrap(documentMgrPtr);
            }

            mainContext = new Context(documentMgr, clientId, window, editBox);
            documentMgr.Push(mainContext.contextPtr);
        }

        @Override
        public void close() {
            mainContext.close();
            documentMgr.Release();
        }
    }

    private static class Context implements AutoCloseable {
        private Arena arena = Arena.ofConfined();
        private final ITextStoreACP2 textStoreACP;
        private final MemorySegment contextPtr, textStorePtr;
        private final ITfContext context;
        private final int editCookie;

        Context(ITfDocumentMgr documentMgr, int clientId, Window window, EditBox editBox) {
            textStoreACP = new EditBoxACP(window, editBox) {
                private int refCount = 1;

                @Override
                public int QueryInterface(MemorySegment riid, MemorySegment ppvObject) {
                    if (ComObject.equalIIDs(riid, IUnknown.iid()) || ComObject.equalIIDs(riid, ITextStoreACP2.iid())) {
                        ppvObject.set(ADDRESS, 0, textStorePtr);
                        return 0;
                    }
                    return E_NOINTERFACE;
                }

                @Override
                public int AddRef() {
                    return ++refCount;
                }

                @Override
                public int Release() {
                    if (--refCount == 0) {
                        arena.close();
                        arena = null;
                    }
                    return refCount;
                }
            };
            textStorePtr = ITextStoreACP2.create(textStoreACP, arena);

            try (var arena = Arena.ofConfined()) {
                var contextHolder = arena.allocate(ADDRESS.withTargetLayout(ITfContext.addressLayout()));
                var editCookieHolder = arena.allocate(JAVA_INT);
                checkResult(documentMgr.CreateContext(clientId, 0, textStorePtr, contextHolder, editCookieHolder));

                contextPtr = contextHolder.get(ITfContext.addressLayout(), 0);
                context = ITfContext.wrap(contextPtr);
                editCookie = editCookieHolder.get(JAVA_INT, 0);
            }
        }

        public int getEditCookie() {
            return editCookie;
        }

        @Override
        public void close() {
            textStoreACP.Release();
        }
    }

    private HashMap<Screen, HashMap<Object, Document>> documents = new HashMap<>();

    public void onScreenAdded(Screen screen) {
        this.documents.put(screen, new HashMap<>());
    }

    public void onScreenRemoved(Screen screen) {
        var contexts = this.documents.remove(screen);
        if (contexts != null) {
            contexts.values().forEach(Document::close);
        }
    }

    public void onScreenFocusedOnWidget(Screen screen, @Nullable GuiEventListener guiEventListener) {
        var entry = documents.get(screen);
        if (entry == null) {
            LOGGER.warn("Screen {} is not added, ignoring its focus: {}", screen, guiEventListener);
            return;
        }

        MemorySegment documentMgr = MemorySegment.NULL;
        if (guiEventListener instanceof EditBox editBox) {
            var document = entry.get(editBox);
            if (document == null) {
                LOGGER.info("EditBox focused, creating documentMgr");
                document = new Document(threadManager, clientId, window, editBox);
                entry.put(editBox, document);
            }
            documentMgr = document.documentMgrPtr;
        }

        try (var arena = Arena.ofConfined()) {
            var prevDocumentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            threadManager.AssociateFocus(MemorySegment.ofAddress(hwnd), documentMgr, prevDocumentMgrHolder);
            var prevDocumentMgr = prevDocumentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            if (prevDocumentMgr.address() != 0) {
                ITfDocumentMgr.wrap(prevDocumentMgr).Release();
            }
        }
    }

    private class UIElementSink extends ComObject implements ITfUIElementSink {
        private final static MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfUIElementSink.iid()};

        public UIElementSink() {
            super(implementedIIDs);
        }

        @Override
        public int BeginUIElement(int dwUIElementId, MemorySegment pbShow) {
            LOGGER.debug("BeginUIElement {}", dwUIElementId);
            try (var arena = Arena.ofConfined()) {
                var uiElementHolder = arena.allocate(ADDRESS.withTargetLayout(ITfUIElement.addressLayout()));
                int result = uiElementMgr.GetUIElement(dwUIElementId, uiElementHolder);
                if (result < 0) {
                    LOGGER.error("Failed to get ITfUIElement {}: {}", dwUIElementId, new WindowsException(result));
                    return result;
                }

                var uiElement = ITfUIElement.wrap(uiElementHolder.get(ITfUIElement.addressLayout(), 0));

                var candidateListUIElementHolder = arena.allocate(ADDRESS.withTargetLayout(ITfCandidateListUIElement.addressLayout()));
                result = uiElement.QueryInterface(ITfCandidateListUIElement.iid(), candidateListUIElementHolder);
                if (result >= 0) {
                    var candidateListUIElement = ITfCandidateListUIElement.wrap(candidateListUIElementHolder.get(ITfCandidateListUIElement.addressLayout(), 0));
                    synchronized (uiElements) {
                        var candidateList = new CandidateList(Minecraft.getInstance());
                        uiElements.put(dwUIElementId, candidateList);
                    }
                    candidateListUIElement.Release();
                    pbShow.set(JAVA_BOOLEAN, 0, true);
                } else {
                    var guidHolder = arena.allocate(tech.tnze.msctf.system.Guid.layout());
                    checkResult(uiElement.GetGUID(guidHolder));
                    LOGGER.warn("Unsupported UIElement: {}", Guid.toString(guidHolder));
                    pbShow.set(JAVA_BOOLEAN, 0, true);
                }

                uiElement.Release();
            }
            return 0;
        }

        @Override
        public int UpdateUIElement(int dwUIElementId) {
            LOGGER.debug("UpdateUIElement {}", dwUIElementId);
            try (var arena = Arena.ofConfined()) {
                var uiElementHolder = arena.allocate(ADDRESS.withTargetLayout(ITfUIElement.addressLayout()));
                int result = uiElementMgr.GetUIElement(dwUIElementId, uiElementHolder);
                if (result < 0) {
                    LOGGER.error("Failed to get ITfUIElement {}: {}", dwUIElementId, new WindowsException(result));
                    return result;
                }

                var uiElement = ITfUIElement.wrap(uiElementHolder.get(ITfUIElement.addressLayout(), 0));

                var candidateListUIElementHolder = arena.allocate(ADDRESS.withTargetLayout(ITfCandidateListUIElement.addressLayout()));
                result = uiElement.QueryInterface(ITfCandidateListUIElement.iid(), candidateListUIElementHolder);
                if (result >= 0) {
                    var candidateListUIElement = ITfCandidateListUIElement.wrap(candidateListUIElementHolder.get(ITfCandidateListUIElement.addressLayout(), 0));
                    synchronized (uiElements) {
                        var candidateList = (CandidateList) uiElements.get(dwUIElementId);
                        updateCandidateList(candidateListUIElement, candidateList);
                    }
                    candidateListUIElement.Release();
                }

                uiElement.Release();
            }
            return 0;
        }

        @Override
        public int EndUIElement(int dwUIElementId) {
            LOGGER.debug("EndUIElement {}", dwUIElementId);
            synchronized (uiElements) {
                uiElements.remove(dwUIElementId);
            }
            return 0;
        }

        private void updateCandidateList(ITfCandidateListUIElement elem, CandidateList list) {
            try (var arena = Arena.ofConfined()) {
                var countHolder = arena.allocate(JAVA_INT);
                checkResult(elem.GetCount(countHolder));
                int count = countHolder.get(JAVA_INT, 0);

                var pageCountHolder = arena.allocate(JAVA_INT);
                checkResult(elem.GetPageIndex(MemorySegment.NULL, 0, pageCountHolder));
                int pageCount = pageCountHolder.get(JAVA_INT, 0);

                var pageIndexHolder = arena.allocate(JAVA_INT, pageCount);
                checkResult(elem.GetPageIndex(pageIndexHolder, pageCount, pageCountHolder));
                var pageIndex = pageIndexHolder.toArray(JAVA_INT);

                var currentPageHolder = arena.allocate(JAVA_INT);
                checkResult(elem.GetCurrentPage(currentPageHolder));
                int currentPage = currentPageHolder.get(JAVA_INT, 0);

                int currentPageStart = pageIndex[currentPage];
                int currentPageEnd = currentPage + 1 < pageIndex.length ? pageIndex[currentPage + 1] : count;

                var currentPageContents = new String[currentPageEnd - currentPageStart];
                var candidateWordHolder = arena.allocate(ADDRESS);
                for (int i = currentPageStart; i < currentPageEnd; i++) {
                    checkResult(elem.GetString(i, candidateWordHolder));
                    var candidateWord = candidateWordHolder.get(ADDRESS, 0);
                    int candidateWordLen = SysStringLen(candidateWord);
                    var chars = candidateWord.reinterpret(candidateWordLen * JAVA_CHAR.byteSize()).toArray(JAVA_CHAR);
                    currentPageContents[i - currentPageStart] = String.valueOf(chars);
                    SysFreeString(candidateWord);
                }

                var currentSelectionHolder = arena.allocate(JAVA_INT);
                checkResult(elem.GetSelection(currentSelectionHolder));
                int currentSelection = currentSelectionHolder.get(JAVA_INT, 0);

                list.setState(count, pageCount, currentPage, currentPageContents, currentSelection - currentPageStart);
            }
        }
    }
}