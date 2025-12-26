package tech.tnze.msctf;

public class Source implements AutoCloseable {
    private final long pointer;

    Source(long p) {
        pointer = p;
    }

    private static native void releaseInstance(long p);

    private native int adviceUIElementSink(UIElementSink uiElementSink);
    private native int adviceContextOwner(ContextOwner contextOwner);

    public native void unadviseSink(int cookie);

    public int adviseSink(Object sink) {
        return switch (sink) {
            case UIElementSink uiElementSink -> adviceUIElementSink(uiElementSink);
            case ContextOwner contextOwner -> adviceContextOwner(contextOwner);
            default -> throw new RuntimeException("Unsupported sink implementation: " + sink);
        };
    }

    @Override
    public void close() {
        releaseInstance(pointer);
    }

    @Override
    public String toString() {
        return "ITfSource@" + Long.toHexString(pointer);
    }
}
