package tech.tnze.msctf;

public class Range implements AutoCloseable {
    private final long pointer;

    Range(long p) {
        pointer = p;
    }

    public native String getText(int editCookie);

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfRange@" + Long.toHexString(pointer);
    }
}
