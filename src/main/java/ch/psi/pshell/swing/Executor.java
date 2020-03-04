
package ch.psi.pshell.swing;

import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import java.util.concurrent.TimeoutException;

/**
 * Tags panels used for executing logic that are persisted in files.
 */
public interface Executor {
    String getFileName();
    
    default boolean canStep() {
        return true;
    }    
    
    default boolean canPause() {
        return true;
    }   
    
    
    boolean isExecuting();
    boolean hasChanged();    
    
    default Object waitComplete(int timeout) throws Exception {
        Chrono chrono = new Chrono();
        try {
            chrono.waitCondition(new Condition() {
                @Override
                public boolean evaluate() throws InterruptedException {
                    return !isExecuting();
                }
            }, timeout);
        } catch (TimeoutException ex) {
        }        
        Object result = getResult();
        if (result instanceof Exception){
            throw (Exception)result;
        }
        return result;
    }        
    
    default Object getResult(){
        return null;
    }  
    
}
