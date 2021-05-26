package ch.psi.pshell.imaging;

import ch.psi.pshell.device.DummyRegister;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;

/**
 *
 */
public abstract class ImageMeasurement extends DummyRegister implements ImageListener {
    
    final Source image;

    public ImageMeasurement(Source image, String name) {
        super(image.getName() + "_" + name);
        this.image = image;
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        Data data = image.getData();
        try {
            return data == null ? Double.NaN : calc(data);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
            return Double.NaN;
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (value) {
            image.addListener(this);
        } else {
            image.removeListener(this);
        }
    }

    @Override
    public void onImage(Object origin, BufferedImage image, Data data) {
        try {
            this.read();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    @Override
    public void onError(Object origin, Exception ex) {
    }

    protected abstract Double calc(Data data);
    
}
