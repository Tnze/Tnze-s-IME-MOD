package tech.tnze.msctf;

import windows.win32.system.com.IUnknown;
import windows.win32.ui.textservices.ITfUIElementSink;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;

public abstract class UIElementSink {
    private final Arena arena = Arena.ofAuto();

    public abstract boolean beginUIElement(int uiElementID);

    public abstract void updateUIElement(int uiElementID);

    public abstract void endUIElement(int uiElementID);

    MemorySegment toComObject() {
        var impl = new IMPL();
        var p = ITfUIElementSink.create(impl, arena);
        impl.setThisPointer(p);
        return p;
    }

    private class IMPL extends ComObject implements ITfUIElementSink {

        private static final MemorySegment[] implementedIIDs = {IUnknown.iid(), ITfUIElementSink.iid()};

        /**
         * Creates a new instance.
         */
        public IMPL() {
            super(implementedIIDs);
        }

        @Override
        public int BeginUIElement(int dwUIElementId, MemorySegment pbShow) {
            pbShow.set(JAVA_BOOLEAN, 0, beginUIElement(dwUIElementId));
            return 0;
        }

        @Override
        public int UpdateUIElement(int dwUIElementId) {
            updateUIElement(dwUIElementId);
            return 0;
        }

        @Override
        public int EndUIElement(int dwUIElementId) {
            endUIElement(dwUIElementId);
            return 0;
        }
    }
}
