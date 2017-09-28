package ch.psi.pshell.device;

/**
 * Provide calibration for x and y positions of 2d arrays, based on scale and offset values.
 */
public class MatrixCalibration {

    public MatrixCalibration(double scaleX, double scaleY, double offsetX, double offsetY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public double[] getAxisX(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < size; i++) {
            ret[i] = getValueX(i);
        }
        return ret;
    }

    public double getValueX(int index) {
        return (((double) index) * scaleX) + offsetX;
    }

    public int getIndexX(double valueX) {
        return (int) Math.floor(((valueX - offsetX) / scaleX) + 0.5);
    }

    public double[] getAxisY(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < size; i++) {
            ret[i] = getValueY(i);
        }
        return ret;
    }

    public double getValueY(int index) {
        return (((double) index) * scaleY) + offsetY;
    }

    public int getIndexY(double valueY) {
        return (int) Math.floor(((valueY - offsetY) / scaleY) + 0.5);
    }

    public final double scaleX;
    public final double scaleY;
    public final double offsetX;
    public final double offsetY;
}
