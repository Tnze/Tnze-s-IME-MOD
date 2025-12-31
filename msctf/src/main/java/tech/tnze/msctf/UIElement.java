package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfUIElement;

public class UIElement implements AutoCloseable {
    private final ITfUIElement inner;

    UIElement(ITfUIElement inner) {
        this.inner = inner;
    }

    public native String getDescription();

    public native String getGUID();

    public native CandidateListUIElement intoCandidateListUIElement();

    public native ToolTipUIElement intoToolTipUIElement();

    public native TransitoryExtensionUIElement intoTransitoryExtensionUIElement();


    @Override
    public void close() throws Exception {
        inner.Release();
    }
}
