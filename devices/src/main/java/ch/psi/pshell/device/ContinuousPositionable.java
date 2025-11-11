package ch.psi.pshell.device;

import java.io.IOException;

/**
 *
 */
public interface ContinuousPositionable extends Positionable<Double>{
    
    default public boolean isInPosition(Double pos) throws IOException, InterruptedException{
        Double offset = getPosition() - pos;
        return Math.abs(offset) <= Math.abs(getDeadband());
    }
    
    /**
     * In-position deadband
     */
    double getDeadband();
}
