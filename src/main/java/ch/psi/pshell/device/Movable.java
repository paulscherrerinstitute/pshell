package ch.psi.pshell.device;

import static ch.psi.pshell.device.Readable.TIMEOUT_INFINITE;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for devices that have a position value and execute move commands.
 */
public interface Movable<T> extends Positionable<T>, Stoppable {

    void move(T destination, int timeout) throws IOException, InterruptedException;

    default void move(T destination) throws IOException, InterruptedException {
        move(destination, TIMEOUT_INFINITE);
    }

    default CompletableFuture moveAsync(T value, int timeout) {
        return Threading.getFuture(() -> 
            move(value, timeout)
        );
    }

    default CompletableFuture moveAsync(T value) {
        return moveAsync(value, TIMEOUT_INFINITE);
    }

    boolean isReady() throws IOException, InterruptedException;

    void waitReady(int timeout) throws IOException, InterruptedException;
}
