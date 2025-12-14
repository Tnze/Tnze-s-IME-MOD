package tech.tnze.msctf;

public class ToolTipUIElement implements AutoCloseable {
    private final long pointer;

    ToolTipUIElement(long p) {
        pointer = p;
    }

    public native String getString();

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfToolTipUIElement@" + Long.toHexString(pointer);
    }
}
