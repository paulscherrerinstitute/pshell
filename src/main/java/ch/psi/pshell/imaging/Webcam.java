package ch.psi.pshell.imaging;

import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Support to webcams as image sources.
 */
public class Webcam extends SourceBase {

    com.github.sarxos.webcam.Webcam webcam;
    final String id;
    final int resolution;

    public Webcam(String name) {
        this(name, null);
    }

    /**
     * If id paramters is null takes the default webcam with the default resolution. 
     * Otherwise: [Camera Id]:[Resolution Index].
     * If any is missing, use the default.
     */
    public Webcam(String name, String id) {
        super(name, new SourceConfig());
        if ((id == null) || id.trim().isEmpty()) {
            this.id = null;
            this.resolution = -1;
        } else {
            String[] tokens = id.trim().split(":");
            this.id = tokens[0].trim().isEmpty() ? null : tokens[0].trim();
            int resolution = -1;
            if (tokens.length > 1) {
                try {
                    resolution = Integer.valueOf(tokens[1]);
                } catch (Exception ex) {
                }
            }
            this.resolution = resolution;
        }
    }

    public Webcam(String name, String cameraId, int resolutionIndex) {
        super(name, new SourceConfig());
        this.id = cameraId;
        this.resolution = resolutionIndex;        
    }    
    
    @Override
    public void doInitialize() throws IOException, InterruptedException {
        if (webcam != null) {
            webcam.close();
        }
        if (id == null) {
            webcam = com.github.sarxos.webcam.Webcam.getDefault();
        } else {
            for (com.github.sarxos.webcam.Webcam w : com.github.sarxos.webcam.Webcam.getWebcams()) {
                if (w.getName().equals(id.trim())) {
                    webcam = w;
                }

            }
        }
        if (webcam == null) {
            throw new IOException("Invalid camera");
        }
        try {
            if ((resolution >= 0) && (resolution < webcam.getViewSizes().length)) {
                if (webcam.isOpen()) {
                    webcam.close();
                }
                webcam.setViewSize(webcam.getViewSizes()[resolution]);
            }

            webcam.open();
        } catch (Exception ex) {
            throw new IOException("Error opening camera: " + ex.getMessage());
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (isInitialized() && webcam.isOpen()) {
            if (value) {
                Chrono chrono = new Chrono();
                try {
                    update();
                    chrono.waitCondition(new Condition() {
                        @Override
                        public boolean evaluate() throws InterruptedException {
                            return getFrameRate() > 0;
                        }
                    }, 5000);
                    setPolling((int) Math.max(1000.0 / getFrameRate(), 1.0));
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
            } else {
                setPolling(-1);
            }
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        try {
            BufferedImage img = webcam.getImage();
            pushImage(img);
        } catch (Exception ex) {
            pushError(ex);
        }
    }

    public com.github.sarxos.webcam.Webcam getObject() {
        return webcam;
    }

    public Dimension[] getResolutions() {
        return webcam.getViewSizes();
    }

    public Dimension getResolution() {
        return webcam.getViewSize();
    }

    public double getFrameRate() {
        return webcam.getFPS();
    }

    public void setResolution(Dimension dimension) {
        if (webcam.isOpen()) {
            webcam.close();
        }
        webcam.setViewSize(dimension);
        if (isInitialized()) {
            webcam.open();
        }
        if (isMonitored()) {
            doSetMonitored(true);
        }
    }

    public static List<String> getWebcams() {
        ArrayList<String> ret = new ArrayList();
        for (com.github.sarxos.webcam.Webcam webcam : com.github.sarxos.webcam.Webcam.getWebcams()) {
            ret.add(webcam.getName());
        }
        return ret;
    }

    protected void doClose() throws IOException {
        setMonitored(false);
        if (webcam != null) {
            webcam.close();
        }
        super.doClose();
    }
}
