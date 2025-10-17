package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import java.util.logging.Level;

/**
 * This enumeration represents the origin of a command (script execution, evaluation...) to the
 * current context. The execution behavior and rights may vary according to the origin.
 */
public enum CommandSource {

    ctr, console, terminal, server, task, script, plugin, ui;

    void putLogTag(StringBuilder sb) {
        if (this != ctr) {
            sb.append("(").append(toString()).append(")");
        }
    }

    public boolean isInternal() {
        return (this == ctr) || (this == plugin) || (this == script) || (this == task);
    }

    public boolean isLocal() {
        return (this == ui) || (this == console);
    }

    public boolean isRemote() {
        return (this == terminal) || (this == server);
    }
    
    public boolean isDisplayable(){
        if (this == ctr) {
            return false;
        }
        if (this == server){
            return !Context.isServerCommandsHidden();
        }
        return true;
    }
    
    public boolean isSavable(){
        return isLocal() || (this == terminal) || ((this == server) && !Context.isServerCommandsHidden());
    }    

    static boolean isBackgroundEval(String command){
        if (command.startsWith(Sequencer.Command.run.toString())){
            //Always log script execution
            return false;
        }
        return command.endsWith(ControlCommand.BACKGROUND_COMMAND_PREFIX.toString());
    }
    
    public Level getLogLevel(String command){
        if ((this == server) && Context.isServerCommandsHidden()){
            if (isBackgroundEval(command)){
                return Level.FINER;
            }
        }
        return Level.INFO;
    }    

}
