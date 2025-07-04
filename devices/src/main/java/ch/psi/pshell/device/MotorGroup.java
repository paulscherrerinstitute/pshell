package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for devices controlling a set of motors that move together.
 */
public interface MotorGroup extends Device, Readable<double[]>, Writable<double[]>, Movable<double[]> {

    void move(double[] destinations, MoveMode mode, double time) throws IOException, InterruptedException;

    void moveRel(double[] offset, MoveMode mode, double time) throws IOException, InterruptedException;

    default public void move(double[] destination, double time) throws IOException, InterruptedException {
        move(destination, MoveMode.timed, time);
    }

    @Override
    default void move(double[] destination, int timeout) throws IOException, InterruptedException {
        move(destination, MoveMode.defaultSpeed, timeout);
    }

    default void moveRel(double[] offset, int timeout) throws IOException, InterruptedException {
        moveRel(offset, MoveMode.defaultSpeed, timeout);
    }

    default void moveRel(double[] offset) throws IOException, InterruptedException {
        moveRel(offset, TIMEOUT_INFINITE);
    }

    default CompletableFuture moveAsync(double[] destinations, MoveMode mode, double time) {
        return Threading.getPrivateThreadFuture(() -> move(destinations, mode, time));
    }

    default CompletableFuture moveRelAsync(double[] offset, int timeout) {
        return Threading.getPrivateThreadFuture(() -> moveRel(offset, timeout));
    }

    default CompletableFuture moveRelAsync(double[] offset) {
        return moveRelAsync(offset, TIMEOUT_INFINITE);
    }

    //Overiding so Jython can contert list properly
    @Override
    default void move(double[] destination) throws IOException, InterruptedException {
        Movable.super.move(destination);
    }

    @Override
    default CompletableFuture moveAsync(double[] destination, int timeout) {
        return Movable.super.moveAsync(destination, timeout);
    }

    @Override
    default CompletableFuture moveAsync(double[] destination) {
        return Movable.super.moveAsync(destination);
    }

    boolean isInPosition(double[] position, double resolution) throws IOException, InterruptedException;

    Motor[] getMotors();
    
    void setRestoreSpeedAfterMove(boolean value);
    
    boolean getRestoreSpeedAfterMove();
    
    void restoreSpeed() throws IOException, InterruptedException;
    
    boolean isExecutingSimultaneousMove();
    
    boolean isStartingSimultaneousMove();

}
