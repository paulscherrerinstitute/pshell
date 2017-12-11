package ch.psi.pshell.imaging;

import ch.psi.pshell.device.DummyRegister;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.GenericDeviceBase;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.Serializer;
import ch.psi.utils.State;
import ch.psi.utils.Threading;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.imageio.ImageIO;

/**
 *
 */
public class SourceBase extends GenericDeviceBase<ImageListener> implements Source {

    volatile Chrono chrono;
    volatile int count;
    Filter filter;
    volatile Data currentData;
    volatile BufferedImage currentImage;

    volatile Data backgroundData;
    volatile BufferedImage backgroundImage;

    Integer timeout;

    protected SourceBase() {
        super();
    }

    protected SourceBase(String name) {
        super(name);
    }

    protected SourceBase(String name, SourceConfig config) {
        super(name, config);
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        chrono = null;
        count = 0;
    }

    protected BufferedImage applyTransformations(BufferedImage image, Data data) {
        if (image == null) {
            return null;
        }
        SourceConfig cfg = getConfig();
        if (cfg != null) {
            if (cfg.grayscale) {
                if (!Utils.isGrayscale(image)) {
                    image = Utils.grayscale(image);
                }
            }
            if (cfg.transpose) {
                image = Utils.transpose(image);
            }
            if ((cfg.flipVertically) || (cfg.flipHorizontally)) {
                image = Utils.flip(image, cfg.flipVertically, cfg.flipHorizontally);
            }
            if (cfg.rotation != 0) {
                image = Utils.rotate(image, cfg.rotation, cfg.rotationCrop);
            }
            if (cfg.scale != 1.0) {
                image = Utils.scale(image, cfg.scale);
            }
            if ((cfg.roiX > 0) || (cfg.roiY > 0) || (cfg.roiWidth >= 0) || (cfg.roiHeight >= 0)) {
                image = image.getSubimage(cfg.roiX, cfg.roiY, cfg.roiWidth, cfg.roiHeight);
            }
            if ((cfg.rescaleFactor != 1.0) || (cfg.rescaleOffset != 0)) {
                image = Utils.rescale(image, cfg.rescaleFactor, cfg.rescaleOffset, false);
            }
            if (cfg.invert) {
                image = Utils.invert(image, false);
            }
        }
        if (filter != null) {
            image = filter.process(image, data);
        }
        return image;
    }

    @Override
    public BufferedImage getImage() {
        return currentImage;
    }

    @Override
    public BufferedImage getOutput() {
        if ((currentImage != null) || (currentData != null)) {
            return applyTransformations(currentImage, currentData);
        }
        return null;
    }

    class ContrastMeasure extends DummyRegister implements ImageListener {

        ContrastMeasure() {
            super(SourceBase.this.getName() + "_contrast");
        }

        @Override
        protected Double doRead() throws IOException, InterruptedException {
            Data data = getData();
            return data == null ? Double.NaN : getData().getGradientVariance(false, null);
        }

        @Override
        protected void doSetMonitored(boolean value) {
            super.doSetMonitored(value);
            if (value) {
                SourceBase.this.addListener(this);
            } else {
                SourceBase.this.removeListener(this);
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

    }

    ReadonlyRegister<Double> contrast;

    @Override
    public ReadonlyRegister<Double> getContrast() {
        if (contrast == null) {
            contrast = new ContrastMeasure();
        }
        return contrast;
    }

    final Object waitLock = new Object();

    @Override
    public void waitNext(int timeout) throws InterruptedException, java.util.concurrent.TimeoutException {
        Chrono chrono = new Chrono();
        int wait = Math.max(timeout, 0);
        int current = count;
        while (count == current) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            synchronized (waitLock) {
                waitLock.wait(wait);
            }
            if (wait > 0) {
                wait = timeout - chrono.getEllapsed();
                if (wait <= 0) {
                    throw new java.util.concurrent.TimeoutException();
                }
            }
        }
    }

    @Override
    public Data getData() {
        if (currentData == null) {
            if (currentImage == null) {
                return null;
            }
            currentData = new Data(currentImage);
            currentData.setSourceConfig(getConfig());
        }
        return currentData;
    }

