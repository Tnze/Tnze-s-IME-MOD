package tech.tnze.msctf;

public class UIElement implements AutoCloseable {
    private final long pointer;

    UIElement(long p) {
        pointer = p;
    }

    public native String getDescription();
    public native String getGUID();

    public native CandidateListUIElement intoCandidateListUIElement();

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfUIElement@" + Long.toHexString(pointer);
    }
}
