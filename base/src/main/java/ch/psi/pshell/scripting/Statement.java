package ch.psi.pshell.scripting;

import javax.script.CompiledScript;

/**
 *
 */
public class Statement {

    public String text;
    public String fileName;
    public int number;
    public int lineNumber;
    public int finalLineNumber;
    CompiledScript compiledScript;
}
