package ch.psi.pshell.detector;

import ch.psi.pshell.device.DeviceConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An entity class holding the detector configuration.
 */
@JsonIgnoreProperties(value = {"listeners"})
public class DetectorConfig extends DeviceConfig {

    public int n_frames = 100;
    public String dtype = "float";
    public int[] shape = new int[]{400, 200};
    public int frame_rate = 100;
}
