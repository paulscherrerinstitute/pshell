package ch.psi.pshell.imaging;

/**
 *
 */
public interface Calibrated {
    Calibration getCalibration();

    void setCalibration(Calibration calibration);

    default void setCalibration(double scaleX, double scaleY, double offsetX, double offsetY){
        setCalibration(new Calibration(scaleX, scaleY, offsetX, offsetY));
    }
    
}
