package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.components.EditBox;
import tech.tnze.msctf.Guid;
import tech.tnze.msctf.windows.win32.foundation.POINT;
import tech.tnze.msctf.windows.win32.foundation.RECT;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static com.sun.jna.platform.win32.WinError.E_NOTIMPL;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.ComObject.equalIIDs;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_FAIL;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_INVALIDARG;
import static tech.tnze.msctf.windows.win32.system.ole.Constants.CONNECT_E_ADVISELIMIT;
import static tech.tnze.msctf.windows.win32.system.ole.Constants.CONNECT_E_NOCONNECTION;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.*;
import static tech.tnze.msctf.windows.win32.ui.textservices.TEXT_STORE_LOCK_FLAGS.TS_LF_READ;
import static tech.tnze.msctf.windows.win32.ui.textservices.TEXT_STORE_LOCK_FLAGS.TS_LF_READWRITE;

public abstract class EditBoxACP implements ITextStoreACP2 {
    private Window window;
    private EditBox editBox;

    private MemorySegment sinkPtr;
    private ITextStoreACPSink sink;

    private LockState currentLockState = LockState.None;
    private LockState pendingAsyncLock = LockState.None;

    private enum LockState {
        None, ReadOnly, ReadWrite;

        int toLockFlags() {
            return switch (this) {
                case ReadOnly -> TS_LF_READ;
                case ReadWrite -> TS_LF_READWRITE;
                default -> 0;
            };
        }

        LockState sum(LockState state) {
            return this.ordinal() < state.ordinal() ? state : this;
        }
    }

    public EditBoxACP(Window window, EditBox editBox) {
        this.window = window;
        this.editBox = editBox;
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
        sink.Release();
        sinkPtr = null;
        sink = null;
        return 0;
    }

    @Override
    public int RequestLock(int dwLockFlags, MemorySegment phrSession) {
        LockState lockType;
        if ((dwLockFlags & TS_LF_READWRITE) != 0) {
            lockType = LockState.ReadWrite;
        } else if ((dwLockFlags & TS_LF_READ) != 0) {
            lockType = LockState.ReadOnly;
        } else {
            LOGGER.error("Cannot request lock because dwLockFlags {} is invalid", dwLockFlags);
            return E_FAIL;
        }

        if ((dwLockFlags & TS_LF_SYNC) == 0 && currentLockState != LockState.None) {
            // Submit the async lock
            pendingAsyncLock = pendingAsyncLock.sum(lockType);
            phrSession.set(JAVA_INT, 0, TS_S_ASYNC);
            return 0;
        }

        if (currentLockState != LockState.None) {
            phrSession.set(JAVA_INT, 0, TS_E_SYNCHRONOUS);
            return 0;
        }

        if (sink == null) {
            LOGGER.error("Cannot request lock because no ITextStoreACPSink is available");
            return E_FAIL;
        }

        // Permit the lock
        currentLockState = lockType;

        var result = sink.OnLockGranted(currentLockState.toLockFlags());

        // Release the lock
        if (currentLockState.ordinal() < pendingAsyncLock.ordinal()) {
            currentLockState = pendingAsyncLock;
            result = sink.OnLockGranted(currentLockState.toLockFlags());
        }
        currentLockState = LockState.None;
        pendingAsyncLock = LockState.None;

        phrSession.set(JAVA_INT, 0, result);
        return 0;
    }

    @Override
    public int GetStatus(MemorySegment pdcs) {
        TS_STATUS.dwDynamicFlags(pdcs, editBox.isEditable() ? 0 : TS_SD_READONLY);
        TS_STATUS.dwStaticFlags(pdcs, TS_SS_NOHIDDENTEXT);
        return 0;
    }

    @Override
    public int QueryInsert(int acpTestStart, int acpTestEnd, int cch, MemorySegment pacpResultStart, MemorySegment pacpResultEnd) {
        // Indicates that the cursor will be at the end of the inserted text after insertion.
        pacpResultStart.set(JAVA_INT, 0, acpTestEnd);
        pacpResultEnd.set(JAVA_INT, 0, acpTestEnd);
        return 0;
    }

    private static final VarHandle TS_SELECTION_ACP$style$ase$VH = TS_SELECTION_ACP.layout().varHandle(MemoryLayout.PathElement.groupElement("style"), MemoryLayout.PathElement.groupElement("ase"));
    private static final VarHandle TS_SELECTION_ACP$style$fInterimChar$VH = TS_SELECTION_ACP.layout().varHandle(MemoryLayout.PathElement.groupElement("style"), MemoryLayout.PathElement.groupElement("fInterimChar"));

    @Override
    public int GetSelection(int ulIndex, int ulCount, MemorySegment pSelection, MemorySegment pcFetched) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        // We have only one selection
        if (ulIndex != 0 && ulIndex != TF_DEFAULT_SELECTION) {
            LOGGER.warn("Selection {} not found", ulIndex);
            pcFetched.set(JAVA_INT, 0, 0);
            return 0;
        }

        if (ulCount < 1) {
            LOGGER.error("pSelection too small: {}", ulCount);
            return E_FAIL;
        }

