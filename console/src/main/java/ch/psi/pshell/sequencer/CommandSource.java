package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;

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
            return !Context.hideServerCommands();
        }
        return true;
    }
    
    public boolean isSavable(){
        return isLocal() || (this == terminal) || ((this == server) && !Context.hideServerCommands());
    }    
}
