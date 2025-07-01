package ch.psi.pshell.utils;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Some Windows utilities. If much native access is needed, 'net.java.dev.jna':jna-platform:4.4.0'
 * should be added to project instead, as most windows API is already mapped there.
 */
public class Windows {

    static final Map UNICODE_OPTIONS = new HashMap() {
        {
            put("type-mapper", W32APITypeMapper.UNICODE);
            put("function-mapper", W32APIFunctionMapper.UNICODE);
        }
    };

    static final Map ASCII_OPTIONS = new HashMap() {
        {
            put("type-mapper", W32APITypeMapper.ASCII);
            put("function-mapper", W32APIFunctionMapper.ASCII);
        }
    };

    public interface Kernel32 extends StdCallLibrary {

        public static final int STILL_ACTIVE = 259;

        public static final String LIBRARY_NAME = "kernel32";
        Kernel32 INSTANCE = (Kernel32) Native.loadLibrary(LIBRARY_NAME, Kernel32.class, UNICODE_OPTIONS);
        Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

        int GetProcessId(int process);

        int GetCurrentProcess();

        boolean Beep(int frequency, int duration);

        void Sleep(int duration);

        int CreateEventW(Pointer securityAttributes, boolean manualReset, boolean initialState, String name);

        int CreateThread(/*Pointer*/int lpThreadAttributes, IntByReference dwStackSize, Function lpStartAddress, Structure lpParameter, int dwCreationFlags, IntByReference lpThreadId);

        int GetExitCodeThread(int hThread, IntByReference lpExitCode);

        int GetLastError();

        boolean CloseHandle(int handle);
    }
}
