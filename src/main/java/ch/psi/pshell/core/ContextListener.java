package ch.psi.pshell.core;

import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Configurable;
import ch.psi.utils.State;

/**
 * The listener interface for receiving context events.
 */
public interface ContextListener {

    void onContextStateChanged(State state, State former);

    void onContextInitialized(int runCount);

    void onShellCommand(CommandSource source, String command);

    void onShellResult(CommandSource source, Object result);

    void onShellStdout(String str);

    void onShellStderr(String str);

    void onShellStdin(String str);

    void onNewStatement(Statement statement);

    void onExecutingStatement(Statement statement);

    void onExecutedStatement(Statement statement);

    void onExecutingFile(String fileName);

    void onExecutedFile(String fileName, Object result);

    void onConfigurationChange(Configurable obj);

    void onBranchChange(String branch);

    /**
     * Triggered by scripts to configure displaying options
     */
    void onPreferenceChange(ViewPreference preference, Object value);
}
