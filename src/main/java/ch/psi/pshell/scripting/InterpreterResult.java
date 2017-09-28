package ch.psi.pshell.scripting;

import javax.script.ScriptException;

/**
 *
 */
public class InterpreterResult {

    public String statement;
    public boolean complete;
    public boolean correct;
    public ScriptException exception;
    public Object result;
}
