package tech.tnze.msctf;

public class CompositionView implements AutoCloseable {
    private final long pointer;

    CompositionView(long p) {
        pointer = p;
    }

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfCompositionView@" + Long.toHexString(pointer);
    }
}
