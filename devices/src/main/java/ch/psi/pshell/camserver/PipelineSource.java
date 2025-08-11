package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.StreamCamera;
import ch.psi.pshell.imaging.ColormapSourceConfig;
import ch.psi.pshell.utils.EncoderJson;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class PipelineSource extends StreamCamera {

    //final PipelineClient client;
    final DefaultProcessingPipelineClient client; //Using instead of PipelineClient to easilly add methods to access stadndard processing pipeline
    String currentPipeline;
    String currentInstance;
    Boolean currentShared;
    Boolean currentReadonly;

    public interface ConfigChangeListener {
        void onConfigChanged(Map<String, Object> config);
    }
    /**
     * Optional persisted configuration of CameraServer objects.
     */
    public static class PipelineSourceConfig extends ColormapSourceConfig {

        public String serverURL = PipelineClient.DEFAULT_URL;
    }
    
    public DefaultProcessingPipelineClient getClient() {
        return client;
    }    

    public PipelineSource(String name) {
        this(name, null, new PipelineSourceConfig());
    }

    public PipelineSource(String name, String url) {
        this(name, url, null);
    }

    public PipelineSource(String name, String host, int port) {
        this(name, "http://" + host + ":" + port);
    }

    protected PipelineSource(String name, String url, ColormapSourceConfig cfg) {
        super(name, null, cfg);
        if (cfg instanceof PipelineSourceConfig pipelineSourceConfig) {
            url = pipelineSourceConfig.serverURL;
        }
        client = new DefaultProcessingPipelineClient(url);
    }

    /**
     * Return the REST API endpoint address.
     */
    public String getUrl() {
        return client.getUrl();
    }   
    
    /**
     * Return the name of the current pipeline name.
     */
    public String getCurrentPipeline() {
        return currentPipeline;
    }

    public void assertSavedConfig() throws IOException {
        if ((currentPipeline == null) || (currentPipeline.isEmpty())){
            throw new IOException ("No persisted pipeline configuration");
        }
    }


    /**
     * Return the name of the current streaming camera.
     */
    public String getCurrentCamera() {
        try {
            return String.valueOf(getInstanceInfo().get("camera_name"));
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Return the name of the current pipeline instance.
     */
    public String getCurrentInstance() {
        return currentInstance;
    }
    
    /**
     * Return if the current instance is a shared connection.
     */
    public Boolean getCurrentShared() {
        return currentShared;
    }    

    public Boolean getCurrentReadonly() {
        return currentReadonly;
    }    

    void assertStarted() throws IOException {
        if (!isStarted()) {
            throw new IOException("Pipeline not started");
        }
    }
    
    private ConfigChangeListener configChangeListener;
    
    public void setConfigChangeListener(ConfigChangeListener listener){
        configChangeListener = listener;
    }

    public ConfigChangeListener getConfigChangeListener(){
        return configChangeListener;
    }

    /**
     * Return the info of the cam server instance.
     */
    public Map<String, Object> getInfo() throws IOException {
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
        if (configChangeListener!=null){
            if (instanceId.equals(currentInstance)){        
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
     * Return the instance stream. If the instance does not exist, it will be created and will be
     * read only - no config changes will be allowed. If instanceId then return existing (writable).
     */
    public String getStream(String instanceId) throws IOException {
        return client.getStream(instanceId);
    }

    /**
     * Create a pipeline from a config file. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromName(String pipelineName, String id) throws IOException {
        return client.createFromName(pipelineName, id);
    }

    /**
     * Create a pipeline from a config file and additional configuration. 
     * instance id and instance stream in a list.
     */
    public List<String> createFromName(String pipelineName, String id, Map<String, Object> additionalConfig) throws IOException {
        return client.createFromName(pipelineName, id, additionalConfig);
    }

    /**
     * Create a readonly pipeline from a config file and additional configuration. 
     * instance id and instance stream in a list.
     */
    public List<String> createReadonlyFromName(String pipelineName, String id) throws IOException {
        return client.createReadonlyFromName(pipelineName, id);
    }

    /**
     * Create a pipeline from the provided config. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromConfig(Map<String, Object> config, String id) throws IOException {
        return client.createFromConfig(config, id);
    }

    /**
     * Create readonly a pipeline from the provided config. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createReadonlyFromConfig(Map<String, Object> config, String id) throws IOException {
        return client.createReadonlyFromConfig(config, id);
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
     * Collect the background image on the selected camera. Return background id.
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
       

    public boolean isInstanceRunning(String instanceId) throws IOException{
        return getInstances().contains(instanceId);
    }
    /**
     * Start pipeline streaming, creating a private instance, and set the stream endpoint to the
     * current stream socket.
     */
    public void start(String pipelineName) throws IOException, InterruptedException {
        start(pipelineName, false);
    }

    /**
     * Start pipeline streaming, and set the stream endpoint to the current stream socket. 
     * If shared is true, start the shared instance of the pipeline, else create a 
     * private instance with unique id.
     */
    public void start(String pipelineName, boolean shared) throws IOException, InterruptedException {
        if (shared){
            start(pipelineName, pipelineName, false);
        } else {
            start(pipelineName, null, false);
        }
    }

    /**
     * Start pipeline streaming, creating instance called instanceId. 
     * If instance name already running,  connects to it (even if pipeline was different.)
     * than the pipeline name, instance is not readonly.
     */
    public void start(String pipelineName, String instanceId) throws IOException, InterruptedException {
        start(pipelineName, instanceId, false);
    }    

    public void start(String pipelineName, String instanceId, boolean readonly) throws IOException, InterruptedException {
        stop();
        boolean shared = (pipelineName !=null) && (pipelineName.equals(instanceId));
        if (pipelineName==null){
            if (!isInstanceRunning(instanceId)) {
                throw new IOException("Instance not started: " + instanceId);
            } 
            String stream = getStream(instanceId);
            setStreamSocket(stream);
            currentPipeline = pipelineName;
            currentInstance = instanceId;            
        } else if (shared){
            if (isInstanceRunning(instanceId)) {
                String stream = getStream(pipelineName);
                setStreamSocket(stream);
            } else {                
                List<String> ret = readonly ? createReadonlyFromName(pipelineName, instanceId) : createFromName(pipelineName, instanceId);
                setStreamSocket(ret.get(1));
            }
            currentPipeline = pipelineName;
            currentInstance = instanceId;
        } else {            
            List<String> ret = readonly ? createReadonlyFromName(pipelineName, instanceId) : createFromName(pipelineName, instanceId);
            setStreamSocket(ret.get(1));
            currentPipeline = pipelineName;
            currentInstance = ret.get(0);
        }
        currentShared = shared;
        currentReadonly = readonly;
        startReceiver();
        getStream().setChannelPrefix(currentInstance);        
    }

           
    public void startConfig(Map<String, Object> config) throws IOException, InterruptedException {
        startConfig(config, null);
    }
    
    public void startConfig(Map<String, Object> config, String instanceId) throws IOException, InterruptedException {
        startConfig(config, instanceId, false);
    }
    
    public void startConfig(Map<String, Object> config, String instanceId, boolean readonly) throws IOException, InterruptedException {
        stop();
        List<String> ret = readonly ? createReadonlyFromConfig(config, instanceId) : createFromConfig(config, instanceId);
        setStreamSocket(ret.get(1));
        currentPipeline = String.valueOf(config.getOrDefault("name", "unknown"));
        currentInstance = ret.get(0);
        currentShared = false;
        currentReadonly = readonly;
        startReceiver();
        getStream().setChannelPrefix(currentInstance);                
    }
        
    public void connect(String instanceId) throws IOException, InterruptedException {
        start(null, instanceId, false);
    }       
        
    public void connectOrStart(String pipelineName, String instanceId) throws IOException, InterruptedException {
        connectOrStart(pipelineName, instanceId, false);
    }
    
    public void connectOrStart(String pipelineName, String instanceId, boolean readonly) throws IOException, InterruptedException {
        if (isInstanceRunning(instanceId)){
            connect(instanceId);
        } else {
            start(pipelineName, instanceId, readonly);
        }
    }
    
    /**
     * Stop receiving from current pipeline.
     */
    public void stop() throws IOException {
        this.currentPipeline = null;
        this.currentInstance = null;
        getStream().setChannelPrefix(null);    
        stopReceiver();
    }

    /**
     * Return the current instance info.
     */
    public Map<String, Object> getInstanceInfo() throws IOException {
        assertStarted();
        return getInstanceInfo(currentInstance);
    }
    
    /**
     * Return processing parameters of last massage received
     */
    public Map<String, Object> getProcessingParameters() throws IOException {
        assertStarted();
        Object val = getValue("processing_parameters");
        if (val == null) {
            return null;
        }
        return (Map) EncoderJson.decode(val.toString(), Map.class);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getReadonly() throws IOException {
        assertStarted();
        return client.getReadonly(currentInstance);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getActive() throws IOException {
        assertStarted();
        return client.getActive(currentInstance);
    }

    /**
     * Return if the current instance id.
     */
    public String getInstanceId() throws IOException {
        assertStarted();
        return client.getInstanceId(currentInstance);
    }

    /**
     * Return if the current instance stream address
     */
    public String getStreamAddress() throws IOException {
        assertStarted();
        return client.getStreamAddress(currentInstance);
    }

    /**
     * Return the current instance camera.
     */
    public String getCameraName() throws IOException {
        assertStarted();
        return client.getCameraName(currentInstance);
    }

    /**
     * Set current instance configuration.
     */
    public void setInstanceConfig(Map<String, Object> config) throws IOException {
        assertStarted();
        setInstanceConfig(currentInstance, config);
    }
    
    public void setInstanceConfigValue(String name, Object value) throws IOException {;
        setInstanceConfigValue(currentInstance, name, value);
    }        

    /**
     * Return the current instance info.
     */
    public Map<String, Object> getInstanceConfig() throws IOException {
        assertStarted();
        return getInstanceConfig(currentInstance);
    }
    
    public Object getInstanceConfigValue(String value) throws IOException {       
        return getInstanceConfigValue(currentInstance, value);
    }        
    
    
    public Map<String, Object> getSavedConfig() throws IOException {
        assertStarted();
        assertSavedConfig();
        return getConfig(currentPipeline);
    }
    
    public Object getSavedConfigValue(String value) throws IOException {      
        assertStarted();
        assertSavedConfig();       
        Map<String, Object> pars = getSavedConfig();
        return pars.get(value);
    }    
    
    public void setSavedConfig(Map<String, Object> config) throws IOException {
        assertStarted();
        assertSavedConfig();
        client.setConfig(currentPipeline,config);
    }

    public void setSavedConfigValue(String name, Object value) throws IOException {
        assertStarted();
        assertSavedConfig();
        client.setConfigValue(currentPipeline, name, value);
    }       
    

    public boolean isRoiEnabled() throws IOException {
        assertStarted();
        return client.isRoiEnabled(currentInstance);
    }

    public void resetRoi() throws IOException {
        assertStarted();
        client.resetRoi(currentInstance);
    }

    public void setRoi(int x, int y, int width, int height) throws IOException {
        assertStarted();
        client.setRoi(currentInstance, x, y, width, height);
    }

    public void setRoi(int[] roi) throws IOException {
        assertStarted();
        client.setRoi(currentInstance, roi);
    }

    public int[] getRoi() throws IOException {
        assertStarted();
        return client.getRoi(currentInstance);
    }

    public void setBinning(int binning_x, int binning_y) throws IOException {
        assertStarted();
        client.setBinning(currentInstance, binning_x, binning_y);
    }

    public void setBinning(int binning_x, int binning_y, boolean mean) throws IOException {
        assertStarted();
        client.setBinning(currentInstance, binning_x, binning_y, mean);
    }

    public Map getBinning() throws IOException {
        assertStarted();
        return client.getBinning(currentInstance);
    }

    public String getBackground() throws IOException {
        assertStarted();
        return client.getBackground(currentInstance);
    }

    public void setBackground(String id) throws IOException {
        setBackground(id, true);
    }
    
    public void setBackground(String id, boolean saveConfig) throws IOException {
        assertStarted();
        client.setBackground(currentInstance, id);
        if (saveConfig){
            if (currentPipeline != null){
                //If persisted config had another background, set to use latest
                client.setConfigValue(currentPipeline, "image_background", null);
            }
        }
    }

    public String getLastBackground() throws IOException {
        assertStarted();
        return client.getLastBackground(getCurrentCamera());
    }

    public String captureBackground(Integer images) throws IOException {
        assertStarted();
        String id = client.captureBackground(getCurrentCamera(), images);
        if (id != null) {
            setBackground(id, true); //Use latest
        }
        return id;        
    }
    
    public boolean isBackgroundSubtractionEnabled() throws IOException {
        assertStarted();
        return client.isBackgroundSubtractionEnabled(currentInstance);
    }
    
    public Object getBackgroundSubtraction() throws IOException {
        assertStarted();
        return client.getBackgroundSubtraction(currentInstance);
    }

    public void setBackgroundSubtraction(Object value) throws IOException {
        assertStarted();
        client.setBackgroundSubtraction(currentInstance, value);
    }    

    public Double getThreshold() throws IOException {
        assertStarted();
        return client.getThreshold(currentInstance);
    }

    public void setThreshold(Double value) throws IOException {
        assertStarted();
        client.setThreshold(currentInstance, value);
    }

    public Map<String, Object> getGoodRegion() throws IOException {
        assertStarted();
        return client.getGoodRegion(currentInstance);
    }

    public void setGoodRegion(Map<String, Object> value) throws IOException {
        assertStarted();
        client.setGoodRegion(currentInstance, value);
    }

    public void setGoodRegion(double threshold, double scale) throws IOException {
        assertStarted();
        client.setGoodRegion(currentInstance, threshold, scale);
    }

    public Map<String, Object> getSlicing() throws IOException {
        assertStarted();
        return client.getSlicing(currentInstance);
    }

    public void setSlicing(Map<String, Object> value) throws IOException {
        assertStarted();
        client.setSlicing(currentInstance, value);
    }

    public void setSlicing(int slices, double scale, String orientation) throws IOException {
        assertStarted();
        client.setSlicing(currentInstance, slices, scale, orientation);
    }
        
    public Map<String, Object> getRotation() throws IOException {
        assertStarted();
        return client.getRotation(currentInstance);
    }

    public void setRotation(Map<String, Object> value) throws IOException {
        assertStarted();
        client.setRotation(currentInstance, value);   
    }
    
     public void setRotation(double angle, int order, String mode) throws IOException {
        assertStarted();
        client.setRotation(currentInstance, angle, order, mode);  
    }   
     
    public Number getAveraging() throws IOException {
        assertStarted();
        return client.getAveraging(currentInstance);   
    }
    
     public void setAveraging(Number value) throws IOException {
        assertStarted();
        client.setAveraging(currentInstance, value);  
    }   

    public String getFunction() throws IOException {
        assertStarted();
        return client.getFunction(currentInstance);
    }

    public void setFunction(String function) throws IOException {
        assertStarted();
        client.setFunction(currentInstance, function);  
    }      
    
    
    public void sendFunctionScript(String fileName) throws IOException {
        client.setScriptFile(fileName);         
    }     
    
    public void setFunctionScript(String fileName) throws IOException {
        sendFunctionScript(fileName);
        setFunction(new File(fileName).getName());
    }
    

    public boolean isStarted() {
        return (currentInstance != null);
    }
    
    @Override
    protected void doClose() throws IOException {
        configChangeListener = null;
        super.doClose();
    }    

}