    public BufferedImage generateImage(Data data) {
        return (data == null) ? null : applyTransformations(null, data);
    }

    @Override
    public void refresh() {
        if (!isClosed()) {
            if (getState() == State.Ready) {
                BufferedImage image = applyTransformations(currentImage, currentData);
                triggerImage(image, currentData);
            }
        }
    }

    @Override
    public void addListener(ImageListener listener) {
        super.addListener(listener);
        refresh();
    }

    protected void pushImage(BufferedImage image) {
        pushImage(image, null);
    }

    protected void pushData(Data data) throws IOException {
        pushImage(null, data);        
    }

    protected void pushImage(BufferedImage image, Data data) {
        if (!isClosed()) {
            if (getPaused()){
                chrono = new Chrono();
                setState(State.Paused);
                return;
            }
            
            if (backgroundEnabled) {
                if ((backgroundData != null) && (data != null)) {
                    data = new Data(data);
                    data.sub(backgroundData);
                    //data.max(0);
                } else if ((backgroundImage != null) && (image != null)) {
                    image = Utils.sub(image, backgroundImage, false);
                }
            }
            currentImage = image;
            currentData = data;
            if (data != null) {
                data.setSourceConfig(getConfig());
            }
            if ((image != null) || (data != null)) {
                chrono = new Chrono((data != null) ? data.getTimestamp() : System.currentTimeMillis());
                setState(State.Ready);
                image = applyTransformations(image, data);
                count++;
            } else {
                chrono = null;
            }
            synchronized (waitLock) {
                waitLock.notifyAll();
            }
            triggerImage(image, data);
        }
    }

    protected void pushError(Exception ex) {
        if (!isClosed()) {
            chrono = null;
            setState(State.Offline);
            triggerError(ex);
        }
    }
    
    volatile boolean paused;
    public void setPaused(boolean value){
        paused = value;
        if (value){
            if (getState()==State.Ready){
                setState(State.Paused);
            }
        } else {
            if (getState()==State.Paused){
                setState(State.Ready);
            }
        }
    }

    public boolean getPaused(){
        return paused;
    }

