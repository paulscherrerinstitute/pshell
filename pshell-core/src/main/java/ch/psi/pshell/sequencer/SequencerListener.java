package ch.psi.pshell.sequencer;

import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.utils.State;

/**
 *
 */
public interface SequencerListener {
    
    default void onInitialized(int runCount) {}
    
    default void onStateChanged(State state, State former) {}
    
    default void onPreferenceChange(ViewPreference preference, Object value) {}
    
    default void onShellCommand(CommandSource source, String command) {}

    default void onShellResult(CommandSource source, Object ret) {}

    default void onShellStdout(String str) {}

    default void onShellStderr(String str) {}

    default void onShellStdin(String str) {}
    
    default void onNewStatement(Statement statement) {}

    default void onExecutingStatement(Statement statement) {}

    default void onExecutedStatement(Statement statement) {}

    default void onExecutingFile(String fileName) {}

    default void onExecutedFile(String fileName, Object result) {}
    
    default void willEval(CommandSource source, String code) throws SecurityException {}

    default void willRun(CommandSource source, String fileName, Object args) throws SecurityException  {}
    
}
