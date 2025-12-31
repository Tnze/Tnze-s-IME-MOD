package tech.tnze.msctf;

import windows.win32.system.com.IUnknown;
import windows.win32.ui.textservices.ITfCompositionView;
import windows.win32.ui.textservices.ITfContextOwnerCompositionSink;
import windows.win32.ui.textservices.ITfRange;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public abstract class ContextOwnerCompositionSink {
    private final Arena arena = Arena.ofAuto();

    MemorySegment toComObject() {
        var impl = new IMPL();
        var p = ITfContextOwnerCompositionSink.create(impl, arena);
        impl.setThisPointer(p);
        return p;
    }

    public abstract boolean onStartComposition(CompositionView composition);

    public abstract void onUpdateComposition(CompositionView composition, Range rangeNew);

    public abstract void onEndComposition(CompositionView composition);

    private class IMPL extends ComObject implements ITfContextOwnerCompositionSink {
        private static final MemorySegment[] implementedIIDs = {
                IUnknown.iid(),
                ITfContextOwnerCompositionSink.iid()
        };

        /**
         * Creates a new instance.
         */
        public IMPL() {
            super(implementedIIDs);
        }

        @Override
        public int OnStartComposition(MemorySegment pComposition, MemorySegment pfOk) {
            var composition = ITfCompositionView.wrap(pComposition);
            composition.AddRef();
            boolean ok = onStartComposition(new CompositionView(composition));
            pfOk.set(ValueLayout.JAVA_BOOLEAN, 0, ok);
            return 0;
        }

        @Override
        public int OnUpdateComposition(MemorySegment pComposition, MemorySegment pRangeNew) {
            var composition = ITfCompositionView.wrap(pComposition);
            composition.AddRef();
            var range = ITfRange.wrap(pRangeNew);
            range.AddRef();
            onUpdateComposition(new CompositionView(composition), new Range(range));
            return 0;
        }

        @Override
        public int OnEndComposition(MemorySegment pComposition) {
            var composition = ITfCompositionView.wrap(pComposition);
            composition.AddRef();
            onEndComposition(new CompositionView(composition));
            return 0;
        }
    }
}
