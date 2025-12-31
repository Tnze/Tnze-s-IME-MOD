package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfContext;
import windows.win32.ui.textservices.ITfSource;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static tech.tnze.msctf.WindowsException.checkResult;

public class Context implements AutoCloseable {
    private final ITfContext inner;
    private final int editCookie;

    Context(ITfContext inner, int editCookie) {
        this.inner = inner;
        this.editCookie = editCookie;
    }

    public int getEditCookie() {
        return editCookie;
    }

    public Source getSource() {
        try (var arena = Arena.ofConfined()) {
            var sourceHolder = arena.allocate(ADDRESS.withTargetLayout(ITfSource.addressLayout()));
            checkResult(inner.QueryInterface(ITfSource.iid(), sourceHolder));
            return new Source(ITfSource.wrap(sourceHolder.get(ITfSource.addressLayout(), 0)));
        }
    }

    @Override
    public void close() throws Exception {
        inner.Release();
    }
}
