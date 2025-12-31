package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfRange;

import static tech.tnze.msctf.WindowsException.checkResult;

public class Range implements AutoCloseable {
    private ITfRange inner;

    Range(ITfRange inner) {
        this.inner = inner;
    }

    public String getText(int editCookie) {
//        checkResult(inner.GetText(editCookie, 0, ))
        return ""; // TODO
    }

    @Override
    public void close() throws Exception {
        inner.Release();
        inner = null; // Avoid double free
    }
}
