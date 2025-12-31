package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfContextOwnerCompositionSink;
import windows.win32.ui.textservices.ITfSource;
import windows.win32.ui.textservices.ITfUIElementSink;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static tech.tnze.msctf.WindowsException.checkResult;

public class Source implements AutoCloseable {
    private ITfSource inner;

    Source(ITfSource inner) {
        this.inner = inner;
    }


    public void unadviseSink(int cookie) {
        checkResult(inner.UnadviseSink(cookie));
    }

    public int adviseSink(Object sink) {
        try (var arena = Arena.ofConfined()) {
            var cookie = arena.allocate(JAVA_INT);
            checkResult(switch (sink) {
                case UIElementSink uiElementSink ->
                        inner.AdviseSink(ITfUIElementSink.iid(), uiElementSink.toComObject(), cookie);
                case ContextOwnerCompositionSink contextOwner ->
                        inner.AdviseSink(ITfContextOwnerCompositionSink.iid(), contextOwner.toComObject(), cookie);
                default -> throw new RuntimeException("Unsupported sink implementation: " + sink);
            });
            return cookie.get(JAVA_INT, 0);
        }
    }

    @Override
    public void close() {
        inner.Release();
        inner = null; // Avoid double free
    }
}
