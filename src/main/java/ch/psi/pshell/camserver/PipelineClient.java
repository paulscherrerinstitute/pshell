package ch.psi.pshell.camserver;

import ch.psi.pshell.imaging.Utils;
import ch.psi.utils.EncoderJson;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;

/**
 * Client to PipelineServer
 */
public class PipelineClient extends InstanceManagerClient{
    
    final public static String DEFAULT_URL = "localhost:8889";
    final public static String PREFIX = "pipeline";

    public PipelineClient(String host, int port) {
        super( host, port, PREFIX);
    }

    public PipelineClient(String url) {
        super((url == null) ? DEFAULT_URL : url, PREFIX);
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        WebTarget resource = client.target(prefix + "/camera");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("cameras");
    }

    /**
     * List existing pipelines.
     */
    public List<String> getPipelines() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
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
     * Return the instance configuration.
     */
    @Override
    public Map<String, Object> getInstanceConfig(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/config");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("config");
    }
    /**
     * Set instance configuration.
     */
    @Override
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        checkName(instanceId);
        String json = EncoderJson.encode(config, false);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }

    /**
     * Return the instance info.
     */
    public Map<String, Object> getInstanceInfo(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId + "/info");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String, Object>) map.get("info");
    }

    /**
     * Return the instance stream. If the instance does not exist, it will be created and will be
     * read only - no config changes will be allowed. If instanceId then return existing (writable).
     */
    @Override
    public String getStream(String instanceId) throws IOException {
        checkName(instanceId);
        WebTarget resource = client.target(prefix + "/instance/" + instanceId);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }

    /**
     * Create a pipeline from a config file. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromName(String pipelineName, String id) throws IOException {
        return createFromName(pipelineName, id, null);
    }

    public List<String> createFromName(String pipelineName, String id, Map<String, Object> additionalConfig) throws IOException {
        checkName(pipelineName);
        WebTarget resource = client.target(prefix + "/" + pipelineName);
        if (id != null) {
            resource = resource.queryParam("instance_id", id);
        }
        if (additionalConfig != null) {
            String json = EncoderJson.encode(additionalConfig, false);
            
            resource = resource.queryParam("additional_config", URLEncoder.encode(json, "UTF-8"));
        }
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(null);
        String json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return Arrays.asList(new String[]{(String) map.get("instance_id"), (String) map.get("stream")});
    }

    public List<String> createReadonlyFromName(String pipelineName, String id) throws IOException {
        Map<String, Object> additionalConfig = new HashMap<>();
        additionalConfig.put("read_only", true);
        return createFromName(pipelineName, id, additionalConfig);
    }

    
    /**
     * Create a pipeline from the provided config. Pipeline config can be changed. Return pipeline
     * instance id and instance stream in a list.
     */
    public List<String> createFromConfig(Map<String, Object> config, String id) throws IOException {
        String json = EncoderJson.encode(config, false);
        WebTarget resource = client.target(prefix);
        if (id != null) {
            resource = resource.queryParam("instance_id", id);
        }
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return Arrays.asList(new String[]{(String) map.get("instance_id"), (String) map.get("stream")});
    }

    public List<String> createReadonlyFromConfig(Map<String, Object> config, String id) throws IOException {
        config.put("read_only", true);
        return createFromConfig(config, id);
    }
    
    /**
     * Set config of the pipeline. Return actual applied config.
     */
    public String savePipelineConfig(String pipelineName, Map<String, Object> config) throws IOException {
        checkName(pipelineName);
        String json = EncoderJson.encode(config, false);
        WebTarget resource = client.target(prefix + "/" + pipelineName + "/config");
        Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
        json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
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
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("background_id");
    }

    public String getLastBackground(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/camera/" + cameraName + "/background");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("background_id");
    }
    
    public List<String> getBackgrounds(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/camera/" + cameraName + "/backgrounds");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List) map.get("background_ids");
    }    
    
    public BufferedImage getLastBackgroundImage(String cameraName) throws IOException {
        return getBackgroundImage(getLastBackground(cameraName));
    }
    
    public BufferedImage getBackgroundImage(String name) throws IOException {
        WebTarget resource = client.target(prefix + "/background/" + name + "/image");
        byte[] ret = resource.request().accept(MediaType.APPLICATION_OCTET_STREAM).get(byte[].class);
        return Utils.newImage(ret);
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

    List<String> start(String pipelineName, boolean shared, String instanceId) throws IOException {
        if (shared) {            
            String url = getStream(pipelineName);
            return Arrays.asList(new String[]{pipelineName, url});
        } else {
            return createFromName(pipelineName, instanceId);
        }
    }

    /**
     * Return if the instance is readonly.
     */
    public Boolean getReadonly(String instanceId) throws IOException {
        return (Boolean) getInstanceInfo(instanceId).get("read_only");
    }

    /**
     * Return if the current instance is readonly.
     */
    public Boolean getActive(String instanceId) throws IOException {
        return (Boolean) getInstanceInfo(instanceId).get("is_stream_active");
    }

    /**
     * Return if the current instance id.
     */
    public String getInstanceId(String instanceId) throws IOException {
        return (String) getInstanceInfo(instanceId).get("instance_id");
    }

    /**
     * Return if the current instance stream address
     */
    public String getStreamAddress(String instanceId) throws IOException {
        return (String) getInstanceInfo(instanceId).get("stream_address");
    }

    /**
     * Return the current instance camera.
     */
    public String getCameraName(String instanceId) throws IOException {
        return (String) getInstanceInfo(instanceId).get("camera_name");
    }

    
    /**
     * Scripts
     */
    public List<String> getScripts() throws IOException {
        WebTarget resource = client.target(prefix+ "/script");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("scripts");
    }
    
    public String getScript(String name) throws IOException {
        WebTarget resource = client.target(prefix+ "/script/" + name + "/script_bytes");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("script");
    }
    
    public void setScript(String name, String script) throws IOException {
        WebTarget resource = client.target(prefix+ "/script/" + name + "/script_bytes");
        Response r = resource.request().accept(MediaType.TEXT_HTML).put(Entity.text(script));
        String json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);   
    }               

    public void deleteScript(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix+ "/script/" + name + "/script_bytes");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }   
    
    public void setScriptFile(String fileName) throws IOException {
        File file = new File(fileName);
        String script = new String(Files.readAllBytes(file.toPath()));
        String name = file.getName();
        setScript(name, script);
    }     
    
    public String getFunction(String instanceId) throws IOException {
        Object ret = getInstanceConfigValue(instanceId, "function");
        return ((ret != null) && (ret instanceof String)) ? (String) ret : null;
    }

    public void setFunction(String instanceId, String function) throws IOException {
        Map<String, Object> pars = new HashMap();
        pars.put("function", function);
        pars.put("reload", true);
        setInstanceConfig(instanceId, pars);
    }     
    
    
    public void setFunctionScript(String instanceId, String fileName) throws IOException {
        setScriptFile(fileName);
        setFunction(instanceId, new File(fileName).getName());
    }
        
    
    public List<String> getLibs() throws IOException {
        WebTarget resource = client.target(prefix+ "/lib");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("libs");
    }
    
    public byte[] getLib(String name) throws IOException {
        WebTarget resource = client.target(prefix+ "/lib/" + name + "/lib_bytes");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        Boolean base64 = (Boolean) map.get("base64");
        String ret = (String)map.get("lib");
        if (base64){
            return  Base64.getDecoder().decode(ret);
        }        
        return ((String) ret).getBytes("UTF-8");
    }
    
    public void setLib(String name, byte[] lib) throws IOException {
        WebTarget resource = client.target(prefix+ "/lib/" + name + "/lib_bytes");
        Response r = resource.request().accept(MediaType.TEXT_HTML).put(Entity.text(lib));
        String json = r.readEntity(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);   
    }               

    public void deleteLib(String name) throws IOException {
        checkName(name);
        WebTarget resource = client.target(prefix+ "/lib/" + name + "/lib_bytes");
        String json = resource.request().accept(MediaType.TEXT_HTML).delete(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
    }   
    
    public void setLibFile(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] lib = Files.readAllBytes(file.toPath());
        String name = file.getName();
        setLib(name, lib);
    }     
    
}
