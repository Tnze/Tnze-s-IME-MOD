package tech.tnze.msctf;

public interface UIElementSink {
    boolean begin(int uiElementId);

    void update(int uiElementId);

    void end(int uiElementId);
}
