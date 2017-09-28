package ch.psi.pshell.imaging;

import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.ReadonlyRegister;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * PShell supports imaging processing and rendering. Images are pushed with a special type of
 * device, implementing__Source__. take() returns image count.
 */
public interface Source extends GenericDevice<ImageListener>, ImageBuffer {

    void refresh();

    Data getData();

    Object getArray();

    Filter getFilter();

    void setFilter(Filter filter);

    BufferedImage getOutput();

    ReadableMatrix getDataMatrix();

    Calibration getCalibration();

    void setCalibration(Calibration calibration);

    void setCalibration(double scaleX, double scaleY, double offsetX, double offsetY);

    void setBackgroundData(Data data);

    void setBackgroundImage(BufferedImage image);

    void captureBackground(int images, int delay) throws IOException, InterruptedException, TimeoutException;

    BufferedImage getBackgroundImage();

    Data getBackgroundData();

    void setBackgroundEnabled(boolean value);

    boolean isBackgroundEnabled();

    void saveBackground(String name);

    void loadBackground(String name);

    ReadonlyRegister<Double> getContrast();

    public interface EmbeddedCameraSource extends Source {

        Camera getCamera();
    }

}
