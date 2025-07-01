package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;
import java.io.IOException;

/**
 * Interface for devices having a speed property.
 */
public interface Speedable extends Register<Double>, DoubleType {

    Double getSpeed() throws IOException, InterruptedException;
}
