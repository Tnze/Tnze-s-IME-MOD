package tech.tnze.msctf;

import windows.win32.system.com.Apis;
import windows.win32.system.com.COINIT;

import java.lang.foreign.MemorySegment;

public class ComBase {
    public static void CoInitialize() {
        WindowsException.checkResult(Apis.CoInitializeEx(MemorySegment.NULL, COINIT.APARTMENTTHREADED | COINIT.DISABLE_OLE1DDE));
    }

    public static void CoUninitialize() {
        Apis.CoUninitialize();
    }
}
