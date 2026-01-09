package tech.tnze.msctf;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class Guid {

    public static String toString(MemorySegment p) {
        var data4 = tech.tnze.msctf.system.Guid.Data4(p);
        return String.format("%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                tech.tnze.msctf.system.Guid.Data1(p),
                tech.tnze.msctf.system.Guid.Data2(p),
                tech.tnze.msctf.system.Guid.Data3(p),
                data4.get(ValueLayout.JAVA_BYTE, 0),
                data4.get(ValueLayout.JAVA_BYTE, 1),
                data4.get(ValueLayout.JAVA_BYTE, 2),
                data4.get(ValueLayout.JAVA_BYTE, 3),
                data4.get(ValueLayout.JAVA_BYTE, 4),
                data4.get(ValueLayout.JAVA_BYTE, 5),
                data4.get(ValueLayout.JAVA_BYTE, 6),
                data4.get(ValueLayout.JAVA_BYTE, 7)
        );
    }
}
