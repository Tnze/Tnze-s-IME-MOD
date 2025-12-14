package tech.tnze.msctf;

public interface ContextOwnerCompositionSink {
    boolean onStartComposition(CompositionView composition);
    void onUpdateComposition(CompositionView composition, Range range);
    void onEndComposition(CompositionView composition);
}
