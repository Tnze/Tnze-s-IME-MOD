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
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.client.Manager.uiElements;
import static tech.tnze.msctf.WindowsException.checkResult;

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
                    candidateList.onStart(candidateListUIElement);
                }
                candidateListUIElement.Release();
                pbShow.set(JAVA_BOOLEAN, 0, false);
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
                    candidateList.onUpdate(candidateListUIElement);
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
            if (result >= 0 && uiElements.remove(dwUIElementId) instanceof CandidateList candidateList) {
                var candidateListUIElement = ITfCandidateListUIElement.wrap(candidateListUIElementHolder.get(ITfCandidateListUIElement.addressLayout(), 0));
                synchronized (uiElements) {
                    candidateList.onEnd(candidateListUIElement);
                }
                candidateListUIElement.Release();
            }
            uiElement.Release();
        }
        return 0;
    }

}