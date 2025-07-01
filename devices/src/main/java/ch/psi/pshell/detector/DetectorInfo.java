package ch.psi.pshell.detector;

import ch.psi.pshell.utils.Str;
import java.lang.reflect.Field;

/**
 * An entity holding the detector information, read through the REST interface.
 */
public class DetectorInfo {

    public Status status;
    public String frame_dtype;
    public String brand;
    public String model;
    public int frames_to_be_sent;
    public int frame_rate;
    public int[] frame_shape;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Field f : getClass().getFields()) {
            try {
                sb.append(f.getName() + "=" + Str.toString(f.get(this), 100));
                sb.append(", ");
            } catch (Exception ex) {
            }
        }
        sb.delete(sb.length() - 2, sb.length() - 1);
        return sb.toString();
    }
}
