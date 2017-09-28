package ch.psi.pshell.imaging;

import ch.psi.utils.Config;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SourceConfig extends Config {

    public boolean grayscale = false;
    public boolean flipHorizontally = false;
    public boolean flipVertically = false;
    public boolean invert = false;
    public double rotation = 0.0;
    public boolean rotationCrop = false;
    public boolean transpose = false;
    public double scale = 1.0;
    public double rescaleFactor = 1.0;
    public double rescaleOffset = 0.0;
    public int roiX = 0;
    public int roiY = 0;
    public int roiWidth = -1;
    public int roiHeight = -1;
    public double spatialCalScaleX = Double.NaN;
    public double spatialCalScaleY = Double.NaN;
    public double spatialCalOffsetX = Double.NaN;
    public double spatialCalOffsetY = Double.NaN;
    public String spatialCalUnits = "mm";

    public boolean isCalibrated() {
        return !(Double.isNaN(spatialCalScaleX) || Double.isNaN(spatialCalScaleY) || Double.isNaN(spatialCalOffsetX) || Double.isNaN(spatialCalOffsetY));
    }

    public Calibration getCalibration() {
        if (!isCalibrated()) {
            return null;
        }
        return new Calibration(spatialCalScaleX, spatialCalScaleY, spatialCalOffsetX, spatialCalOffsetY);
    }

    public void setCalibration(Calibration calibration) {
        if (calibration == null) {
            calibration = new Calibration(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        if (!calibration.equals(getCalibration())) {
            spatialCalScaleX = calibration.getScaleX();
            spatialCalScaleY = calibration.getScaleY();
            spatialCalOffsetX = calibration.getOffsetX();
            spatialCalOffsetY = calibration.getOffsetY();
            try {
                save();
            } catch (IOException ex) {
                Logger.getLogger(SourceConfig.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
}
