package ch.psi.pshell.bs;

import ch.psi.pshell.bs.ProviderConfig.SocketType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.imaging.Calibration;
import ch.psi.pshell.imaging.ColormapSource;
import ch.psi.pshell.imaging.ColormapSourceConfig;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An imaging source from a bsread source. 
 * "image" (waveform) is the default image channel name.
 * "width" and "height" are optional (image channel shape is used instead if defined)
 * "x_axis" and "y_axis" are optional for calibrating the image, and if present determine shape.
 * identifiers, containing the x and y axis values, used to calibrate the image.
 */
public class StreamCamera extends ColormapSource {

    final Provider provider;
    final Stream stream;
    
    String channelImage = "image";
    String channelAxisX = "x_axis";
    String channelAxisY = "y_axis";
    String channelWidth = "width";
    String channelHeight = "height";

    public StreamCamera(String name) {
        this(name, null);
    }

    public StreamCamera(String name, String host, int port) {
        this(name, "tcp://" + host + ":" + port);
    }
    
    public StreamCamera(String name, String streamSocket) {
        this(name, streamSocket, (ColormapSourceConfig)null);
    }

    public StreamCamera(String name, String host, int port, String channelImage) {
        this(name, host, port);
        this.channelImage = channelImage;     
    }


    public StreamCamera(String name, String streamSocket, String channelImage) {
        this(name, streamSocket);
        this.channelImage = channelImage;   
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

    public void setStreamSocket(String streamSocket) throws IOException, InterruptedException {     
        if (isInitialized()){
            if ((streamSocket==null) || !streamSocket.equals(provider.getAddress())){
                stream.initialize();                            
            }   
        }
        provider.setAddress(streamSocket);
    }
    
    public String getChannelImage() {
        return channelImage;
    }

    public void setChannelImage(String value) {
        channelImage = value;
    }    

    public String getChannelAxisY() {
        return channelAxisY;
    }

    public void setChannelAxisY(String value) {
        channelAxisY = value;
    }    

    public String getChannelWidth() {
        return channelWidth;
    }

    public void setChannelWidth(String value) {
        channelWidth = value;
    }    
    
    public String getChannelHeight() {
        return channelHeight;
    }

    public void setChannelHeight(String value) {
        channelHeight = value;
    }    

    public Object getValue(String name) {
        return stream.getValue(name);
    }

    public int[] getShape(String name) {
        return stream.getShape(name);
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
            if (stream.getStreamSocket()!=null){
                startReceiver();
            }
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

    DeviceListener streamListener = new DeviceListener() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                Object image = getValue(channelImage);
                int[] shape = getShape(channelImage);
                Number width=0, height=0;
                
                Object x = getValue(channelAxisX);
                Object y = getValue(channelAxisY);
                if ((x != null) && (y != null)) {
                    width = Array.getLength(x);
                    height = Array.getLength(y);
                    setCalibration(new Calibration(x, y));
                } else {                
                    if ((shape!=null) && (shape.length==2)){                        
                        width=shape[0];
                        height=shape[1];
                    } else {
                        width = (Number) getValue(channelWidth);
                        height = (Number) getValue(channelHeight);
                    }
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
        super.doClose();
    }
}
