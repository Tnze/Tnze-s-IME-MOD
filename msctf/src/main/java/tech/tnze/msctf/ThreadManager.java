package tech.tnze.msctf;

import java.util.function.Consumer;

public class ThreadManager implements AutoCloseable {
    private final long pointer;

    public static final int TF_TMAE_NOACTIVATETIP = 0x00000001;
    public static final int TF_TMAE_SECUREMODE = 0x00000002;
    public static final int TF_TMAE_UIELEMENTENABLEDONLY = 0x00000004;
    public static final int TF_TMAE_COMLESS = 0x00000008;
    public static final int TF_TMAE_WOW16 = 0x00000010;
    public static final int TF_TMAE_NOACTIVATEKEYBOARDLAYOUT = 0x00000020;
    public static final int TF_TMAE_CONSOLE = 0x00000040;

    public ThreadManager() {
        pointer = createInstance();
    }

    private static native long createInstance();

    private static native void releaseInstance(long p);

    public native int activate();

    public native int activateEx(int flags);

    public native DocumentManager associateFocus(long hwnd, DocumentManager documentManager);

    public native void deactivate();

    public native Source getSource();

    public native DocumentManager createDocumentManager();

    public native UIElementManager getUIElementManager();

    public native void enumDocumentManagers(Consumer<DocumentManager> consumer);

    @Override
    public void close() {
        releaseInstance(pointer);
    }
}
