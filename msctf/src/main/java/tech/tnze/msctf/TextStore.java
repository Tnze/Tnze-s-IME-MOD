package tech.tnze.msctf;

public interface TextStore {
    void adviseSink();
    void findNextAttrTransition();
    void getACPFromPoint();
    void getActiveView();
    void getEmbedded();
    void getEndACP();
    void getFormattedText();
    void getScreenExt();
    void getSelection();
    void getStatus();
    void getText();
    void getWnd();
    void insertEmbedded();
    void insertEmbeddedAtSelection();
    void queryInsert();
    void queryInsertEmbedded();
    void requestAttrsAtPosition();
    void requestAttrsTransitioningAtPosition();
    void requestLock();
    void requestSupportedAttrs();
    void setSelection();
    void unadviseSink();
}
