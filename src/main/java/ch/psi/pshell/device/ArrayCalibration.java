package ch.psi.pshell.device;

/**
 * Provide calibration for x position of 1d arrays, based on scale and offset values.
 */
public class ArrayCalibration {

    public ArrayCalibration(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    public double[] getAxisX(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < size; i++) {
            ret[i] = getValue(i);
        }
        return ret;
    }

    public double getValue(int index) {
        return (((double) index) * scale) + offset;
    }

    public int getIndex(double value) {
        return (int) Math.floor(((value - offset) / scale) + 0.5);
    }

    public final double scale;
    public final double offset;
}
