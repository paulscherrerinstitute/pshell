package ch.psi.pshell.camserver;

import ch.psi.utils.EncoderJson;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class InstanceManagerClient extends CamServerClient {

    public InstanceManagerClient(String host, int port, String prefix) {
        super(host, port, prefix);
    }

    public InstanceManagerClient(String url, String prefix) {
        super(url, prefix);
    }

    /**
     * Stop instance.
     */
    public void stopInstance(String instanceName) throws IOException {
        checkName(instanceName);
        WebTarget resource = client.target(prefix + "/" + instanceName);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Stop all the instances in the server.
     */
    public void stopAllInstances() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }
    
    /**
     * Stop and delete instance.
     */
    public void deleteInstance(String instanceName) throws IOException {
        checkName(instanceName);
        WebTarget resource = client.target(prefix + "/" + instanceName + "/del");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }    

    public boolean isInstanceRunning(String instanceName) throws IOException {
        checkName(instanceName);
        return ((List) getInfo().get("active_instances")).contains(instanceName);
    }

    /**
     * Return the configuration.
     */
    public Map<String, Object> getConfig(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }

    /**
     * Set configuration
     */
    public void setConfig(String name, Map<String, Object> config) throws IOException {
        checkName(name);
        String json = EncoderJson.encode(config, true);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }

    public List<String> getConfigNames() throws IOException {
        WebTarget resource = client.target(prefix + "/config_names");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("config_names");
    }

    /**
     * Delete configuration
     */
    public void deleteConfig(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Camera groups.
     */
    public Map<String, List<String>> getGroups() throws IOException {
        WebTarget resource = client.target(prefix + "/groups");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, List<String>>) map.get("groups");
    }

    /**
     * Get the camera stream address.
     */
    public String getStream(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

}
