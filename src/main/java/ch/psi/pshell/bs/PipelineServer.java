package ch.psi.pshell.bs;

import ch.psi.pshell.core.JsonSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class PipelineServer extends StreamCamera {

    final String url;
    Client client;
    final String prefix;
    String currentPipeline;
    String currentInstance;
    Boolean shared;

    /**
     * Optional persisted configuration of CameraServer objects.
     */
    public static class PipelineServerConfig extends ColormapSourceConfig {

        public String serverURL = "localhost:8889";
    }

    public PipelineServer(String name) {
        this(name, null, new PipelineServerConfig());
    }

    public PipelineServer(String name, String url) {
        this(name, url, null);
    }

    public PipelineServer(String name, String host, int port) {
        this(name, "http://" + host + ":" + port);
    }

    protected PipelineServer(String name, String url, ColormapSourceConfig cfg) {
        super(name, null, cfg);
        if (cfg instanceof PipelineServerConfig) {
            url = ((PipelineServerConfig) cfg).serverURL;
        }
        url = (url == null) ? "localhost:8889" : url;
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        this.url = url;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        //In order to be able to delete an entity
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        client = ClientBuilder.newClient(config);
        prefix = getUrl() + "/api/v1/pipeline";
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Return the name of the current streaming pipeline.
     */
    public String getCurrentPipeline() {
        return currentPipeline;
    }

    /**
     * Return the name of the current streaming pipeline.
     */
    public String getCurrentCamera() {
        try {
            return String.valueOf(getInstanceInfo().get("camera_name"));
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Return the name of the current streaming pipeline.
     */
    public String getCurrentInstance() {
        return currentInstance;
    }

    void checkReturn(Map<String, Object> ret) throws IOException {
        if (!ret.get("state").equals("ok")) {
            throw new IOException(String.valueOf(ret.get("status")));
        }
    }

    void checkName(String pipelineName) throws IOException {
        if (pipelineName == null) {
            throw new IOException("Invalid camera name");
        }
    }

    void assertStarted() throws IOException {
        if (!isStarted()) {
            throw new IOException("Pipeline not started");
        }
    }

    /**
     * Return the info of the cam server instance.
     */
    public Map<String, Object> getInfo() throws IOException {
        WebTarget resource = client.target(prefix + "/info");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("info");
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        WebTarget resource = client.target(prefix + "/camera");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("cameras");
    }

    /**
     * List existing pipelines.
     */
    public List<String> getPipelines() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("pipelines");
    }

    /**
     * List running instances.
     */
    public List<String> getInstances() throws IOException {
        Map<String, Object> info = getInfo();
        Map<String, Object> instances = (Map<String, Object>) info.get("active_instances");
        return new ArrayList(instances.keySet());
    }

    /**
     * Return the pipeline configuration.
     */
    public Map<String, Object> getConfig(String pipelineName) throws IOException {
        checkName(pipelineName);
        WebTarget resource = client.target(prefix + "/" + pipelineName + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }

    /**
     * Set configuration of the pipeline.
     */
    public void setConfig(String pipelineName, Map<String, Object> config) throws IOException {
        checkName(pipelineName);
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "/" + pipelineName + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Delete configuration of the pipeline.
     */
    public void deleteConfig(String pipelineName) throws IOException {
        checkName(pipelineName);
        WebTarget resource = client.target(prefix + "/" + pipelineName + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Return the instance configuration.
     */
    public Map<String, Object> getInstanceConfig(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }

    /**
     * Set instance configuration.
     */
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        checkName(instanceId);
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Return the instance info.
     */
    public Map<String, Object> getInstanceInfo(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/info");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("info");
    }

    /**
     * Return the instance stream. If the instance does not exist, it will be created and will be
     * read only - no config changes will be allowed. If instanceId then return existing (writable).
     */
    public String getStream(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

    /**
     * Create a pipeline from a config file. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromName(String pipelineName, String id) throws IOException {
        checkName(pipelineName);
        WebTarget resource = client.target(prefix + "/" + pipelineName);
        if (id != null) {
            resource = resource.queryParam("instance_id", id);
        }
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(null);
        String json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return Arrays.asList(new String[]{(String) map.get("instance_id"), (String) map.get("stream")});
    }

    /**
     * Create a pipeline from the provided config. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromConfig(Map<String, Object> config, String id) throws IOException {
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix);
        if (id != null) {
            resource = resource.queryParam("instance_id", id);
        }
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return Arrays.asList(new String[]{(String) map.get("instance_id"), (String) map.get("stream")});
    }

    /**
     * Set config of the pipeline. Return actual applied config.
     */
    public String savePipelineConfig(String pipelineName, Map<String, Object> config) throws IOException {
        checkName(pipelineName);
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "/" + pipelineName + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

    /**
     * Stop the pipeline.
     */
    public void stopInstance(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/" + instanceId);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Stop all the pipelines on the server.
     */
    public void stopAllInstances() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Collect the background image on the selected camera. Return background id.
     */
    public String captureBackground(String cameraName, Integer images) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/camera/" + cameraName + "/background");
        if (images != null) {
            resource = resource.queryParam("n_images", images);
        }
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(null);
        String json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("background_id");
    }

    public String getLastBackground(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/camera/" + cameraName + "/background");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("background_id");
    }

    /**
     * Start pipeline streaming, creating a private instance, and set the stream endpoint to the
     * current stream socket.
     */
    public void start(String pipelineName) throws IOException {
        start(pipelineName, false);
    }

    /**
     * Start pipeline streaming, and set the stream endpoint to the current stream socket. If shared
     * is true, start the read-only shared instance of the pipeline.
     */
    public void start(String pipelineName, boolean shared) throws IOException {
        start(pipelineName, shared, null);
    }

    /**
     * Start pipeline streaming, using a shared instance called instanceId. If instance id different
     * than the pipeline name, instance is not readonly.
     */
    public void start(String pipelineName, String instanceId) throws IOException {
        if (!getInstances().contains(instanceId)) {
            start(pipelineName, false, instanceId);
        } else {
            start(instanceId, true, null);
        }
    }

    void start(String pipelineName, boolean shared, String instanceId) throws IOException {
        stop();
        if (shared) {
            String stream = getStream(pipelineName);
            setStreamSocket(stream);
            this.currentPipeline = pipelineName;
            this.currentInstance = pipelineName;
        } else {
            List<String> ret = createFromName(pipelineName, instanceId);
            setStreamSocket(ret.get(1));
            this.currentPipeline = pipelineName;
            this.currentInstance = ret.get(0);
        }
        this.shared = shared;
        startReceiver();
    }

    /**
     * Stop receiving from current pipeline.
     */
    public void stop() throws IOException {
        this.currentPipeline = null;
        this.currentInstance = null;
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
        return (Map) JsonSerializer.decode(val.toString(), Map.class);
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getReadonly() throws IOException {
        assertStarted();
        return (Boolean) getInstanceInfo().get("read_only");
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getActive() throws IOException {
        return (Boolean) getInstanceInfo().get("is_stream_active");
    }

    /**
     * Return if the current instance id.
     */
    public String getInstanceId() throws IOException {
        return (String) getInstanceInfo().get("instance_id");
    }

    /**
     * Return if the current instance stream address
     */
    public String getStreamAddress() throws IOException {
        return (String) getInstanceInfo().get("stream_address");
    }

    /**
     * Return the current instance camera.
     */
    public String getCameraName() throws IOException {
        return (String) getInstanceInfo().get("camera_name");
    }

    /**
     * Set current instance configuration.
     */
    public void setInstanceConfig(Map<String, Object> config) throws IOException {
        assertStarted();
        setInstanceConfig(currentInstance, config);
    }

    /**
     * Return the current instance info.
     */
    public Map<String, Object> getInstanceConfig() throws IOException {
        assertStarted();
        return getInstanceConfig(currentInstance);
    }

    public boolean isRoiEnabled() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        List roi = (List) pars.get("image_region_of_interest");
        return (roi != null) && (roi.size() == 4);
    }

    public void resetRoi() throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_region_of_interest", null);
        setInstanceConfig(pars);
    }

    public void setRoi(int x, int y, int width, int height) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_region_of_interest", new int[]{x, width, y, height});
        setInstanceConfig(pars);
    }

    public void setRoi(int[] roi) throws IOException {
        setRoi(roi[0], roi[1], roi[2], roi[3]);
    }

    public int[] getRoi() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        List roi = (List) pars.get("image_region_of_interest");
        if ((roi != null) && (roi instanceof List)) {
            List<Integer> l = (List<Integer>) roi;
            if (l.size() >= 4) {
                return new int[]{l.get(0), l.get(2), l.get(1), l.get(3)};
            }
        }
        return null;
    }

    public String getBackground() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        return (String) pars.get("image_background");
    }

    public void setBackground(String id) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_background", id);
        setInstanceConfig(pars);
    }

    public String getLastBackground() throws IOException {
        assertStarted();
        return getLastBackground(getCurrentCamera());
    }

    public String captureBackground(Integer images) throws IOException {
        assertStarted();
        String id = captureBackground(getCurrentCamera(), images);
        //If capturing background of current camera, sets is as vurrent background
        if (id != null) {
            setBackground(id);
        }
        return id;
    }

    public boolean getBackgroundSubtraction() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        return Boolean.TRUE.equals(pars.get("image_background_enable"));
    }

    public void setBackgroundSubtraction(boolean value) throws IOException {
        Map<String, Object> pars = new HashMap();
        if (value) {
            String id = getBackground();
            if (id == null) {
                setBackground(getLastBackground());
            }
        }
        pars.put("image_background_enable", value);
        setInstanceConfig(pars);
    }

    public Double getThreshold() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        Object ret = pars.get("image_threshold");
        return ((ret != null) && (ret instanceof Number)) ? ((Number) ret).doubleValue() : null;
    }

    public void setThreshold(Double value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_threshold", value);
        setInstanceConfig(pars);
    }

    public Map<String, Object> getGoodRegion() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        Object ret = pars.get("image_good_region");
        return ((ret != null) && (ret instanceof Map)) ? (Map) ret : null;
    }

    public void setGoodRegion(Map<String, Object> value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_good_region", value);
        setInstanceConfig(pars);
    }

    public void setGoodRegion(double threshold, double scale) throws IOException {
        Map<String, Object> gr = new HashMap<>();
        gr.put("threshold", threshold);
        gr.put("gfscale", scale);
        setGoodRegion(gr);
    }

    public Map<String, Object> getSlicing() throws IOException {
        Map<String, Object> pars = getInstanceConfig();
        Object ret = pars.get("image_slices");
        return ((ret != null) && (ret instanceof Map)) ? (Map) ret : null;
    }

    public void setSlicing(Map<String, Object> value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("image_slices", value);
        setInstanceConfig(pars);
    }

    public void setSlicing(int slices, double scale, String orientation) throws IOException {
        Map<String, Object> gr = new HashMap<>();
        gr.put("number_of_slices", slices);
        gr.put("scale", scale);
        gr.put("orientation", orientation);
        setSlicing(gr);
    }
        
    /**
     * Return if the current instance is a shared connection.
     */
    public Boolean getShared() throws IOException {
        assertStarted();
        return shared;
    }

    public boolean isStarted() {
        return (currentInstance != null);
    }

}
