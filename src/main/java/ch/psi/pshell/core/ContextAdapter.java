package ch.psi.pshell.core;

import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Configurable;
import ch.psi.utils.State;

/**
 * A convenience abstract adapter class for receiving context events, with empty methods. This
 * adapter class only exists because of Jython bug extending default methods. Python scripts cannot
 * extend the Listener directly.
 */
public abstract class ContextAdapter implements ContextListener {

    @Override
    public void onContextStateChanged(State state, State former) {
    }

    @Override
    public void onContextInitialized(int runCount) {
    }

    @Override
    public void onShellCommand(CommandSource source, String command) {
    }

    @Override
    public void onShellResult(CommandSource source, Object result) {
    }

    @Override
    public void onShellStdout(String str) {
    }

    @Override
    public void onShellStderr(String str) {
    }

    @Override
    public void onShellStdin(String str) {
    }

    @Override
    public void onNewStatement(Statement statement) {
    }

    @Override
    public void onExecutingStatement(Statement statement) {
    }

    @Override
    public void onExecutedStatement(Statement statement) {
    }

    @Override
    public void onExecutingFile(String fileName) {

    }

    @Override
    public void onExecutedFile(String fileName, Object result) {

    }

    @Override
    public void onConfigurationChange(Configurable obj) {

    }

    @Override
    public void onBranchChange(String branch) {

    }

    @Override
    public void onPreferenceChange(ViewPreference preference, Object value) {

    }
    
    @Override
    public void onPathChange(String pathId){
        
    }
}
