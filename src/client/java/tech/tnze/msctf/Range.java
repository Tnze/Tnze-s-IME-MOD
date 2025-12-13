package tech.tnze.msctf;

public class Range {
    private final long pointer;

    Range(long p) {
        pointer = p;
    }

    @Override
    public String toString() {
        return "ITfRange@" + Long.toHexString(pointer);
    }
}
