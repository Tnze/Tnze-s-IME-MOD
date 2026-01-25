package tech.tnze.client.uielement;

import net.minecraft.client.Minecraft;
import tech.tnze.msctf.ComObject;
import tech.tnze.msctf.Guid;
import tech.tnze.msctf.WindowsException;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.ITfCandidateListUIElement;
import tech.tnze.msctf.windows.win32.ui.textservices.ITfUIElement;
import tech.tnze.msctf.windows.win32.ui.textservices.ITfUIElementMgr;
import tech.tnze.msctf.windows.win32.ui.textservices.ITfUIElementSink;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.client.Manager.uiElements;
import static tech.tnze.msctf.WindowsException.checkResult;
import static tech.tnze.msctf.windows.win32.foundation.Apis.SysFreeString;
import static tech.tnze.msctf.windows.win32.foundation.Apis.SysStringLen;

public class UIElementSink extends ComObject implements ITfUIElementSink {
    private final ITfUIElementMgr uiElementMgr;
    private final static MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfUIElementSink.iid()};

    public UIElementSink(ITfUIElementMgr uiElementMgr) {
        super(implementedIIDs);
        this.uiElementMgr = uiElementMgr;
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