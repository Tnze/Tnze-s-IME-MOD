package tech.tnze.msctf;

public class Context implements AutoCloseable {
    private long pointer;
    private int editCookie;

    Context(long p, int ec) {
        pointer = p;
        editCookie = ec;
    }

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfContext@" + Long.toHexString(pointer);
    }
}
