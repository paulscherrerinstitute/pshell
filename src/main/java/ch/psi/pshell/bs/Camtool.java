package ch.psi.pshell.bs;

import ch.psi.pshell.core.JsonSerializer;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
 * Imaging Source implementation connecting to a Camtool server.
 */
public class Camtool extends StreamCamera {

    final String url;
    Client client;
    final String prefix;
    public static final String SIMULATION = "Simulated";
    String cameraName;

    /**
     * Optional persisted configuration of Camtool objects.
     */
    public static class CamtoolConfig extends ColormapSourceConfig {

        public String serverURL = "localhost:8080";
    }

    public Camtool(String name) {
        this(name, null, new CamtoolConfig());
    }

    public Camtool(String name, String url) {
        this(name, url, null);
    }

    protected Camtool(String name, String url, ColormapSourceConfig cfg) {
        super(name, null, cfg);
        if (cfg instanceof CamtoolConfig) {
            url = ((CamtoolConfig) cfg).serverURL;
        }
        url = (url == null) ? "localhost:8080" : url;
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        this.url = url;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        //In order to be able to delete an entity
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        client = ClientBuilder.newClient(config);
        prefix = getUrl() + "/api/v1/";
    }

    public Camtool(String name, String host, int port) {
        this(name, "http://" + host + ":" + port);
    }

    public String getUrl() {
        return url;
    }

    public String getCurrentCamera() {
        return cameraName;
    }

    public List<String> getCameras() throws IOException {
        WebTarget resource = client.target(prefix + "camera");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, List<String>> map = (Map) JsonSerializer.decode(json, Map.class);
        return map.values().iterator().next();
    }

    public Map<String, Object> getConfig(String cameraName) throws IOException {
        if (cameraName == null) {
            throw new IOException("Invalid camera name");
        }
        WebTarget resource = client.target(prefix + "camera/" + cameraName);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Map<String, Object>> map = (Map) JsonSerializer.decode(json, Map.class);
        return map.values().iterator().next();
    }

    public Rectangle getGeometry(String cameraName) throws IOException {
        if (cameraName == null) {
            throw new IOException("Invalid camera name");
        }
        WebTarget resource = client.target(prefix + "camera/" + cameraName + "/geometry");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        int x = ((Number) map.get("offset_x")).intValue();
        int y = ((Number) map.get("offset_y")).intValue();
        int width = ((Number) map.get("width")).intValue();
        int height = ((Number) map.get("height")).intValue();
        return new Rectangle(x, y, width, height);
    }

