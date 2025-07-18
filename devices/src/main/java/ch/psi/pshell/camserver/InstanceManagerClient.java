package ch.psi.pshell.camserver;

import ch.psi.pshell.utils.EncoderJson;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Client to CamServer services
 */
public abstract class InstanceManagerClient extends CamServerClient {

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
     * Groups.
     */
    public Map<String, List<String>> getGroups() throws IOException {
        WebTarget resource = client.target(prefix + "/groups");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, List<String>>) map.get("groups");
    }

    /**
     * Get the stream address.
     */
    public String getStream(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

    
    
    /**
     * Return the instance configuration.
     */
    abstract public Map<String, Object> getInstanceConfig(String instanceId) throws IOException;
    
    public Object getInstanceConfigValue(String instanceId, String value) throws IOException {
        Map<String, Object> pars = getInstanceConfig(instanceId);
        return pars.get(value);
    }    

    /**
     * Set instance configuration.
     */
    abstract public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException;

    public void setInstanceConfigValue(String instanceId, String name, Object value) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put(name, value);
        setInstanceConfig(instanceId, pars);
    }    
}