        TS_SELECTION_ACP.acpStart(pSelection, Math.min(editBox.cursorPos, editBox.highlightPos));
        TS_SELECTION_ACP.acpEnd(pSelection, Math.max(editBox.cursorPos, editBox.highlightPos));
        TS_SELECTION_ACP$style$ase$VH.set(pSelection, 0, editBox.cursorPos > editBox.highlightPos ? TsActiveSelEnd.TS_AE_END : TsActiveSelEnd.TS_AE_START);
        TS_SELECTION_ACP$style$fInterimChar$VH.set(pSelection, 0, 0);
        pcFetched.set(JAVA_INT, 0, 1);

        return 0;
    }

    @Override
    public int SetSelection(int ulCount, MemorySegment pSelection) {
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }

        if (ulCount != 1) {
            LOGGER.error("Only support one selection, but setting {}", ulCount);
            return E_FAIL;
        }

        int start = TS_SELECTION_ACP.acpStart(pSelection);
        int end = TS_SELECTION_ACP.acpEnd(pSelection);
        int ase = (int) TS_SELECTION_ACP$style$ase$VH.get(pSelection, 0);

        if (ase == TsActiveSelEnd.TS_AE_START) {
            editBox.setHighlightPos(end);
            editBox.moveCursorTo(start, true);
        } else { // End or None
            editBox.setHighlightPos(start);
            editBox.moveCursorTo(end, true);
        }

        return 0;
    }

    @Override
    public int GetText(int acpStart, int acpEnd, MemorySegment pchPlain, int cchPlainReq, MemorySegment pcchPlainRet, MemorySegment prgRunInfo, int cRunInfoReq, MemorySegment pcRunInfoRet, MemorySegment pacpNext) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        String value = editBox.getValue();
        if (acpEnd == -1) {
            acpEnd = value.length();
        }
        if (acpStart < 0 || acpStart > value.length() || acpEnd < acpStart || acpEnd > value.length()) {
            return TS_E_INVALIDPOS;
        }

        var chars = new char[acpEnd - acpStart];
        value.getChars(acpStart, acpEnd, chars, 0);
        int count = Math.min(cchPlainReq, chars.length);
        MemorySegment.copy(MemorySegment.ofArray(chars), 0, pchPlain.reinterpret(cchPlainReq * JAVA_CHAR.byteSize()), 0, count * JAVA_CHAR.byteSize());
        pcchPlainRet.set(JAVA_INT, 0, count);

        if (cRunInfoReq > 0) {
            TS_RUNINFO.uCount(prgRunInfo, count);
            TS_RUNINFO.type(prgRunInfo, TsRunType.TS_RT_PLAIN);
            pcRunInfoRet.set(JAVA_INT, 0, 1);
        }

        pacpNext.set(JAVA_INT, 0, acpStart + count);
        return 0;
    }

    @Override
    public int SetText(int dwFlags, int acpStart, int acpEnd, MemorySegment pchText, int cch, MemorySegment pChange) {
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);
        var replacement = String.valueOf(chars);

        int newEnd = acpStart + cch;
        editBox.value = editBox.value.substring(0, acpStart) + replacement + editBox.value.substring(acpEnd);
        editBox.onValueChange(editBox.value);

        // Fix selections. TODO: Related to gravity
        {
            int delta = cch - (acpEnd - acpStart);
            int start = Math.min(editBox.cursorPos, editBox.highlightPos);
            int end = Math.max(editBox.cursorPos, editBox.highlightPos);
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
            editBox.highlightPos = start;
            editBox.cursorPos = end;
        }

        TS_TEXTCHANGE.acpStart(pChange, acpStart);
        TS_TEXTCHANGE.acpOldEnd(pChange, acpEnd);
        TS_TEXTCHANGE.acpNewEnd(pChange, newEnd);
        return 0;
    }

    @Override
    public int GetFormattedText(int acpStart, int acpEnd, MemorySegment ppDataObject) {
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }
        ppDataObject.set(ADDRESS, 0, MemorySegment.NULL);
        return 0;
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
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);

        int start = Math.min(editBox.cursorPos, editBox.highlightPos);
        int oldEnd = Math.max(editBox.cursorPos, editBox.highlightPos);
        editBox.insertText(String.valueOf(chars));

        pacpStart.set(JAVA_LONG, 0, Math.min(editBox.cursorPos, editBox.highlightPos));
        pacpEnd.set(JAVA_LONG, 0, Math.max(editBox.cursorPos, editBox.highlightPos));

        TS_TEXTCHANGE.acpStart(pChange, start);
        TS_TEXTCHANGE.acpOldEnd(pChange, oldEnd);
        TS_TEXTCHANGE.acpNewEnd(pChange, Math.max(editBox.cursorPos, editBox.highlightPos));

        return 0;
    }

    @Override
    public int InsertEmbeddedAtSelection(int dwFlags, MemorySegment pDataObject, MemorySegment pacpStart, MemorySegment pacpEnd, MemorySegment pChange) {
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }

        return E_FAIL;
    }

    @Override
    public int RequestSupportedAttrs(int dwFlags, int cFilterAttrs, MemorySegment paFilterAttrs) {
        var elemSize = tech.tnze.msctf.system.Guid.layout().byteSize();
        paFilterAttrs = paFilterAttrs.reinterpret(elemSize * cFilterAttrs);
        paFilterAttrs.elements(tech.tnze.msctf.system.Guid.layout()).forEach(elem -> {
            LOGGER.debug("Request supported attributes: {}", Guid.toString(elem));
        });
        return 0;
    }

    @Override
    public int RequestAttrsAtPosition(int acpPos, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags) {
        var elemSize = tech.tnze.msctf.system.Guid.layout().byteSize();
        paFilterAttrs = paFilterAttrs.reinterpret(elemSize * cFilterAttrs);
        paFilterAttrs.elements(tech.tnze.msctf.system.Guid.layout()).forEach(elem -> {
            LOGGER.debug("Request attributes at {}: {}", acpPos, Guid.toString(elem));
        });
        return 0;
    }

    @Override
    public int RequestAttrsTransitioningAtPosition(int acpPos, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags) {
        var elemSize = tech.tnze.msctf.system.Guid.layout().byteSize();
        paFilterAttrs = paFilterAttrs.reinterpret(elemSize * cFilterAttrs);
        paFilterAttrs.elements(tech.tnze.msctf.system.Guid.layout()).forEach(elem -> {
            LOGGER.debug("Request attributes transitioning at {}: {}", acpPos, Guid.toString(elem));
        });
        return 0;
    }

    @Override
    public int FindNextAttrTransition(int acpStart, int acpHalt, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags, MemorySegment pacpNext, MemorySegment pfFound, MemorySegment plFoundOffset) {
        pacpNext.set(JAVA_LONG, 0, editBox.value.length());
        pfFound.set(JAVA_BOOLEAN, 0, false);
        return 0;
    }

    @Override
    public int RetrieveRequestedAttrs(int ulCount, MemorySegment paAttrVals, MemorySegment pcFetched) {
        pcFetched.set(JAVA_INT, 0, 0);
        return 0;
    }

    @Override
    public int GetEndACP(MemorySegment pacp) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        pacp.set(JAVA_LONG, 0, editBox.value.length());
        return 0;
    }

    @Override
    public int GetActiveView(MemorySegment pvcView) {
        pvcView.set(JAVA_INT, 0, 0);
        return 0;
    }

    @Override
    public int GetACPFromPoint(int vcView, MemorySegment ptScreen, int dwFlags, MemorySegment pacp) {
        int windowX = (POINT.x(ptScreen) - window.getX()) / window.getGuiScale();
        int windowY = (POINT.y(ptScreen) - window.getY()) / window.getGuiScale();

        int widgetX = windowX - editBox.textX;
        int widgetY = windowY - editBox.textY;

        if (widgetX < 0 || widgetX >= editBox.getInnerWidth() || widgetY < 0 || widgetY >= editBox.font.lineHeight) {
            return TS_E_INVALIDPOINT;
        }

        String displayValue = editBox.value.substring(editBox.displayPos);
        int i = editBox.displayPos + editBox.font.plainSubstrByWidth(displayValue, widgetX).length();

        pacp.set(JAVA_LONG, 0, i);
        return 0;
    }

    @Override
    public int GetTextExt(int vcView, int acpStart, int acpEnd, MemorySegment prc, MemorySegment pfClipped) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        if (acpStart < 0 || acpStart > editBox.value.length() || acpEnd < acpStart || acpEnd > editBox.value.length()) {
            return TS_E_INVALIDPOS;
        }

        boolean clipped = false;
        if (acpStart < editBox.displayPos) {
            acpStart = editBox.displayPos;
            clipped = true;
        }
        if (acpEnd < editBox.displayPos) {
            acpEnd = editBox.displayPos;
        }
        int startOffset = editBox.font.width(editBox.value.substring(editBox.displayPos, acpStart));
        int endOffset = editBox.font.width(editBox.value.substring(editBox.displayPos, acpEnd));
        if (endOffset > editBox.getInnerWidth()) {
            endOffset = editBox.getInnerWidth();
            clipped = true;
        }

        RECT.top(prc, window.getY() + editBox.textY * window.getGuiScale());
        RECT.left(prc, window.getX() + (editBox.textX + startOffset) * window.getGuiScale());
        RECT.right(prc, window.getX() + (editBox.textX + endOffset) * window.getGuiScale());
        RECT.bottom(prc, window.getY() + (editBox.textY + editBox.font.lineHeight) * window.getGuiScale());

        pfClipped.set(JAVA_BOOLEAN, 0, clipped);

        return 0;
    }

    @Override
    public int GetScreenExt(int vcView, MemorySegment prc) {
        RECT.left(prc, window.getX());
        RECT.top(prc, window.getY());
        RECT.right(prc, window.getX() + window.getWidth());
        RECT.bottom(prc, window.getY() + window.getHeight());
        return 0;
    }

}
