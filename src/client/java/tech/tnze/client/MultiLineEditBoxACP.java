package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.util.Mth;
import tech.tnze.msctf.windows.win32.foundation.POINT;
import tech.tnze.msctf.windows.win32.foundation.RECT;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_FAIL;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.*;

public class MultiLineEditBoxACP extends AbstractEditBoxACP {
    private final MultiLineEditBox editBox;

    public MultiLineEditBoxACP(Window window, MultiLineEditBox editBox) {
        super(window);
        this.editBox = editBox;
    }

    @Override
    protected void adviseACPSink(ITextStoreACPSink sink) {

    }

    @Override
    protected void unadviseACPSink(ITextStoreACPSink sink) {

    }

    @Override
    public int GetStatus(MemorySegment pdcs) {
        TS_STATUS.dwDynamicFlags(pdcs, 0);
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

        var selected = editBox.textField.getSelected();

        TS_SELECTION_ACP.acpStart(pSelection, selected.beginIndex);
        TS_SELECTION_ACP.acpEnd(pSelection, selected.endIndex);
        TS_SELECTION_ACP$style$ase$VH.set(pSelection, 0, editBox.textField.cursor > editBox.textField.selectCursor ? TsActiveSelEnd.TS_AE_END : TsActiveSelEnd.TS_AE_START);
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

        editBox.tnze$setSinkEnabled(false);
        if (ase == TsActiveSelEnd.TS_AE_START) {
            editBox.textField.cursor = start;
            editBox.textField.selectCursor = end;
        } else { // End or None
            editBox.textField.cursor = end;
            editBox.textField.selectCursor = start;
        }
        editBox.tnze$setSinkEnabled(true);

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

        var value = editBox.getValue();
        if (acpStart < 0 || acpStart > value.length() || acpEnd < acpStart || acpEnd > value.length()) {
            return TS_E_INVALIDPOS;
        }

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);
        var replacement = String.valueOf(chars);

        int newEnd = acpStart + cch;
        var selected = editBox.textField.getSelected();
        editBox.setValue(value.substring(0, acpStart) + replacement + value.substring(acpEnd));
        // Fix selections. TODO: Related to gravity
        {
            int delta = cch - (acpEnd - acpStart);
            int start = selected.beginIndex;
            int end = selected.endIndex;
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
            editBox.textField.selectCursor = start;
            editBox.textField.cursor = end;
        }

        editBox.tnze$setSinkEnabled(false);
        editBox.textField.onValueChange();
        editBox.tnze$setSinkEnabled(true);

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

        var chars = pchText.reinterpret(JAVA_CHAR.byteSize() * cch).toArray(JAVA_CHAR);

        var selected = editBox.textField.getSelected();
        int start = selected.beginIndex;
        int oldEnd = selected.endIndex;
        editBox.tnze$setSinkEnabled(false);
        editBox.textField.insertText(String.valueOf(chars));
        editBox.tnze$setSinkEnabled(true);

        selected = editBox.textField.getSelected();
        pacpStart.set(JAVA_LONG, 0, selected.beginIndex);
        pacpEnd.set(JAVA_LONG, 0, selected.endIndex);

        TS_TEXTCHANGE.acpStart(pChange, start);
        TS_TEXTCHANGE.acpOldEnd(pChange, oldEnd);
        TS_TEXTCHANGE.acpNewEnd(pChange, selected.endIndex);

        return 0;
    }

    @Override
    public int FindNextAttrTransition(int acpStart, int acpHalt, int cFilterAttrs, MemorySegment paFilterAttrs, int dwFlags, MemorySegment pacpNext, MemorySegment pfFound, MemorySegment plFoundOffset) {
        pacpNext.set(JAVA_LONG, 0, editBox.getValue().length());
        pfFound.set(JAVA_BOOLEAN, 0, false);
        return 0;
    }

    @Override
    public int GetEndACP(MemorySegment pacp) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        pacp.set(JAVA_LONG, 0, editBox.getValue().length());
        return 0;
    }

    @Override
    public int GetACPFromPoint(int vcView, MemorySegment ptScreen, int dwFlags, MemorySegment pacp) {
        int windowX = (POINT.x(ptScreen) - window.getX()) / window.getGuiScale();
        int windowY = (POINT.y(ptScreen) - window.getY()) / window.getGuiScale();

        double widgetX = windowX - (editBox.getX() + editBox.innerPadding());
        double widgetY = windowY - (editBox.getY() + editBox.innerPadding() - editBox.scrollAmount());

        int line = Mth.floor(widgetY / 9.0);
        if (line < 0 || line >= editBox.textField.displayLines.size()) {
            return TS_E_INVALIDPOINT;
        }

        var lineView = editBox.textField.displayLines.get(line);
        var lineText = editBox.textField.value().substring(lineView.beginIndex, lineView.endIndex);

        var lineWidth = editBox.textField.font.width(lineText);
        if (widgetX < 0 || widgetX > lineWidth) {
            return TS_E_INVALIDPOINT;
        }
        int i = editBox.textField.font.plainSubstrByWidth(lineText, Mth.floor(widgetX)).length();

        pacp.set(JAVA_LONG, 0, i);
        return 0;
    }

    @Override
    public int GetTextExt(int vcView, int acpStart, int acpEnd, MemorySegment prc, MemorySegment pfClipped) {
        if (currentLockState.ordinal() < LockState.ReadOnly.ordinal()) {
            return TS_E_NOLOCK;
        }

        var value = editBox.getValue();
        var length = value.length();
        if (acpStart < 0 || acpEnd < acpStart || acpEnd > length) {
            return TS_E_INVALIDPOS;
        }

        int lineStart = editBox.textField.displayLines.size(), lineEnd = -1;
        double offsetX = editBox.getWidth();
        double offsetY = 0;
        int width = 0;
        int height = 0;

        for (int i = 0; i < editBox.textField.displayLines.size(); i++) {
            var lineView = editBox.textField.displayLines.get(i);
            var lineText = editBox.textField.value().substring(lineView.beginIndex, lineView.endIndex);
            var lineWidth = editBox.textField.font.width(lineText);

            if (lineView.beginIndex <= acpStart && lineView.endIndex >= acpStart) {
                lineStart = i;
                offsetX = editBox.textField.font.width(lineText.substring(0, acpStart - lineView.beginIndex));
                offsetY = 9 * i;
            }
            if (i > lineStart) {
                width = Math.max(width, lineWidth);
                offsetX = Math.min(offsetX, 0);
            }
            if (lineView.beginIndex <= acpEnd && lineView.endIndex >= acpEnd) {
                lineEnd = i;
                width = Math.max(width, editBox.textField.font.width(lineText.substring(0, acpEnd - lineView.beginIndex)));
                break;
            }

        }

        offsetX += editBox.getX() + editBox.innerPadding();
        offsetY += editBox.getY() + editBox.innerPadding() - editBox.scrollAmount();

        RECT.top(prc, (int) (window.getY() + offsetY * window.getGuiScale()));
        RECT.left(prc, (int) (window.getX() + offsetX * window.getGuiScale()));
        RECT.right(prc, (int) (window.getX() + (offsetX + width) * window.getGuiScale()));
        RECT.bottom(prc, (int) (window.getY() + (offsetY + height) * window.getGuiScale()));

        pfClipped.set(JAVA_BOOLEAN, 0, false);

        return 0;
    }

}
