package ch.psi.pshell.device;

import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Nameable;
import java.io.IOException;

/**
 * Interface for devices that have a position value and are capable of knowing if are in position.
 */
public interface Positionable<T> extends Nameable {        
    
    public T getPosition() throws IOException, InterruptedException;

    public boolean isInPosition(T pos) throws IOException, InterruptedException;

    default public void waitInPosition(T pos, int timeout) throws IOException, InterruptedException{
        Chrono chrono = new Chrono();
        while (!isInPosition(pos)) {
            if ((timeout >= 0) && (chrono.isTimeout(timeout))) {
                throw new IOException("Timeout waiting value: " + pos);
            }
            Thread.sleep(10);
        }
    }

    default public void assertInPosition(T pos) throws IOException, InterruptedException{
        if (!isInPosition(pos)) {
            throw new IOException("Not in position: " + pos);
        }
    }
}
