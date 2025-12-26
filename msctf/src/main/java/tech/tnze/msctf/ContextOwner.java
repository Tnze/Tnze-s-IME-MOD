package tech.tnze.msctf;

public interface ContextOwner {
    class InvalidPointException extends Exception {
    }

    class NoLayoutException extends Exception {
    }

    long getACPFromPoint(long x, long y, int flags) throws InvalidPointException, NoLayoutException;

    Object getAttribute(String guidAttribute);

    record Rect(int x, int y, int w, int h) {
    }

    void getScreenExt(Rect out);

    record Status(int dynamicFlags, int staticFlags) {
    }

    void getStatus(Status status);

    boolean getTextExt(long acpStart, long acpEnd, Rect rect);

    int getWnd();
}
