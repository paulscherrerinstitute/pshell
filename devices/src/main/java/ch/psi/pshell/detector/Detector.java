package ch.psi.pshell.detector;

import ch.psi.pshell.device.Device;
import java.io.IOException;

/**
 * Interface for PSI streamed detectors.
 */
public interface Detector extends Device {

    public DetectorInfo readInfo() throws IOException;

    public void start() throws IOException;

    public void stop() throws IOException;
}
