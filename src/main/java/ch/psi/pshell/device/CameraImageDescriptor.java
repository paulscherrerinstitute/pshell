package ch.psi.pshell.device;

import ch.psi.pshell.device.Camera.ColorMode;
import ch.psi.pshell.device.Camera.DataType;

/**
 * Entity class holding the attributes of the data provided by a Camera device.
 */
public class CameraImageDescriptor {

    @Override
    public String toString() {
        return width + " x " + height;
    }
    public int width;
    public int height;
    public ColorMode colorMode;
    public DataType dataType;

    public MatrixCalibration calibration;

    public int getPixelSize() {
        return colorMode.getDepth() * dataType.getSize();
    }

    public int getDepth() {
        return colorMode.getDepth();
    }

    public int getPixels() {
        return width * height;
    }

    public int getTotalSize() {
        return getPixelSize() * getPixels();
    }
}
