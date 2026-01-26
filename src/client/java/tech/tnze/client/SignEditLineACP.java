package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_FAIL;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.*;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.TF_DEFAULT_SELECTION;

public class SignEditLineACP extends AbstractTextFieldACP {
    private final int line;
    private final AbstractSignEditScreen screen;

    public SignEditLineACP(Window window, AbstractSignEditScreen screen, int line) {
        super(window);
        this.line = line;
        this.screen = screen;
    }

    public int getLine() {
        return line;
    }

    @Override
    protected void adviseACPSink(ITextStoreACPSink sink) {
        screen.tnze$registerACPSink(this, sink);
    }

    @Override
    protected void unadviseACPSink(ITextStoreACPSink sink) {
        screen.tnze$unregisterACPSink(this, sink);
    }

    @Override
    public int GetStatus(MemorySegment pdcs) {
        TS_STATUS.dwDynamicFlags(pdcs, screen.line == line && screen.signField != null ? 0 : TS_SD_READONLY);
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

        int cursorPos = 0, selectionPos = 0;
        if (screen.signField != null) {
            cursorPos = screen.signField.getCursorPos();
            selectionPos = screen.signField.getSelectionPos();
        }

        TS_SELECTION_ACP.acpStart(pSelection, Math.min(cursorPos, selectionPos));
        TS_SELECTION_ACP.acpEnd(pSelection, Math.max(cursorPos, selectionPos));
        TS_SELECTION_ACP$style$ase$VH.set(pSelection, 0, cursorPos > selectionPos ? TsActiveSelEnd.TS_AE_END : TsActiveSelEnd.TS_AE_START);
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

        if (screen.line != line || screen.signField == null) {
            return E_FAIL; // This line is not selected
        }

        int start = TS_SELECTION_ACP.acpStart(pSelection);
        int end = TS_SELECTION_ACP.acpEnd(pSelection);
        int ase = (int) TS_SELECTION_ACP$style$ase$VH.get(pSelection, 0);


        screen.tnze$setSinkEnabled(false);
        if (ase == TsActiveSelEnd.TS_AE_START) {
            screen.signField.setSelectionPos(end);
            screen.signField.setCursorPos(start);
        } else { // End or None
            screen.signField.setSelectionPos(start);
            screen.signField.setCursorPos(end);
        }
        screen.tnze$setSinkEnabled(true);

        return 0;
    }

    @Override
    public int GetText(int acpStart, int acpEnd, MemorySegment pchPlain, int cchPlainReq, MemorySegment pcchPlainRet, MemorySegment prgRunInfo, int cRunInfoReq, MemorySegment pcRunInfoRet, MemorySegment pacpNext) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        String value = screen.messages[line];
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

        if (screen.line != line || screen.signField == null) {
            return TS_E_READONLY; // This line is not selected
        }

        String value = screen.messages[line];
        if (acpStart < 0 || acpStart > value.length() || acpEnd < acpStart || acpEnd > value.length()) {
            return TS_E_INVALIDPOS;
        }

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);
        var replacement = String.valueOf(chars);

        int newEnd = acpStart + cch;
        value = value.substring(0, acpStart) + replacement + value.substring(acpEnd);
        // Fix selections. TODO: Related to gravity
        {
            int delta = cch - (acpEnd - acpStart);
            int cursorPos = screen.signField.getCursorPos();
            int selectionPos = screen.signField.getSelectionPos();
            int start = Math.min(cursorPos, selectionPos);
            int end = Math.max(cursorPos, selectionPos);
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
            screen.signField.setSelectionPos(start);
            screen.signField.setCursorPos(end);
        }

        screen.tnze$setSinkEnabled(false);
        screen.setMessage(value);
        screen.tnze$setSinkEnabled(true);

        TS_TEXTCHANGE.acpStart(pChange, acpStart);
        TS_TEXTCHANGE.acpOldEnd(pChange, acpEnd);
        TS_TEXTCHANGE.acpNewEnd(pChange, newEnd);
        return 0;
    }

    @Override
    public int InsertTextAtSelection(int dwFlags, MemorySegment pchText, int cch, MemorySegment pacpStart, MemorySegment pacpEnd, MemorySegment pChange) {
        if (currentLockState.ordinal() < LockState.ReadWrite.ordinal()) {
            return TS_E_NOLOCK;
        }

        if (screen.line != line || screen.signField == null) {
            return TS_E_READONLY; // This line is not selected
        }

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);

        int cursorPos = screen.signField.getCursorPos();
        int selectionPos = screen.signField.getSelectionPos();
        int start = Math.min(cursorPos, selectionPos);
        int oldEnd = Math.max(cursorPos, selectionPos);

        screen.tnze$setSinkEnabled(false);
        screen.signField.insertText(String.valueOf(chars));
        screen.tnze$setSinkEnabled(true);

        cursorPos = screen.signField.getCursorPos();
        selectionPos = screen.signField.getSelectionPos();
        pacpStart.set(JAVA_LONG, 0, Math.min(cursorPos, selectionPos));
        pacpEnd.set(JAVA_LONG, 0, Math.max(cursorPos, selectionPos));

        TS_TEXTCHANGE.acpStart(pChange, start);
        TS_TEXTCHANGE.acpOldEnd(pChange, oldEnd);
        TS_TEXTCHANGE.acpNewEnd(pChange, Math.max(cursorPos, selectionPos));
        return 0;
    }

    @Override
    public int FindNextAttrTransition(int acpStart, int acpHalt, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags, MemorySegment pacpNext, MemorySegment pfFound, MemorySegment plFoundOffset) {
        pacpNext.set(JAVA_LONG, 0, screen.messages[line].length());
        pfFound.set(JAVA_BOOLEAN, 0, false);
        return 0;
    }

    @Override
    public int GetEndACP(MemorySegment pacp) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        pacp.set(JAVA_LONG, 0, screen.messages[line].length());
        return 0;
    }

    @Override
    public int GetACPFromPoint(int vcView, MemorySegment ptScreen, int dwFlags, MemorySegment pacp) {
        return 0;
    }

    @Override
    public int GetTextExt(int vcView, int acpStart, int acpEnd, MemorySegment prc, MemorySegment pfClipped) {
        return 0;
    }
}
