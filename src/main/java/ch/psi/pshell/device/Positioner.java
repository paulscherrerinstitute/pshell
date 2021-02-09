package ch.psi.pshell.device;

import static ch.psi.pshell.device.Readable.TIMEOUT_INFINITE;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * ControlledVariable with move commands (sync and async absolute and relative), and using waitReady
 * to wait end of move . If a rotation device ('rotation' option in PositionerConfig): - In-position
 * check compares against modulo 360. - move() will write to the closest modulo of destination. -
 * write() is not affected.
 */
public interface Positioner extends Movable<Double> {

    void moveRel(Double offset, int timeout) throws IOException, InterruptedException;

    default void moveRel(Double offset) throws IOException, InterruptedException {
        moveRel(offset, TIMEOUT_INFINITE);
    }

    default CompletableFuture moveRelAsync(Double offset) {
        return moveRelAsync(offset, TIMEOUT_INFINITE);
    }

    default CompletableFuture moveRelAsync(Double offset, int timeout) {
        return Threading.getFuture(() -> moveRel(offset, timeout));
    }

}
