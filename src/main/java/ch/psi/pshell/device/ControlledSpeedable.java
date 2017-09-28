package ch.psi.pshell.device;

import ch.psi.utils.Reflection.Hidden;
import java.io.IOException;

/**
 * Interface for devices having a read/write speed property.
 */
public interface ControlledSpeedable extends Speedable {

    void setSpeed(Double speed) throws IOException, InterruptedException;

    Double getDefaultSpeed();

    Double getMinSpeed();

    Double getMaxSpeed();

    default public boolean isValidSpeed(double speed) {
        double max_speed = getMaxSpeed();
        double min_speed = getMinSpeed();
        return (!Double.isNaN(speed) && (speed > 0)
                && ((Double.isNaN(max_speed)) || (speed <= max_speed))
                && ((Double.isNaN(min_speed)) || (speed >= min_speed)));
    }

    @Hidden
    default public void assertValidSpeed(double speed) throws IllegalArgumentException {
        if (!isValidSpeed(speed)) {
            throw new IllegalArgumentException(getName() + " invalid speed: " + speed);
        }
    }
}
