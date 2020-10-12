package ch.psi.pshell.bs;

import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.imaging.Utils;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
public class CameraServer extends StreamCamera {

    final String url;
    Client client;
    final String prefix;
    String currentName;

    /**
     * Optional persisted configuration of CameraServer objects.
     */
    public static class CameraServerConfig extends ColormapSourceConfig {

        public String serverURL = "localhost:8888";
    }

    public CameraServer(String name) {
        this(name, null, new CameraServerConfig());
    }

    public CameraServer(String name, String url) {
        this(name, url, null);
    }

    public CameraServer(String name, String host, int port) {
        this(name, "http://" + host + ":" + port);
    }

    protected CameraServer(String name, String url, ColormapSourceConfig cfg) {
        super(name, null, cfg);
        if (cfg instanceof CameraServerConfig) {
            url = ((CameraServerConfig) cfg).serverURL;
        }
        url = (url == null) ? "localhost:8888" : url;
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        this.url = url;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        //In order to be able to delete an entity
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        client = ClientBuilder.newClient(config);
        prefix = getUrl() + "/api/v1/cam";
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Return the name of the current streaming camera.
     */
    public String getCurrentName() {
        return currentName;
    }

    void checkReturn(Map<String, Object> ret) throws IOException {
        if (!ret.get("state").equals("ok")) {
            throw new IOException(String.valueOf(ret.get("status")));
        }
    }

    void checkName(String cameraName) throws IOException {
        if (cameraName == null) {
            throw new IOException("Invalid camera name");
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
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("cameras");
    }

    /**
     * Camera aliases.
     */
    public Map<String,String> getCameraAliases() throws IOException {
        WebTarget resource = client.target(prefix+ "/aliases");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String,String>) map.get("aliases");
    }

    /**
     * Camera groups.
     */
    public Map<String,List<String>> getCameraGroups() throws IOException {
        WebTarget resource = client.target(prefix+ "/groups");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String,List<String>>) map.get("groups");
    }

    /**
     * Return the camera configuration.
     */
    public Map<String, Object> getConfig(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }

    /**
     * Set the camera configuration.
     */
    public void setConfig(String cameraName, Map<String, Object> config) throws IOException {
        checkName(cameraName);
        String json = JsonSerializer.encode(config);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Delete the camera configuration.
     */
    public void deleteConfig(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Return the camera image size.
     */
    public Dimension getGeometry(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/geometry");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        List geometry = (List) map.get("geometry");
        int width = ((Number) geometry.get(0)).intValue();
        int height = ((Number) geometry.get(1)).intValue();
        return new Dimension(width, height);
    }

    /**
     * Return the camera image in PNG format.
     */
    public BufferedImage getImage(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/image");
        byte[] ret = resource.request().accept(MediaType.APPLICATION_OCTET_STREAM).get(byte[].class);
        return Utils.newImage(ret);
    }

    /**
     * Get the camera stream address.
     */
    public String getStream(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

    /**
     * Stop the camera.
     */
    public void stopCamera(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Stop all the cameras in the server.
     */
    public void stopAllCameras() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) JsonSerializer.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Start camera streaming and set the stream endpoint to the current stream socket.
     */
    public void start(String cameraName) throws IOException {
        stop();
        setStreamSocket(getStream(cameraName));
        this.currentName = cameraName;
        startReceiver();
    }

    /**
     * Stop receiving from current camera.
     */
    public void stop() throws IOException {
        stopReceiver();
    }
}
