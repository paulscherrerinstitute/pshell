package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class PipelineStream extends CamServerStream {

    final String pipelineName;
    String pipeline;

    public PipelineStream(String name, String url, String instanceId) {
        super(name, new DefaultProcessingPipelineClient(url), instanceId);
        this.pipelineName = null;
    }

    public PipelineStream(String name, String url, String pipelineName, String instanceId) {
        super(name, new DefaultProcessingPipelineClient(url), instanceId);
        this.pipelineName = pipelineName;
    }

    public PipelineStream(String name, String url, Map<String, Object> config, String instanceId) {
        super(name, new DefaultProcessingPipelineClient(url), config, instanceId);
        this.pipelineName = null;
    }

    public PipelineClient getClient() {
        return (PipelineClient) super.getClient();
    }

    /**
     * Return the name of the current streaming pipeline.
     */
    public String getPipeline() {
        return pipeline;
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
     * List existing pipelines.
     */
    public List<String> getPipelines() throws IOException {
        return getClient().getPipelines();
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        return getClient().getCameras();
    }

    /**
     * List running instances.
     */
    public List<String> getInstances() throws IOException {
        return getClient().getInstances();
    }

    /**
     * Return the instance configuration.
     */
    public Map<String, Object> getInstanceConfig(String instanceId) throws IOException {
        return getClient().getInstanceConfig(instanceId);
    }

    public Object getInstanceConfigValue(String instanceId, String value) throws IOException {
        Map<String, Object> pars = getInstanceConfig(instanceId);
        return pars.get(value);
    }

    /**
     * Set instance configuration.
     */
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        getClient().setInstanceConfig(instanceId, config);
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
        return getClient().getInstanceInfo(instanceId);
    }

    /**
     * Create a pipeline from a config file. Pipeline config can be changed.
     * Return pipeline instance id and instance stream in a list.
     */
    protected List<String> createFromName(String pipelineName, String id) throws IOException {
        return getClient().createFromName(pipelineName, id);
    }

    /**
     * Create a pipeline from the provided config. Pipeline config can be
     * changed. Return pipeline instance id and instance stream in a list.
     */
    protected List<String> createFromConfig(Map<String, Object> config, String id) throws IOException {
        return getClient().createFromConfig(config, id);
    }

    /**
     * Set config of the pipeline. Return actual applied config.
     */
    public String savePipelineConfig(String pipelineName, Map<String, Object> config) throws IOException {
        return getClient().savePipelineConfig(pipelineName, config);
    }

    /**
     * Collect the background image on the selected camera. Return background
     * id.
     */
    public String captureBackground(String cameraName, Integer images) throws IOException {
        return getClient().captureBackground(cameraName, images);
    }

    public String getLastBackground(String cameraName) throws IOException {
        return getClient().getLastBackground(cameraName);
    }

    public List<String> getBackgrounds(String cameraName) throws IOException {
        return getClient().getBackgrounds(cameraName);
    }

    public BufferedImage getLastBackgroundImage(String cameraName) throws IOException {
        return getBackgroundImage(getLastBackground(cameraName));
    }

    public BufferedImage getBackgroundImage(String name) throws IOException {
        return getClient().getBackgroundImage(name);
    }

    void startInstance(String instanceId) throws IOException {
        stop();
        setPush(false);
        String streamUrl = getStream(instanceId);
        try {
            setPush(proxy.isPush(instanceId));
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
        }
        stream = new Stream(getName() + " stream", streamUrl, push);
        setInstance(instanceId);
        try {
            this.pipeline = (String) ((Map) proxy.getInstance(instance).get("config")).get("name");
        } catch (Exception ex) {
            this.pipeline = "Unknown";
        }
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
        setInstance(ret.get(0));
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
        this.pipeline = (String) config.getOrDefault("name", "Unknown");
        setInstance(ret.get(0));
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
            stream.addListener(new DeviceListener() {
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

    public void stop() {
        super.stop();
        pipeline = null;
    }

    /**
     * Return the current instance camera.
     */
    public String getCameraName() throws IOException {
        assertInitialized();
        return getClient().getCameraName(instanceId);
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
        return getClient().getReadonly(instanceId);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getActive() throws IOException {
        assertInitialized();
        return getClient().getActive(instanceId);
    }

    /**
     * Return if the current instance id.
     */
    public String getInstanceId() throws IOException {
        assertInitialized();
        return getClient().getInstanceId(instanceId);
    }

    /**
     * Return if the current instance stream address
     */
    public String getStreamAddress() throws IOException {
        assertInitialized();
        return getClient().getStreamAddress(instanceId);
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
        return getClient().getFunction(instanceId);
    }

    public void setFunction(String function) throws IOException {
        assertInitialized();
        getClient().setFunction(instanceId, function);
    }

    public void sendFunctionScript(String fileName) throws IOException {
        getClient().setScriptFile(fileName);
    }

    public void setFunctionScript(String fileName) throws IOException {
        sendFunctionScript(fileName);
        setFunction(new File(fileName).getName());
    }

    public Stream createSubsampled(double maxFrameRate) throws IOException, InterruptedException {
        return createSubsampled(maxFrameRate, null);
    }

    public Stream createSubsampled(int downsampling) throws IOException, InterruptedException {
        return createSubsampled(null, downsampling);
    }

    protected Stream createSubsampled(Double maxFrameRate, Integer downsampling) throws IOException, InterruptedException {
        Map<String, Object> config = new HashMap<>();
        config.put("name", instance + "_subsampled");
        config.put("input_mode", push ? "PULL" : "SUB");
        config.put("input_pipeline", instanceId);
        config.put("pipeline_type", "stream");
        config.put("mode", "PUB");
        config.put("function", "propagate_stream");
        if (maxFrameRate != null) {
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

    @Override
    protected void doClose() throws IOException {
        configChangeListener = null;
        super.doClose();
    }
}
