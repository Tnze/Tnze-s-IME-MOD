package tech.tnze.msctf;

import system.Guid;
import windows.win32.ui.textservices.ITfCompositionView;
import windows.win32.ui.textservices.ITfRange;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static tech.tnze.msctf.WindowsException.checkResult;

public class CompositionView implements AutoCloseable {
    private final ITfCompositionView inner;

    CompositionView(ITfCompositionView inner) {
        this.inner = inner;
    }

    public String getOwnerClsid() {
        try (var arena = Arena.ofConfined()) {
            var clsid = arena.allocate(Guid.layout());
            checkResult(inner.GetOwnerClsid(clsid));
            return ""; // TODO
        }
    }

    public Range getRange() {
        try (var arena = Arena.ofConfined()) {
            var rangeHolder = arena.allocate(ADDRESS.withTargetLayout(ITfRange.addressLayout()));
            checkResult(inner.GetRange(rangeHolder));
            var range = ITfRange.wrap(rangeHolder.get(ITfRange.addressLayout(), 0));
            return new Range(range);
        }
    }

    @Override
    public void close() throws Exception {
        inner.Release();
    }
}
