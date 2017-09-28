package ch.psi.pshell.core;

/**
 * This enumeration represents the origin of a command (script execution, evaluation...) to the
 * current context. The execution behaviour and rights may vary according ti th origin.
 */
public enum CommandSource {

    ctr, console, terminal, server, task, script, plugin, ui;

    void putLogTag(StringBuilder sb) {
        if (this != ctr) {
            sb.append("(").append(toString()).append(")");
        }
    }

    public boolean isInternal() {
        return (this == ctr) || (this == plugin) || (this == script);
    }

    public boolean isLocal() {
        return (this == ui) || (this == console) || (this == task);
    }

    public boolean isRemote() {
        return (this == terminal) || (this == server);
    }
}
