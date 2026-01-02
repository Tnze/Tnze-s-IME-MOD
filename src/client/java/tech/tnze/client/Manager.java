package tech.tnze.client;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import tech.tnze.msctf.*;
import windows.win32.foundation.RECT;
import windows.win32.system.com.Apis;
import windows.win32.system.com.CLSCTX;
import windows.win32.system.com.IUnknown;
import windows.win32.ui.textservices.*;

import static com.sun.jna.platform.win32.WinError.E_FAIL;
import static com.sun.jna.platform.win32.WinError.E_NOTIMPL;
import static java.lang.foreign.ValueLayout.*;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.WindowsException.checkResult;
import static windows.win32.foundation.Apis.*;
import static windows.win32.foundation.Constants.*;
import static windows.win32.system.ole.Constants.*;
import static windows.win32.ui.textservices.Constants.*;

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
    private int clientId, editCookie;
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
            LOGGER.info("ITfThreadManager client id: {}", clientId);
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
            var sinkUpcallWrapper2 = ITextStoreACP2.create(sink, Arena.global());
            sink.setThisPointer(sinkUpcallWrapper);
            sink.setThisPointer2(sinkUpcallWrapper2);

            var contextHolder = arena.allocate(ADDRESS.withTargetLayout(ITfContext.addressLayout()));
            var editCookieHolder = arena.allocate(JAVA_INT);
            checkResult(documentMgr.CreateContext(clientId, 0, sinkUpcallWrapper, contextHolder, editCookieHolder));

            contextPtr = contextHolder.get(ITfContext.addressLayout(), 0);
            context = ITfContext.wrap(contextPtr);
            editCookie = editCookieHolder.get(JAVA_INT, 0);
            LOGGER.info("Edit cookie={}", editCookie);

            // Push Context
            checkResult(documentMgr.Push(contextPtr));

            var rangeHolder = arena.allocate(ADDRESS.withTargetLayout(ITfRange.addressLayout()));
            checkResult(context.GetStart(editCookie, rangeHolder));
        }
    }

    public void setFocus(boolean isFocused) {
        try (var arena = Arena.ofConfined()) {
            var prevDocumentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            threadManager.AssociateFocus(MemorySegment.ofAddress(hwnd), isFocused ? documentMgrPtr : MemorySegment.NULL, prevDocumentMgrHolder);
            var prevDocumentMgr = prevDocumentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            LOGGER.info("Set focused {}: previous ITfDocumentMgr={}", isFocused, prevDocumentMgr);
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
            LOGGER.info("BeginUIElement {}", dwUIElementId);
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
                }

                uiElement.Release();
            }
            return 0;
        }

        @Override
        public int UpdateUIElement(int dwUIElementId) {
            LOGGER.info("UpdateUIElement {}", dwUIElementId);
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
            LOGGER.info("EndUIElement {}", dwUIElementId);
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

    private class TextStore extends ComObject implements ITfContextOwnerCompositionSink, ITextStoreACP2 {
        private final static MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfContextOwnerCompositionSink.iid(), ITextStoreACP2.iid()};

        private MemorySegment thisPointer2;
        private MemorySegment sinkPtr;
        private ITextStoreACPSink sink;

        private final StringBuilder buffer = new StringBuilder();
        private Selection cursor = new Selection(0, 0);

        private boolean editLock;
        private int pendingLock; // Ignore the warning! The static analyzing failed when crossing ffi bounding

        public TextStore() {
            super(implementedIIDs);
        }

        private record Selection(int start, int end) {
        }

        public void setThisPointer2(MemorySegment thisPointer2) {
            this.thisPointer2 = thisPointer2;
        }

        @Override
        public int QueryInterface(MemorySegment riid, MemorySegment ppvObject) {
            if (ppvObject.address() == 0L) {
                return E_POINTER;
            }
            if (equalIIDs(riid, ITextStoreACP2.iid())) {
                ppvObject.set(ADDRESS, 0, thisPointer2);
                AddRef();
                return 0;
            }
            int ret = super.QueryInterface(riid, ppvObject);
            if (ret != 0) {
                LOGGER.warn("QueryInterface {} failed: {}", Guid.toString(riid), new WindowsException(ret));
            }
            return ret;
        }

        @Override
        public int OnStartComposition(MemorySegment pComposition, MemorySegment pfOk) {
            LOGGER.info("OnStartComposition");
            pfOk.set(JAVA_BOOLEAN, 0, true);
            return 0;
        }

        @Override
        public int OnUpdateComposition(MemorySegment pComposition, MemorySegment pRangeNew) {
            LOGGER.info("OnUpdateComposition");
            return 0;
        }

        @Override
        public int OnEndComposition(MemorySegment pComposition) {
            LOGGER.info("OnEndComposition");
            try (var arena = Arena.ofConfined()) {
                var composition = ITfCompositionView.wrap(pComposition);
                var rangeHolder = arena.allocate(ADDRESS.withTargetLayout(ITfRange.addressLayout()));
                int result = composition.GetRange(rangeHolder);
                if (result < 0) {
                    return result;
                }

                var range = ITfRange.wrap(rangeHolder.get(ITfRange.addressLayout(), 0));
                var buffer = arena.allocate(JAVA_CHAR, 128);
                var cchHolder = arena.allocate(JAVA_INT);
                range.GetText(editCookie, TF_TF_IGNOREEND, buffer, 128, cchHolder);
                var cch = cchHolder.get(JAVA_INT, 0);
                var chars = buffer.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);
                var compositionResult = String.valueOf(chars);

                LOGGER.info("Composition result: {}", compositionResult);

                var minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    Screen screen = minecraft.screen;
                    if (screen != null && minecraft.getOverlay() == null) {
                        compositionResult.codePoints().forEach(ch -> {
                            CharacterEvent characterEvent = new CharacterEvent(ch, 0);
                            try {
                                screen.charTyped(characterEvent);
                            } catch (Throwable var8) {
                                CrashReport crashReport = CrashReport.forThrowable(var8, "charTyped event handler");
                                screen.fillCrashDetails(crashReport);
                                CrashReportCategory crashReportCategory = crashReport.addCategory("Key");
                                crashReportCategory.setDetail("Codepoint", characterEvent.codepoint());
                                crashReportCategory.setDetail("Mods", characterEvent.modifiers());
                                throw new ReportedException(crashReport);
                            }
                        });
                    }
                });

                range.Release();
            }
            return 0;
        }

        @Override
        public int AdviseSink(MemorySegment riid, MemorySegment punk, int dwMask) {
            if (!equalIIDs(riid, ITextStoreACPSink.iid())) {
                return E_INVALIDARG;
            }
            if (sinkPtr != null) {
                LOGGER.error("Sink is already advised: {}", punk);
                return CONNECT_E_ADVISELIMIT;
            }
            try (var arena = Arena.ofConfined()) {
                var sinkHolder = arena.allocate(ADDRESS.withTargetLayout(ITextStoreACPSink.addressLayout()));
                int result = IUnknown.wrap(punk).QueryInterface(riid, sinkHolder);
                if (result < 0) {
                    return result;
                }
                sink = ITextStoreACPSink.wrap(sinkHolder.get(ITextStoreACPSink.addressLayout(), 0));
                sinkPtr = punk;
            }
            return 0;
        }

        @Override
        public int UnadviseSink(MemorySegment punk) {
            if (!punk.equals(sinkPtr)) {
                LOGGER.error("Failed to unadvise sink: {}", punk);
                return CONNECT_E_NOCONNECTION;
            }
            sinkPtr = null;
            sink = null;
            return 0;
        }

        @Override
        public int RequestLock(int dwLockFlags, MemorySegment phrSession) {
            // TODO: No idea how to implement Read-Write locking here
            synchronized (buffer) {
                if (editLock) {
                    // Already locked
                    if ((dwLockFlags & TS_LF_SYNC) != 0) {
                        phrSession.set(JAVA_INT, 0, TS_E_SYNCHRONOUS);
                    } else {
                        pendingLock = dwLockFlags;
                        phrSession.set(JAVA_INT, 0, TS_S_ASYNC);
                    }
                    return 0;
                }
                try {
                    editLock = true;
                    pendingLock = 0;
                    int result = 0;
                    if (sink != null) {
                        result = sink.OnLockGranted(dwLockFlags);
                    }
                    // For lock upgrade support
                    if (pendingLock != 0) {
                        result = sink.OnLockGranted(pendingLock);
                    }
                    phrSession.set(JAVA_INT, 0, result);
                } finally {
                    editLock = false;
                }
            }
            return 0;
        }

        @Override
        public int GetStatus(MemorySegment pdcs) {
            TS_STATUS.dwDynamicFlags(pdcs, 0); // TODO: Support TS_SD_LOADING | TS_SD_READONLY
            TS_STATUS.dwStaticFlags(pdcs, TS_SS_NOHIDDENTEXT);
            return 0;
        }

        @Override
        public int QueryInsert(int acpTestStart, int acpTestEnd, int cch, MemorySegment pacpResultStart, MemorySegment pacpResultEnd) {
            // "If pacpResultStart and pacpResultEnd are the same as acpTextEnd,
            // the cursor is at the end of the inserted text after insertion."
            pacpResultStart.set(JAVA_INT, 0, acpTestEnd);
            pacpResultEnd.set(JAVA_INT, 0, acpTestEnd);
            return 0;
        }

        @Override
        public int GetSelection(int ulIndex, int ulCount, MemorySegment pSelection, MemorySegment pcFetched) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                if ((ulCount & 0xFFFFFFFFL) < 1) {
                    pcFetched.set(JAVA_INT, 0, 0);
                    return 0;
                }
                LOGGER.info("GetSelection: {}", debugBuffer());
                TS_SELECTION_ACP.acpStart(pSelection, cursor.start);
                TS_SELECTION_ACP.acpEnd(pSelection, cursor.end);
                try (var arena = Arena.ofConfined()) {
                    var style = arena.allocate(TS_SELECTIONSTYLE.layout());
                    TS_SELECTIONSTYLE.ase(style, TsActiveSelEnd.TS_AE_END);
                    TS_SELECTIONSTYLE.fInterimChar(style, 0);
                    TS_SELECTION_ACP.style(pSelection, style); // TODO: set value inplace
                }
                pcFetched.set(JAVA_INT, 0, 1);
            }
            return 0;
        }

        @Override
        public int SetSelection(int ulCount, MemorySegment pSelection) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                if ((ulCount & 0xFFFFFFFFL) != 1) {
                    return E_FAIL;
                }
                cursor = new Selection(TS_SELECTION_ACP.acpStart(pSelection), TS_SELECTION_ACP.acpEnd(pSelection));
                var style = TS_SELECTION_ACP.style(pSelection);
                var ase = TS_SELECTIONSTYLE.ase(style);
                var interim = TS_SELECTIONSTYLE.fInterimChar(style);
                LOGGER.info("SetSelection: {}, ase={}, interim={}", debugBuffer(), ase, interim);
            }
            return 0;
        }

        @Override
        public int GetText(int acpStart, int acpEnd, MemorySegment pchPlain, int cchPlainReq, MemorySegment pcchPlainRet, MemorySegment prgRunInfo, int cRunInfoReq, MemorySegment pcRunInfoRet, MemorySegment pacpNext) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                if (acpEnd == -1) {
                    acpEnd = buffer.length();
                }
                if (acpStart < 0 || acpStart > buffer.length() || acpEnd < acpStart || acpEnd > buffer.length()) {
                    return TF_E_INVALIDPOS;
                }

                LOGGER.info("GetText: {}", debugBuffer());

                var chars = new char[acpEnd - acpStart];
                buffer.getChars(acpStart, acpEnd, chars, 0);
                int count = Math.min(cchPlainReq, chars.length);
                MemorySegment.copy(MemorySegment.ofArray(chars), 0, pchPlain.reinterpret(cchPlainReq * JAVA_CHAR.byteSize()), 0, count * JAVA_CHAR.byteSize());
                pcchPlainRet.set(JAVA_INT, 0, count);

                if (cRunInfoReq > 0) {
                    TS_RUNINFO.uCount(prgRunInfo, count);
                    TS_RUNINFO.type(prgRunInfo, TsRunType.TS_RT_PLAIN);
                    pcRunInfoRet.set(JAVA_INT, 0, 1);
                }

                pacpNext.set(JAVA_INT, 0, acpStart + count);
            }
            return 0;
        }

        @Override
        public int SetText(int dwFlags, int acpStart, int acpEnd, MemorySegment pchText, int cch, MemorySegment pChange) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                LOGGER.info("SetText: {}, {}", acpStart, acpEnd);

                var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);
                var replacement = String.valueOf(chars);

                LOGGER.info("SetText before: {}", debugBuffer());
                buffer.replace(acpStart, acpEnd, replacement);
                int newEnd = acpStart + cch;

                // Fix selections. TODO: Related to gravity
                {
                    int delta = cch - (acpEnd - acpStart);
                    int start = cursor.start, end = cursor.end;
                    if (end < acpStart) {
                        // Do nothing
                    } else if (start < acpStart && end < acpEnd) {
                        end = acpStart;
                    } else if (start < acpStart) {
                        end += delta;
                    } else if (start < acpEnd && end < acpEnd) {
                        start = end = newEnd;
                    } else if (start < acpEnd) {
                        start = newEnd;
                        end += delta;
                    } else {
                        start += delta;
                        end += delta;
                    }
                    cursor = new Selection(start, end);
                }
                LOGGER.info("SetText after: {}", debugBuffer());

                TS_TEXTCHANGE.acpStart(pChange, acpStart);
                TS_TEXTCHANGE.acpOldEnd(pChange, acpEnd);
                TS_TEXTCHANGE.acpNewEnd(pChange, newEnd);
            }
            return 0;
        }

        @Override
        public int GetFormattedText(int acpStart, int acpEnd, MemorySegment ppDataObject) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                throw new UnsupportedOperationException(); // TODO
            }
        }

        @Override
        public int GetEmbedded(int acpPos, MemorySegment rguidService, MemorySegment riid, MemorySegment ppunk) {
            return E_NOTIMPL;
        }

        @Override
        public int QueryInsertEmbedded(MemorySegment pguidService, MemorySegment pFormatEtc, MemorySegment pfInsertable) {
            pfInsertable.set(JAVA_BOOLEAN, 0, false);
            return 0;
        }

        @Override
        public int InsertEmbedded(int dwFlags, int acpStart, int acpEnd, MemorySegment pDataObject, MemorySegment pChange) {
            return E_NOTIMPL;
        }

        @Override
        public int InsertTextAtSelection(int dwFlags, MemorySegment pchText, int cch, MemorySegment pacpStart, MemorySegment pacpEnd, MemorySegment pChange) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
            }
            return E_NOTIMPL;
        }

        @Override
        public int InsertEmbeddedAtSelection(int dwFlags, MemorySegment pDataObject, MemorySegment pacpStart, MemorySegment pacpEnd, MemorySegment pChange) {
            return E_NOTIMPL;
        }

        @Override
        public int RequestSupportedAttrs(int dwFlags, int cFilterAttrs, MemorySegment paFilterAttrs) {
            var elemSize = system.Guid.layout().byteSize();
            paFilterAttrs = paFilterAttrs.reinterpret(elemSize * cFilterAttrs);
            paFilterAttrs.elements(system.Guid.layout()).forEach(elem -> {
                LOGGER.info("Request supported attributes: {}", Guid.toString(elem));
            });
            return 0;
        }

        @Override
        public int RequestAttrsAtPosition(int acpPos, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags) {
            return 0;
        }

        @Override
        public int RequestAttrsTransitioningAtPosition(int acpPos, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags) {
            return 0;
        }

        @Override
        public int FindNextAttrTransition(int acpStart, int acpHalt, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags, MemorySegment pacpNext, MemorySegment pfFound, MemorySegment plFoundOffset) {
            return 0;
        }

        @Override
        public int RetrieveRequestedAttrs(int ulCount, MemorySegment paAttrVals, MemorySegment pcFetched) {
            pcFetched.set(JAVA_INT, 0, 0);
            return 0;
        }

        @Override
        public int GetEndACP(MemorySegment pacp) {
            synchronized (buffer) {
                if (!editLock) {
                    return TS_E_NOLOCK;
                }
                pacp.set(JAVA_INT, 0, buffer.length());
            }
            return 0;
        }

        @Override
        public int GetActiveView(MemorySegment pvcView) {
            pvcView.set(JAVA_INT, 0, 0); // TODO: What is a TsViewCookie?
            return 0;
        }

        @Override
        public int GetACPFromPoint(int vcView, MemorySegment ptScreen, int dwFlags, MemorySegment pacp) {
            return 0;
        }

        @Override
        public int GetTextExt(int vcView, int acpStart, int acpEnd, MemorySegment prc, MemorySegment pfClipped) {
            return TS_E_NOLAYOUT;
        }

        @Override
        public int GetScreenExt(int vcView, MemorySegment prc) {
            RECT.left(prc, 0);
            RECT.top(prc, 0);
            RECT.right(prc, 100);
            RECT.bottom(prc, 100);
            return 0;
        }

        private String debugBuffer() {
            return String.format("%s<anchor>%s</anchor>%s",
                    buffer.substring(0, cursor.start),
                    buffer.substring(cursor.start, cursor.end),
                    buffer.substring(cursor.end)
            );
        }
    }
}