package tech.tnze.msctf;

public class CandidateListUIElement implements AutoCloseable {
    private final long pointer;

    CandidateListUIElement(long p) {
        pointer = p;
    }

    public native int getCount();

    public native int getCurrentPage();

    public native int getPageIndex(int[] indexes);

    public native int getSelection();

    public native String getString(int index);

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfUIElement@" + Long.toHexString(pointer);
    }
}
