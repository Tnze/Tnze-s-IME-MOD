package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import tech.tnze.client.uielement.UIElementSink;
import tech.tnze.msctf.*;
import tech.tnze.msctf.windows.win32.system.com.Apis;
import tech.tnze.msctf.windows.win32.system.com.CLSCTX;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import static java.lang.foreign.ValueLayout.*;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.WindowsException.checkResult;
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
    private int clientId;
    private ITfUIElementMgr uiElementMgr;

    public synchronized void init(Window window, long hwnd) {
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

            var sink = new UIElementSink(uiElementMgr);
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

    private MemorySegment getActiveDocument() {
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

    public int getClientId() {
        return clientId;
    }

    private static class Document implements AutoCloseable {
        private ITfDocumentMgr documentMgr;
        private final MemorySegment documentMgrPtr;
        private Context mainContext;

        Document(ITfThreadMgrEx threadMgr, int clientId, Window window, EditBox editBox) {
            try (var arena = Arena.ofConfined()) {
                var documentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
                checkResult(threadMgr.CreateDocumentMgr(documentMgrHolder));
                documentMgrPtr = documentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
                documentMgr = ITfDocumentMgr.wrap(documentMgrPtr);
            }

            mainContext = new Context(documentMgr, clientId, window, editBox);
            checkResult(documentMgr.Push(mainContext.contextPtr));
        }

        @Override
        public void close() {
            checkResult(documentMgr.Pop(TF_POPF_ALL));
            mainContext.close();
            mainContext = null;
            documentMgr.Release();
            documentMgr = null;
        }
    }

    private static class Context implements AutoCloseable {
        private Arena arena = Arena.ofConfined();
        private ITextStoreACP2 textStoreACP;
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
            textStoreACP = null;
        }
    }

    private final HashMap<Screen, HashMap<Object, Document>> documents = new HashMap<>();

    public void onScreenAdded(Screen screen) {
        this.documents.put(screen, new HashMap<>());
        onScreenFocusedOnWidget(screen, screen.getFocused());
    }

    public void onScreenRemoved(Screen screen) {
        onScreenFocusedOnWidget(screen, null);
        var contexts = this.documents.remove(screen);
        if (contexts != null) {
            contexts.values().forEach(Document::close);
        }
    }

    public synchronized void onScreenFocusedOnWidget(Screen screen, @Nullable GuiEventListener guiEventListener) {
        if (threadManager == null) {
            LOGGER.warn("Not initialized yet");
            return;
        }

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

        LOGGER.info("AssociateFocus on {}", documentMgr);

        try (var arena = Arena.ofConfined()) {
            var prevDocumentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            checkResult(threadManager.AssociateFocus(MemorySegment.ofAddress(hwnd), documentMgr, prevDocumentMgrHolder));
            var prevDocumentMgr = prevDocumentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            if (prevDocumentMgr.address() != 0) {
                ITfDocumentMgr.wrap(prevDocumentMgr).Release();
            }
        }
    }
}