    void triggerImage(BufferedImage image, Data data) {
        for (ImageListener listener : getListeners()) {
            try {
                listener.onImage(this, image, data);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    void triggerError(Exception e) {
        for (ImageListener listener : getListeners()) {
            try {
                listener.onError(this, e);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    Calibration calibration;

    @Override
    public Calibration getCalibration() {
        return (getConfig() == null) ? calibration : getConfig().getCalibration();
    }

    @Override
    public void setCalibration(Calibration calibration) {
        if (getConfig() == null) {
            this.calibration = calibration;
        } else {
            getConfig().setCalibration(calibration);
        }
    }

    @Override
    public void setCalibration(double scaleX, double scaleY, double offsetX, double offsetY) {
        setCalibration(new Calibration(scaleX, scaleY, offsetX, offsetY));
    }

    @Override
    public Integer getAge() {
        if (chrono == null) {
            return null;
        }
        return chrono.getEllapsed();
    }

    @Override
    public Long getTimestamp() {
        if (chrono == null) {
            return null;
        }
        return chrono.getTimestamp();
    }

    @Override
    public Object take() {
        return count;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public void setBackgroundData(Data data) {
        backgroundImage = null;
        backgroundData = (data == null) ? null : new Data(data);
    }

    @Override
    public Data getBackgroundData() {
        return backgroundData;
    }

    @Override
    public void setBackgroundImage(BufferedImage image) {
        backgroundData = null;
        backgroundImage = (image == null) ? null : Utils.copy(image, null, null);
    }

    @Override
    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    @Override
    public void captureBackground(int images, int delay) throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        boolean back = backgroundEnabled;
        backgroundEnabled = false;
        backgroundImage = null;
        backgroundData = null;
        try {
            waitNext(0);
            if (currentData != null) {
                Class type = (currentData.getType() == float.class) ? float.class : double.class;
                Data aux = new Data(type, false, currentData.width, currentData.height);
                for (int i = 0; i < images; i++) {
                    waitNext(0);
                    aux.sum(new Data(currentData, type, false));
                    Thread.sleep(delay);
                }
                aux.max(0);
                aux.div(images);
                backgroundData = new Data(aux, currentData.getType(), currentData.unsigned);
            } else {
                if (currentImage == null) {
                    throw new IOException("No image");
                }
                //TODO: not averaging images
                backgroundImage = Utils.copy(currentImage, null, null);
            }
        } finally {
            backgroundEnabled = back;
        }
    }

    volatile boolean backgroundEnabled;

    @Override
    public void setBackgroundEnabled(boolean value) {
        backgroundEnabled = value;
    }

    @Override
    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    @Override
    public void saveBackground(String name) {
        Path file = Paths.get(GenericDevice.getConfigPath(), ((name == null) ? getName() : name) + ".bkg");
        try {
            Files.delete(file);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
        try {
            if (backgroundImage != null) {
                ImageIO.write(backgroundImage, "png", file.toFile());
            } else if (backgroundData != null) {
                Files.write(file, Serializer.encode(backgroundData, Serializer.EncoderType.bin));
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void loadBackground(String name) {
        Path file = Paths.get(GenericDevice.getConfigPath(), ((name == null) ? getName() : name) + ".bkg");
        setBackgroundImage(null);
        try {
            setBackgroundData((Data) Serializer.decode(Files.readAllBytes(file)));
        } catch (Exception e) {
            try {
                setBackgroundImage(ImageIO.read(file.toFile()));
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }
    }

    @Override
    public SourceConfig getConfig() {
        return (SourceConfig) super.getConfig();
    }

    @Override
    public Object getArray() {
        if (currentImage == null) {
            if (currentData != null) {
                return currentData.getTransformedMatrix();
            }
            return null;
        }
        BufferedImage img = currentImage;
        if (!Utils.isGrayscale(img)) {
            img = Utils.grayscale(img);
        }
        int width = img.getWidth();
        int height = img.getHeight();

        DataBuffer buffer = img.getRaster().getDataBuffer();
        if (buffer instanceof java.awt.image.DataBufferByte) {
            byte[] arr = ((java.awt.image.DataBufferByte) img.getRaster().getDataBuffer()).getData();
            short[] sarr = Convert.toUnsigned(arr);
            return Convert.reshape(sarr, height, width);
        } else if (buffer instanceof java.awt.image.DataBufferShort) {
            short[] arr = ((java.awt.image.DataBufferShort) img.getRaster().getDataBuffer()).getData();
            int[] iarr = Convert.toUnsigned(arr);
            return Convert.reshape(iarr, height, width);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    protected ScheduledExecutorService newPollingScheduller(int interval, Runnable r) {
        return Threading.scheduleAtFixedRateNotRetriggerable(r, 10, interval, TimeUnit.MILLISECONDS, "Image source scheduler: " + getName());
    }

    @Override
    public ReadableMatrix getDataMatrix() {
        return new ReadableMatrix() {
            @Override
            public Object read() throws IOException, InterruptedException {
                Data data = getData();
                return (data == null) ? null : data.getMatrix();
            }

            @Override
            public int getWidth() {
                Data data = getData();
                return (data == null) ? 0 : data.getWidth();
            }

            @Override
            public int getHeight() {
                Data data = getData();
                return (data == null) ? 0 : data.getHeight();
            }

            @Override
            public String getName() {
                return SourceBase.this.getName();
            }
        };
    }

    //Timeout management   
    volatile Timer timeoutTimer;

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
        if (timeout > 0) {
            Chrono timeoutChrono = new Chrono();
            timeoutTimer = new Timer(getName() + " timeout timer", true);
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (timeoutChrono.getEllapsed() >= timeout) {
                            Integer age = getAge();
                            if ((age == null) || (age >= timeout)) {
                                setTimeout(0);
                                onTimeout();
                            }
                        }

                    } catch (Exception ex) {
                        getLogger().log(Level.WARNING, null, ex);
                    }
                }
            }, 1000, 1000);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    protected void onTimeout() {
    }

    //Overridables
    @Override
    protected void doClose() throws IOException {
        setTimeout(-1);
        chrono = null;
    }

}
