package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.utils.Str;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.pshell.bs.AddressableDevice;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class CameraStream extends ReadonlyRegisterBase<StreamValue> implements ch.psi.pshell.device.Readable.ReadableType, CamServerStream {

    final CameraClient client;
    final ProxyClient proxy;
    final String instanceId;

    String instance;
    boolean push;

    Stream stream;

    public CameraClient getClient() {
        return client;
    }
    
    public ProxyClient getProxy() {
        return proxy;
    }
    
    public CameraStream(String name, String url, String instanceId) {
        super(name, null);
        client = new CameraClient(url);
        proxy = new ProxyClient(url);
        this.instanceId = instanceId;
    }
    
    @Override
    public Class _getElementType() {
        return Long.class;
    }        

    public Object getValue(String name) {
        StreamValue cache = take();
        if (cache != null) {
            for (int i = 0; i < cache.getIdentifiers().size(); i++) {
                if (cache.getIdentifiers().get(i).equals(name)) {
                    return cache.getValues().get(i);
                }
            }
        }
        return null;
    }
    
    public Device getChannel(String name){
        if (stream!=null){
            return stream.getChild(name);
        }
        return null;
    }
    
    public List<Device> getChannels(){
        if (stream!=null){
            return Arrays.asList(stream.getChildren());
        }
        return new ArrayList();
    }

    public List<String> getChannelNames(){
        if (stream!=null){
            return stream.getIdentifiers();
        }
        return new ArrayList();
    }

    public Stream getStream() {
        return stream;
    }
    
    protected void startReceiver() {
        try {
            stream.start(true);
        } catch (Exception ex) {
            Logger.getLogger(CameraStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void stopReceiver() {
        try {
            stream.stop();
        } catch (Exception ex) {
            Logger.getLogger(CameraStream.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (isInitialized() && (stream != null)) {
            if (value) {
                startReceiver();
            } else {
                stopReceiver();
            }
        }
    }
    
    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (stream != null){
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
    }
    
    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        start();
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return client.getUrl();
    }

    /**
     * Return the name of the current streaming camera.
     */
    public String getInstance() {
        return instance;
    }
    
    @Override
    public String getAddress() {
        return getUrl() + "/" + Str.toString(instanceId);
    }    
    
    @Override
    protected StreamValue doRead() throws IOException, InterruptedException {
        update();
        return take();
    }

    @Override
    public StreamValue request() {
        if (stream == null) {
            return null;
        }
        return stream.request();
    }

    /**
     * Return the info of the  server instance.
     */
    public Map<String, Object> getServerInfo() throws IOException {
        return client.getInfo();
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        return client.getCameras();
    }
    /**
     * Return the camera configuration.
     */
    public Map<String, Object> getConfig(String name) throws IOException {
        return client.getConfig(name);
    }

    public Map<String, Object> getInstanceConfig() throws IOException {
        return getConfig(instanceId);
    }
    
    /**
     * Set camera config
     */
    public void setConfig(String name, Map<String, Object> config) throws IOException {
        client.setConfig(name, config);
    }

    public void setInstanceConfig(Map<String, Object> config) throws IOException {
        setConfig(instanceId, config);
    }    
    /**
     * Delete configuration of the camera.
     */
    public void deleteConfig(String name) throws IOException {
        client.deleteConfig(name);
    }

    public void deleteInstanceConfig() throws IOException {
        deleteConfig(instanceId);
    }
    
    
    /**
     * Returns URl of stream to the camera
     */
    protected String getStream(String instanceId) throws IOException {
        return client.getStream(instanceId);
    }

    /**
     * Stop the camera.
     */
    public void stopInstance(String instanceId) throws IOException {
        client.stopInstance(instanceId);
    }

    /**
     * Stop all the cameras on the server.
     */
    public void stopAllInstances() throws IOException {
        client.stopAllInstances();
    }

    void startInstance(String instanceId) throws IOException {
        stop();
        String streamUrl = getStream(instanceId);
        stream = new Stream(getName() + " stream", streamUrl, false);
        this.instance = instanceId;
    }
     
    
     /**
     * Start the camera.
     */
    public void start() throws IOException, InterruptedException {
        startInstance(instanceId);

        if (stream != null) {
            stream.addListener(new DeviceAdapter() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    setCache(value, timestamp);
                }
            });            
            stream.initialize();
            if (isMonitored()) {
                startReceiver();
            }
        }  
    }
    
    /**
     * Stop the camera.
     */
    public void stop() {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                Logger.getLogger(CameraStream.class.getName()).log(Level.WARNING, null, ex);
            }
            stream = null;
        }
    }

    @Override
    protected void doClose() throws IOException {
        stop();
        for (Device d: this.getComponents()){
            try {
                d.close();
            } catch (Exception ex) {
                Logger.getLogger(CameraStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.doClose();
    }  
}
