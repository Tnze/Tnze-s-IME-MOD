package tech.tnze.msctf;

public class TransitoryExtensionUIElement implements AutoCloseable {
    private final long pointer;

    TransitoryExtensionUIElement(long p) {
        pointer = p;
    }

    public native String getString();

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfTransitoryExtensionUIElement@" + Long.toHexString(pointer);
    }
}
