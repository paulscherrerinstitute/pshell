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
 * Client to a CamServer cluster manager (Proxy)
 */
public class ProxyClient extends CamServerClient{
   
    final public static String DEFAULT_URL = "localhost:8889";
    final public static String PREFIX = "proxy";
    
    public ProxyClient(String host, int port) {
        super( host, port, PREFIX);
    }

    public ProxyClient(String url) {
        super((url == null) ? DEFAULT_URL : url, PREFIX);
    }
    
    public Map<String, Map<String, Object>> getServers() throws IOException {
        return (Map<String, Map<String, Object>>) getInfo().get("servers");
    }
    
    public Map<String, Map<String, Object>> getInstances() throws IOException {
        return (Map<String, Map<String, Object>>) getInfo().get("active_instances");
    }
    
    public Map getInstance(String instance) throws IOException{
        Map<String, Map<String, Object>> active_instances = getInstances();        
        return active_instances.getOrDefault(instance, null);        
    }     
    
    
    public List<String> getConfigNames() throws IOException {
        WebTarget resource = client.target(prefix+ "/config_names");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("config_names");        
    }  
    
    /**
     * Return the configuration.
     */
    public Map<String, Object> getConfig() throws IOException {
        WebTarget resource = client.target(prefix + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }
    
    
    /**
     * Return the configuration.
     */
    public String getNamedConfig(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("config");
    }
    
    /**
     * Set configuration 
     */
    public void setNamedConfig(String name, String config) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        Map<String, Object> map = (Map) EncoderJson.decode(config, Map.class); //Check if serializable
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(config));
        String json = json = r.readEntity(String.class);
        map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }
    
    public void deleteNamedConfig(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix + "/" + name + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }    
    
    
    /**
     * Set configuration 
     */
    public void setConfig(Map<String, Object> config) throws IOException {
        String json = EncoderJson.encode(config, true);
        WebTarget resource = client.target(prefix + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }    
    
    
    /**
     * Stop  instance.
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
    public void stopAllInstances(int serverIndex) throws IOException {
        WebTarget resource = client.target(prefix+ "/server/" + serverIndex);
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }    
    
    public void stopAllInstances(String server) throws IOException {
        String compactName = server.replace("http", "").replace("/", "").replace(":", "");
        WebTarget resource = client.target(prefix+ "/server/" + compactName);
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
    
    
    /**
     * Return the configuration.
     */
    public Map<String, String> getPemanentInstances() throws IOException {
        WebTarget resource = client.target(prefix + "/permanent");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, String>) map.get("permanent_instances");
    }
        
    /**
     * Set configuration 
     */
    public void setPemanentInstances(Map<String, String> config) throws IOException {
        String json = EncoderJson.encode(config, false);
        WebTarget resource = client.target(prefix + "/permanent");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }    
    
     /**
     * Get server logs
     */
    public String getLogs(int serverIndex) throws IOException {
        WebTarget resource = client.target(prefix+ "/server/logs/" + serverIndex + "/txt");
        String logs = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        return logs;
    }    
    
    public String getLogs(String server) throws IOException {
        String compactName = server.replace("http", "").replace("/", "").replace(":", "");        
        WebTarget resource = client.target(prefix+ "/server/logs/" + compactName + "/txt");
        String logs = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        return logs;
    }    

    public String getLogs(int serverIndex, String instance) throws IOException {
        WebTarget resource = client.target(prefix+ "/server/instance/logs/" + serverIndex + "/" + instance +"/txt");
        String logs = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        return logs;
    }    

    public String getLogs(String server, String instance) throws IOException {
        String compactName = server.replace("http", "").replace("/", "").replace(":", "");        
        WebTarget resource = client.target(prefix+ "/server/instance/logs/" + compactName + "/" + instance+ "/txt");
        String logs = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        return logs;
    }    
    
    public static boolean isPush(Map instanceData){
        if (instanceData==null){
            return false;
        }
        Map cfg = (Map) instanceData.getOrDefault("config", new HashMap());    
        if (cfg.getOrDefault("pipeline_type", "").equals("store")){
            return true;
        }
        if (cfg.getOrDefault("mode", "").equals("PUSH")){
            return true;
        }
        if (cfg.getOrDefault("mode", "").equals("PULL")){
            return false;
        }
        if (cfg.getOrDefault("pipeline_type", "").equals("fanout")){
            return true;
        }
        return false;
    }
    
    boolean isPush(String instance) throws IOException{
        return isPush(getInstance(instance));
    }    

}