    public void setConfig(String cameraName, Map<String, Object> config) throws IOException {
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "camera/" + cameraName);
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        if (r.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IOException("Error updating camera config: " + json);
        }
    }

    public void setCalibration(String cameraName, Map<String, Object> calibration) throws IOException {
        HashMap config = new HashMap();
        config.put("calibration", calibration);
        setConfig(cameraName, config);
    }

    public Map<String, Object> getCalibration(String cameraName) throws IOException {
        Map<String, Object> config = getConfig(cameraName);
        return (Map<String, Object>) config.get("calibration");
    }

    public List<String> getInstances() throws IOException {
        WebTarget resource = client.target(prefix + "instance");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        //Map<String, List<String>> map  = (Map) JsonSerializer.decode(json, Map.class);
        //return map.values().iterator().next();
        List<String> list = (List) JsonSerializer.decode(json, List.class);
        return list;
    }

    public Map<String, Object> getInstance(String instanceName) throws IOException {
        WebTarget resource = client.target(prefix + "instance/" + instanceName);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        return map;
    }

    protected Map<String, Object> createInstance(String cameraName, String operation, Integer numberOfImages, Map<String, Object> pars) throws IOException {
        if (cameraName == null) {
            throw new IOException("Invalid camera name");
        }
        HashMap config = new HashMap();
        config.put("camera_name", cameraName);
        config.put("operation", operation);
        if (numberOfImages != null) {
            config.put("number_of_images", numberOfImages);
        }
        if (pars != null) {
            config.put("parameter", pars);
        }
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "instance");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        if (r.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IOException("Error creating instance: " + json);
        }
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        return map;
    }

    protected void updateInstance(String name, Map<String, Object> pars) throws IOException {
        HashMap config = new HashMap();
        if (pars != null) {
            config.put("parameter", pars);
        }
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "instance/" + name);
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        if (r.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IOException("Error updating instance: " + json);
        }
    }

    public void waitInstance(String instanceName) throws IOException {
        if (instanceName == null) {
            throw new IOException("Invalid instance name");
        }
        WebTarget resource = client.target(prefix + "instance/" + instanceName + "/wait");
        resource.request().accept(MediaType.TEXT_HTML).get();
    }

    public void deleteInstance(String instanceName) throws IOException {
        if (instanceName == null) {
            throw new IOException("Invalid instance name");
        }
        WebTarget resource = client.target(prefix + "instance/" + instanceName);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
    }

    public void grabBackground(String cameraName, Integer numberOfImages) throws IOException {
        Map<String, Object> ret = createInstance(cameraName, "background", numberOfImages, new HashMap<>());
        String instanceName = String.valueOf(ret.keySet().toArray()[0]);
        try {
            waitInstance(instanceName);
        } finally {
            try {
                deleteInstance(instanceName);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    public void startPipeline(String cameraName) throws IOException {
        startPipeline(cameraName, 0);
    }

    public void startPipeline(String cameraName, Integer numberOfImages) throws IOException {
        startPipeline(cameraName, numberOfImages, null, null, null, null);
    }

    public void startPipeline(String cameraName, Integer numberOfImages,
            Boolean backgroundSubtraction,
            List<Integer> regionOfInterest,
            Double threshold,
            Map<String, Object> goodRegion
    ) throws IOException {
        HashMap pars = new HashMap<>();
        if (backgroundSubtraction != null) {
            pars.put("background_subtraction", backgroundSubtraction);
        }
        if (regionOfInterest != null) {
            pars.put("region_of_interest", regionOfInterest);
        }
        if (threshold != null) {
            pars.put("threshold", threshold);
        }
        if (goodRegion != null) {
            pars.put("good_region", goodRegion);
        }

        startPipeline(cameraName, numberOfImages, pars);
    }

    public void startPipeline(String cameraName, Integer numberOfImages, Map<String, Object> pars) throws IOException {
        stopPipeline();
        String operation = SIMULATION.equals(cameraName) ? "simulation" : "pipeline";
        Map<String, Object> ret = createInstance(cameraName, operation, numberOfImages, pars);
        this.cameraName = cameraName;
        pipelineInstanceName = (String) ret.get("instance_id");
        setStreamSocket((String) ret.get("stream"));
    }

    public void updatePipeline(Map<String, Object> pars) throws IOException {
        if (pipelineInstanceName == null) {
            throw new IOException("Pipeline not started");
        }
        updateInstance(pipelineInstanceName, pars);
    }

    public Map<String, Object> getPipelinePars() throws IOException {
        if (pipelineInstanceName == null) {
            throw new IOException("Pipeline not started");
        }
        Map<String, Object> ret = getInstance(pipelineInstanceName);
        try {
            return (Map<String, Object>) ret.get("parameter");
        } catch (Exception ex) {
            throw new IOException("Error reading pipeline parameters");
        }

    }

    String pipelineInstanceName;

    public void stopPipeline() {
        if (pipelineInstanceName != null) {
            try {
                deleteInstance(pipelineInstanceName);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            } finally {
                pipelineInstanceName = null;
            }
        }
        cameraName = null;
    }

    public boolean isPipelineStarted() {
        return (pipelineInstanceName != null);
    }

    public boolean isRoiEnabled() throws IOException {
        Map<String, Object> pars = getPipelinePars();
        return pars.get("region_of_interest") != null;
    }

    public void resetRoi() throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("region_of_interest", null);
        updatePipeline(pars);
    }

    public void setRoi(int x, int y, int width, int height) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("region_of_interest", new int[]{x, width, y, height});
        updatePipeline(pars);
    }

    public void setRoi(int[] roi) throws IOException {
        setRoi(roi[0], roi[1], roi[2], roi[3]);
    }

    public int[] getRoi() throws IOException {
        Map<String, Object> pars = getPipelinePars();
        Object roi = pars.get("region_of_interest");
        if ((roi != null) && (roi instanceof List)) {
            List<Integer> l = (List<Integer>) roi;
            if (l.size() >= 4) {
                return new int[]{l.get(0), l.get(2), l.get(1), l.get(3)};
            }
        }
        return null;
    }

    public boolean getBackgroundSubtraction() throws IOException {
        Map<String, Object> pars = getPipelinePars();
        return Boolean.TRUE.equals(pars.get("background_subtraction"));
    }

    public void setBackgroundSubtraction(boolean value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("background_subtraction", value);
        updatePipeline(pars);
    }

    public Double getThreshold() throws IOException {
        Map<String, Object> pars = getPipelinePars();
        Object ret = pars.get("threshold");
        return ((ret != null) && (ret instanceof Number)) ? ((Number) ret).doubleValue() : null;
    }

    public void setThreshold(Double value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("threshold", value);
        updatePipeline(pars);
    }

    public Map<String, Object> getGoodRegion() throws IOException {
        Map<String, Object> pars = getPipelinePars();
        Object ret = pars.get("good_region");

        return ((ret != null) && (ret instanceof Map)) ? (Map) ret : null;
    }

    public void setGoodRegion(Map<String, Object> value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("good_region", value);
        updatePipeline(pars);
    }

    public void setGoodRegion(double threshold, double scale) throws IOException {
        Map<String, Object> gr = new HashMap<>();
        gr.put("threshold", threshold);
        gr.put("gfscale", scale);
        setGoodRegion(gr);
    }

    public void start(String cameraName) throws IOException {
        stop();
        startPipeline(cameraName);
        startReceiver();
    }

    public void start(String cameraName, Integer numberOfImages) throws IOException {
        stop();
        startPipeline(cameraName, numberOfImages);
        startReceiver();
    }

    public void start(String cameraName, Integer numberOfImages,
            Boolean backgroundSubtraction,
            List<Integer> regionOfInterest,
            Double threshold,
            Map<String, Object> goodRegion
    ) throws IOException {
        stop();
        startPipeline(cameraName, numberOfImages, backgroundSubtraction, regionOfInterest, threshold, goodRegion);
        startReceiver();
    }

    public void start(String cameraName, Integer numberOfImages, Map<String, Object> pars) throws IOException {
        stop();
        startPipeline(cameraName, numberOfImages, pars);
        startReceiver();
    }

    public void stop() throws IOException {
        stopPipeline();
        stopReceiver();
    }

    //Overridables
    @Override
    protected void doClose() throws IOException {
        stopPipeline();
        super.doClose();
    }

    public static void main(String[] args) throws IOException {
        Camtool c = new Camtool("Camtool", "gfa-lc6-64", 8080);
        List<String> cameras = c.getCameras();
        Map<String, Object> config = c.getConfig("S10BC02-DSCR220");
        //c.grabBackground("S10BC02-DSCR220", 2);

        c.getInstances();
        c.startPipeline("S10BC02-DSCR220", 2);
        c.getInstances();
        c.stopPipeline();

        System.out.println(cameras);
    }
}
