package ch.psi.pshell.device;

import ch.psi.pshell.utils.Nameable;
import java.io.IOException;

/**
 * Interface for devices that have a position value and are capable of knowing if are in position.
 */
public interface Positionable<T> extends Nameable {        
    
    public T getPosition() throws IOException, InterruptedException;

    public boolean isInPosition(T pos) throws IOException, InterruptedException;

    public void waitInPosition(T pos, int timeout) throws IOException, InterruptedException;

    public void assertInPosition(T pos) throws IOException, InterruptedException;

}
