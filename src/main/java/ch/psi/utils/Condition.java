package ch.psi.utils;

import java.util.concurrent.TimeoutException;

/**
 * Interface used by Chrono.waitCondition to check if condition is met.
 */
@FunctionalInterface
public interface Condition {

    boolean evaluate() throws InterruptedException;
    
    /**
     * A wait for the condition to be set in a for-sleep loop
     */
    
    public default void waitFor(int timeout) throws TimeoutException, InterruptedException {
        Chrono chrono = new Chrono();        
        while (!evaluate()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }       
            chrono.checkTimeout(timeout, "Timeout waiting ready");
            Thread.sleep(10); //Looping in 10ms because isReady can make HW access
        }
    }    
    
   
}
