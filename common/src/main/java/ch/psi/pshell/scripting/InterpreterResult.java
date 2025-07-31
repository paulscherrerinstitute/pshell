package ch.psi.pshell.scripting;

import ch.psi.pshell.app.Stdio;
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
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(statement);
        if (!complete){
            sb.append(": incomplete");
        } else if (!correct){
            sb.append(": incorrect");
        } else if (exception!=null){
            sb.append(" -> exception: ").append(exception.getMessage());
        } else {
            sb.append(" -> ").append(result);
        }
        return sb.toString();
    }
    
    public String getExceptionPrintableMessage() {
        if (exception==null){
            return null;
        }
        return getPrintableMessage(exception);
    }
    
    public static String getPrintableMessage(Throwable ex) {
        return Stdio.getPrintableMessage(ex);
    }        
}
