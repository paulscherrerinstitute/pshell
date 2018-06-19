package ch.psi.pshell.bs;

import ch.psi.pshell.bs.ProviderConfig.SocketType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.imaging.Calibration;
import ch.psi.pshell.imaging.ColormapSource;
import ch.psi.utils.State;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An imaging source from a bsread source. The following identifiers are mandatory in the stream:
 * "image" (waveform), "width" and "height" (scalars). "x_axis" and "y_axis" are optional
 * identifiers, containing the x and y axis values, used to calibrate the image.
 */
public class StreamCamera extends ColormapSource {

    final Provider provider;
    final Stream stream;

    public StreamCamera(String name) {
        this(name, null);
    }

    public StreamCamera(String name, String host, int port) {
        this(name, "tcp://" + host + ":" + port);
    }


    public StreamCamera(String name, String streamSocket) {
        this(name, streamSocket, null);
    }

    protected StreamCamera(String name, String streamSocket, ColormapSourceConfig config) {
        super(name, (config == null) ? new ColormapSourceConfig() : config);
        provider = new Provider(name + " provider", streamSocket, SocketType.SUB);
        stream = new Stream(name + " stream", provider);
        stream.addListener(streamListener);
    }

    public String getStreamSocket() {
        return provider.getAddress();
    }

    public void setStreamSocket(String socket) {
        provider.setAddress(socket);
    }

    public Object getValue(String name) {
        StreamValue cache = stream.take();
        if (cache != null) {
            for (int i = 0; i < cache.identifiers.size(); i++) {
                if (cache.identifiers.get(i).equals(name)) {
                    return cache.values.get(i);
                }
            }
        }
        return null;
    }

    public StreamValue getValue() {
        return stream.take();
    }

    public Stream getStream() {
        return stream;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        provider.initialize();
        stream.initialize();
        if (isMonitored()) {
            startReceiver();
        }
    }

    public void startReceiver() {
        try {
            stream.start(true);
        } catch (Exception ex) {
            Logger.getLogger(StreamCamera.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stopReceiver() {
        try {
            stream.stop();
        } catch (Exception ex) {
            Logger.getLogger(StreamCamera.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (isInitialized()) {
            if (value) {
                startReceiver();
            } else {
                stopReceiver();
            }
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        Object cache = stream.take();
        if (!isMonitored()) {
            try {
                startReceiver();
                stream.waitValueNot(cache, -1);
            } finally {
                stopReceiver();
            }
        } else {
            stream.waitValueNot(cache, -1);
        }
    }

    DeviceListener streamListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                Object image = getValue("image");
                Number width = (Number) getValue("width");
                Number height = (Number) getValue("height");
                Object x = getValue("x_axis");
                Object y = getValue("y_axis");
                if ((x != null) && (y != null)) {
                    width = Array.getLength(x);
                    height = Array.getLength(y);
                    setCalibration(new Calibration(x, y));
                }
                if (image == null) {
                    pushData(null);
                } else if ((width == null) || (height == null)) {
                    pushError(new Exception("Image size is not defined"));
                } else {
                    pushData(image, width.intValue(), height.intValue());
                }
            } catch (Exception ex) {
                pushError(ex);
            }
        }

        @Override
        public void onStateChanged(Device device, State state, State former) {
            if (state == State.Ready) {
                setState(State.Ready);
            } else if (state == State.Busy) {
                setState(State.Busy);
            }
        }
    };

    @Override
    protected void doClose() throws IOException {
        provider.close();
        stream.close();
    }
}
