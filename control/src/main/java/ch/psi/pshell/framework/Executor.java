
package ch.psi.pshell.framework;

import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Condition;
import java.util.concurrent.TimeoutException;

/**
 * Tags panels used for executing logic that are persisted in files.
 */
public interface Executor {
    String getFileName();
    
    default boolean isTabNameUpdated() {
        return true;
    }  
    
    default boolean canStep() {
        return true;
    }    
    
    default boolean canPause() {
        return true;
    }   
    
    default boolean canSave() {
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
        if (result instanceof Exception exception){
            throw exception;
        }
        return result;
    }        
    
    default Object getResult(){
        return null;
    }  
    
}
