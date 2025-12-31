package tech.tnze.msctf;

public class Context implements AutoCloseable {
    private long pointer;
    private int editCookie;

    Context(long p, int ec) {
        pointer = p;
        editCookie = ec;
    }

    public int getEditCookie() {
        return editCookie;
    }

    public native Source getSource();

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfContext@" + Long.toHexString(pointer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Context) {
            return this.pointer == ((Context) obj).pointer;
        }
        return false;
    }
}
