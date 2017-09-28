package ch.psi.pshell.device;

import java.io.IOException;

/**
 * Interface for devices having a speed property.
 */
public interface Speedable extends Register<Double> {

    Double getSpeed() throws IOException, InterruptedException;
}
