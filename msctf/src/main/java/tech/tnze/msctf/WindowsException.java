package tech.tnze.msctf;

import java.lang.foreign.*;

import static tech.tnze.msctf.windows.win32.foundation.Apis.LocalFree;
import static tech.tnze.msctf.windows.win32.system.diagnostics.debug.FORMAT_MESSAGE_OPTIONS.*;
import static tech.tnze.msctf.windows.win32.system.diagnostics.debug.Apis.FormatMessageW;

public class WindowsException extends RuntimeException {
    private int hr; // HRESULT

    public WindowsException(int hr) {
        this.hr = hr;
    }

    @Override
    public String getMessage() {
        char[] message;
        var errorStateLayout = Linker.Option.captureStateLayout();
        try (var arena = Arena.ofConfined()) {
            var errorState = arena.allocate(errorStateLayout);
            var buffer = arena.allocate(AddressLayout.ADDRESS);
            int length = FormatMessageW(
                    errorState,
                    FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                    MemorySegment.NULL,
                    hr,
                    0x409, // MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US)
                    buffer,
                    0,
                    MemorySegment.NULL
            );
            var lpwstr = buffer.get(ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(length, ValueLayout.JAVA_CHAR)), 0);
            message = lpwstr.toArray(ValueLayout.JAVA_CHAR);
            LocalFree(errorState, lpwstr);
        }
        return "(" + hr + ") " + String.valueOf(message).trim();
    }

    public static void checkResult(int hr) throws WindowsException {
        if (hr < 0) {
            throw new WindowsException(hr);
        }
    }
}
