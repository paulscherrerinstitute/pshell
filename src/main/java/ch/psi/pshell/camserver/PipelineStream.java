package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.Provider;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.utils.Str;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.pshell.bs.AddressableDevice;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class PipelineStream extends ReadonlyRegisterBase<StreamValue> implements ch.psi.pshell.device.Readable.ReadableType, AddressableDevice {

    final PipelineClient client;
    final ProxyClient proxy;
    final String pipelineName;
    final String instanceId;
    final Map<String, Object> config;

    String pipeline;
    String instance;
    boolean push;

    Stream stream;

    public PipelineClient getClient() {
        return client;
    }
    
    public ProxyClient getProxy() {
        return proxy;
    }
    
    public PipelineStream(String name, String url, String instanceId) {
        super(name, null);
        client = new DefaultProcessingPipelineClient(url);
        proxy = new ProxyClient(url);
        this.instanceId = instanceId;
        this.pipelineName = null;
        this.config = null;
    }

    public PipelineStream(String name, String url, String pipelineName, String instanceId) {
        super(name, null);
        client = new DefaultProcessingPipelineClient(url);
        proxy = new ProxyClient(url);
        this.pipelineName = pipelineName;
        this.instanceId = instanceId;
        this.config = null;
    }

    public PipelineStream(String name, String url, Map<String, Object> config, String instanceId) {
        super(name, null);
        client = new DefaultProcessingPipelineClient(url);
        proxy = new ProxyClient(url);
        this.pipelineName = null;
        this.instanceId = instanceId;
        this.config = config;
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
            Logger.getLogger(PipelineStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void stopReceiver() {
        try {
            stream.stop();
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
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
     * Return the name of the current streaming pipeline.
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Return the name of the current streaming pipeline.
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

    public interface ConfigChangeListener {

        void onConfigChanged(Map<String, Object> config);
    }

    private ConfigChangeListener configChangeListener;

    public void setConfigChangeListener(ConfigChangeListener listener) {
        configChangeListener = listener;
    }

    public ConfigChangeListener getConfigChangeListener() {
        return configChangeListener;
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
     * List existing pipelines.
     */
    public List<String> getPipelines() throws IOException {
        return client.getPipelines();
    }

    /**
     * List running instances.
     */
    public List<String> getInstances() throws IOException {
        return client.getInstances();
    }

    /**
     * Return the pipeline configuration.
     */
    public Map<String, Object> getConfig(String pipelineName) throws IOException {
        return client.getConfig(pipelineName);
    }

    /**
     * Set configuration of the pipeline.
     */
    public void setConfig(String pipelineName, Map<String, Object> config) throws IOException {
        client.setConfig(pipelineName, config);
    }

    /**
     * Delete configuration of the pipeline.
     */
    public void deleteConfig(String pipelineName) throws IOException {
        client.deleteConfig(pipelineName);
    }

    /**
     * Return the instance configuration.
     */
    public Map<String, Object> getInstanceConfig(String instanceId) throws IOException {
        return client.getInstanceConfig(instanceId);
    }

    public Object getInstanceConfigValue(String instanceId, String value) throws IOException {
        Map<String, Object> pars = getInstanceConfig(instanceId);
        return pars.get(value);
    }

    /**
     * Set instance configuration.
     */
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        client.setInstanceConfig(instanceId, config);
        if (configChangeListener != null) {
            if (instanceId.equals(instanceId)) {
                configChangeListener.onConfigChanged(config);
            }
        }
    }

    public void setInstanceConfigValue(String instanceId, String name, Object value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put(name, value);
        setInstanceConfig(instanceId, pars);
    }

    /**
     * Return the instance info.
     */
    public Map<String, Object> getInstanceInfo(String instanceId) throws IOException {
        return client.getInstanceInfo(instanceId);
    }

    /**
     * Return the instance stream. If the instance does not exist, it will be
     * created and will be read only - no config changes will be allowed. If
     * instanceId then return existing (writable).
     */
    protected String getStream(String instanceId) throws IOException {
        return client.getStream(instanceId);
    }

    /**
     * Create a pipeline from a config file. Pipeline config can be changed.
     * Return pipeline instance id and instance stream in a list.
     */
    protected List<String> createFromName(String pipelineName, String id) throws IOException {
        return client.createFromName(pipelineName, id);
    }

    /**
     * Create a pipeline from the provided config. Pipeline config can be
     * changed. Return pipeline instance id and instance stream in a list.
     */
    protected List<String> createFromConfig(Map<String, Object> config, String id) throws IOException {
        return client.createFromConfig(config, id);
    }

    /**
     * Set config of the pipeline. Return actual applied config.
     */
    public String savePipelineConfig(String pipelineName, Map<String, Object> config) throws IOException {
        return client.savePipelineConfig(pipelineName, config);
    }

    /**
     * Stop the pipeline.
     */
    public void stopInstance(String instanceId) throws IOException {
        client.stopInstance(instanceId);
    }

    /**
     * Stop all the pipelines on the server.
     */
    public void stopAllInstances() throws IOException {
        client.stopAllInstances();
    }

    /**
     * Collect the background image on the selected camera. Return background
     * id.
     */
    public String captureBackground(String cameraName, Integer images) throws IOException {
        return client.captureBackground(cameraName, images);
    }

    public String getLastBackground(String cameraName) throws IOException {
        return client.getLastBackground(cameraName);
    }

    public List<String> getBackgrounds(String cameraName) throws IOException {
        return client.getBackgrounds(cameraName);
    }

    public BufferedImage getLastBackgroundImage(String cameraName) throws IOException {
        return getBackgroundImage(getLastBackground(cameraName));
    }

    public BufferedImage getBackgroundImage(String name) throws IOException {
        return client.getBackgroundImage(name);
    }

    void startInstance(String instanceId) throws IOException {
        stop();
        push = false;
        String streamUrl = getStream(instanceId);
        try {
            push = proxy.isPush(pipelineName);
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
        }
        stream = new Stream(getName() + " stream", streamUrl, push);
        this.pipeline = instanceId;
        this.instance = instanceId;

    }

    void startPipeline(String pipelineName, String instanceId) throws IOException {
        stop();
        push = false;
        List<String> ret = createFromName(pipelineName, instanceId);
        try {
            push = proxy.isPush(ret.get(0));
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
        }
        stream = new Stream(getName() + " stream", ret.get(1), push);
        this.pipeline = pipelineName;
        this.instance = ret.get(0);
    }

    void startPipeline(Map<String, Object> config, String instanceId) throws IOException {
        stop();
        push = false;
        List<String> ret = createFromConfig(config, instanceId);
        try {
            push = proxy.isPush(ret.get(0));
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
        }
        stream = new Stream(getName() + " stream", ret.get(1), push);
        this.pipeline = pipelineName;
        this.instance = ret.get(0);
    }

 
     /**
     * Start the pipeline.
     */
    public void start() throws IOException, InterruptedException {
        if ((pipelineName == null) && (config == null)) {
            if (getInstances().contains(instanceId)) {
                startInstance(instanceId);
            } else {
                startPipeline(instanceId, instanceId);
            }
        } else {
            if (getInstances().contains(instanceId)) {
                //If instance id is present connect to it instead of raising errors
                Logger.getLogger(PipelineStream.class.getName()).warning("Instance already present in server: " + instanceId);
                this.startInstance(instanceId);
            } else {            
                if (config != null) {
                    startPipeline(config, instanceId);
                } else {
                    startPipeline(pipelineName, instanceId);
                }
            }
        }

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
     * Stop the pipeline.
     */
    public void stop() {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
            }
            stream = null;
        }
    }

    /**
     * Return the current instance info.
     */
    public Map<String, Object> getInstanceInfo() throws IOException {
        assertInitialized();
        return getInstanceInfo(instanceId);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getReadonly() throws IOException {
        assertInitialized();
        return client.getReadonly(instanceId);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getActive() throws IOException {
        assertInitialized();
        return client.getActive(instanceId);
    }

    /**
     * Return if the current instance id.
     */
    public String getInstanceId() throws IOException {
        assertInitialized();
        return client.getInstanceId(instanceId);
    }

    /**
     * Return if the current instance stream address
     */
    public String getStreamAddress() throws IOException {
        assertInitialized();
        return client.getStreamAddress(instanceId);
    }

    /**
     * Return the current instance camera.
     */
    public String getCameraName() throws IOException {
        assertInitialized();
        return client.getCameraName(instanceId);
    }

    /**
     * Set current instance configuration.
     */
    public void setInstanceConfig(Map<String, Object> config) throws IOException {
        assertInitialized();
        setInstanceConfig(instanceId, config);
    }

    public void setInstanceConfigValue(String name, Object value) throws IOException {
        assertInitialized();
        setInstanceConfigValue(instanceId, name, value);
    }

    /**
     * Return the current instance info.
     */
    public Map<String, Object> getInstanceConfig() throws IOException {
        assertInitialized();
        return getInstanceConfig(instanceId);
    }

    public Object getInstanceConfigValue(String value) throws IOException {
        assertInitialized();
        return getInstanceConfigValue(instanceId, value);
    }

    public String getFunction() throws IOException {
        assertInitialized();
        return client.getFunction(instanceId);
    }

    public void setFunction(String function) throws IOException {
        assertInitialized();
        client.setFunction(instanceId, function);
    }

    public void sendFunctionScript(String fileName) throws IOException {
        client.setScriptFile(fileName);
    }

    public void setFunctionScript(String fileName) throws IOException {
        sendFunctionScript(fileName);
        setFunction(new File(fileName).getName());
    }

    @Override
    protected void doClose() throws IOException {
        configChangeListener = null;
        stop();
        for (Device d: this.getComponents()){
            try {
                d.close();
            } catch (Exception ex) {
                Logger.getLogger(PipelineStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.doClose();
    }

    public Stream createSubsampled(double maxFrameRate) throws IOException, InterruptedException{
        return createSubsampled(maxFrameRate, null);
    }
    
    public Stream createSubsampled(int downsampling) throws IOException, InterruptedException{
        return createSubsampled(null, downsampling);
    }    
    
    protected Stream createSubsampled(Double maxFrameRate, Integer downsampling) throws IOException, InterruptedException{
        Map<String, Object> config = new HashMap<>();
        config.put("name", instance + "_subsampled");
        config.put("input_mode", push ? "PULL" : "SUB");
        config.put("input_pipeline", instanceId);
        config.put("pipeline_type", "stream");
        config.put("mode", "PUB");
        config.put("function", "propagate_stream");
        if (maxFrameRate!=null){
            config.put("max_frame_rate", maxFrameRate);
        } else {
            config.put("downsampling", downsampling);
        }
        List<String> ret = createFromConfig(config, null);
        Stream stream = new Stream(getName() + " subsampled", ret.get(1), false);
        stream.setMonitored(true);
        stream.initialize();
        stream.start(true);
        addComponent(stream);
        return stream;
    }       
}
