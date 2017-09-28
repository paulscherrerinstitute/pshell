package ch.psi.pshell.core;

/**
 * The listener interface for receiving script stdio events.
 */
public interface ScriptStdioListener {

    void onStdout(String str);

    void onStderr(String str);

    public String readStdin() throws InterruptedException;
}
