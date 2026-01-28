package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import tech.tnze.msctf.ComObject;
import tech.tnze.msctf.Guid;
import tech.tnze.msctf.windows.win32.foundation.RECT;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static com.sun.jna.platform.win32.WinError.E_NOTIMPL;
import static java.lang.foreign.ValueLayout.*;
import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.msctf.ComObject.equalIIDs;
import static tech.tnze.msctf.windows.win32.foundation.Constants.*;
import static tech.tnze.msctf.windows.win32.system.ole.Constants.CONNECT_E_ADVISELIMIT;
import static tech.tnze.msctf.windows.win32.system.ole.Constants.CONNECT_E_NOCONNECTION;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.*;
import static tech.tnze.msctf.windows.win32.ui.textservices.TEXT_STORE_LOCK_FLAGS.TS_LF_READ;
import static tech.tnze.msctf.windows.win32.ui.textservices.TEXT_STORE_LOCK_FLAGS.TS_LF_READWRITE;

public abstract class AbstractEditBoxACP implements ITextStoreACP2, AutoCloseable {
    private Arena arena = Arena.ofShared();
    protected final Window window;
    private int refCount = 1;

    protected MemorySegment sinkPtr, textStorePtr;
    protected ITextStoreACPSink sink;

    protected LockState currentLockState = LockState.None;
    protected LockState pendingAsyncLock = LockState.None;

    protected enum LockState {
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

    public AbstractEditBoxACP(Window window) {
        this.window = window;
        textStorePtr = ITextStoreACP2.create(this, arena);
    }

    @Override
    public int QueryInterface(MemorySegment riid, MemorySegment ppvObject) {
        if (ComObject.equalIIDs(riid, IUnknown.iid()) || ComObject.equalIIDs(riid, ITextStoreACP2.iid())) {
            ppvObject.set(ADDRESS, 0, textStorePtr);
            this.AddRef();
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
            textStorePtr = MemorySegment.NULL;
        }
        return refCount;
    }

    @Override
    public void close() {
        Release();
    }

    public MemorySegment getPointer() {
        return textStorePtr;
    }


    protected abstract void adviseACPSink(ITextStoreACPSink sink);
    protected abstract void unadviseACPSink(ITextStoreACPSink sink);

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
            adviseACPSink(sink);
        }
        return 0;
    }

    @Override
    public int UnadviseSink(MemorySegment punk) {
        if (!punk.equals(sinkPtr)) {
            LOGGER.error("Failed to unadvise sink: {}", punk);
            return CONNECT_E_NOCONNECTION;
        }
        unadviseACPSink(sink);
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

    protected static final VarHandle TS_SELECTION_ACP$style$ase$VH = TS_SELECTION_ACP.layout().varHandle(MemoryLayout.PathElement.groupElement("style"), MemoryLayout.PathElement.groupElement("ase"));
    protected static final VarHandle TS_SELECTION_ACP$style$fInterimChar$VH = TS_SELECTION_ACP.layout().varHandle(MemoryLayout.PathElement.groupElement("style"), MemoryLayout.PathElement.groupElement("fInterimChar"));

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
    public int RetrieveRequestedAttrs(int ulCount, MemorySegment paAttrVals, MemorySegment pcFetched) {
        pcFetched.set(JAVA_INT, 0, 0);
        return 0;
    }

    @Override
    public int GetActiveView(MemorySegment pvcView) {
        pvcView.set(JAVA_INT, 0, 0);
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
