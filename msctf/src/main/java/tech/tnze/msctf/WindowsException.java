package tech.tnze.msctf;

public class WindowsException extends Exception {
    private int hr; // HRESULT

    public WindowsException(String message, int hr) {
        super(message);
        this.hr = hr;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ": " + getOsMessage();
    }

    public native String getOsMessage();
}
