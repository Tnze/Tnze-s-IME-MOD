package tech.tnze.msctf;

public class UIElementManager implements AutoCloseable {
    private final long pointer;

    UIElementManager(long p) {
        pointer = p;
    }

    public native UIElement getUIElement(int uiElementId);

    @Override
    public native void close() throws Exception;

    @Override
    public String toString() {
        return "ITfUIElementMgr@" + Long.toHexString(pointer);
    }
}
