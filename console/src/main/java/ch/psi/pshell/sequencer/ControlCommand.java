package ch.psi.pshell.sequencer;

/**
 * Enumeration representing the control commands, the console commands that evaluated directly - not
 * passed to the interpreter.
 */
public enum ControlCommand {

    history, evalb, inject, reload, login, restart, run, pause, resume, abort, shutdown, tasks, devices, users;

    public static final Character CONTROL_COMMAND_PREFIX = ':';
    public static final Character BACKGROUND_COMMAND_PREFIX = '&';

    static ControlCommand fromString(String command) {
        try {
            if (command.endsWith(BACKGROUND_COMMAND_PREFIX.toString())) {
                return ControlCommand.evalb;
            }
            if (command.startsWith(CONTROL_COMMAND_PREFIX.toString())) {
                return ControlCommand.valueOf(command.split(" ")[0].substring(1));
            }
        } catch (Exception ex) {
        }
        return null;
    }

    static String adjust(String command) {
        if (command.endsWith(ControlCommand.BACKGROUND_COMMAND_PREFIX.toString())) {
            command = ControlCommand.CONTROL_COMMAND_PREFIX.toString() + ControlCommand.evalb.toString() + " " + command.substring(0, command.length() - 1);
        }
        return command;
    }

    public static boolean match(String str) {
        return str.startsWith(CONTROL_COMMAND_PREFIX.toString()) || str.endsWith(BACKGROUND_COMMAND_PREFIX.toString());
    }

    boolean isEval() {
        return (this == evalb) || (this == restart) || (this == inject) || (this == reload)  || (this == shutdown);
    }

    boolean isScripControl() {
        return (this == abort) ||  (this == pause) || (this == resume) ||(this == run);
    }    
}
