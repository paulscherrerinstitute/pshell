package ch.psi.pshell.camserver;

import ch.psi.pshell.imaging.Utils;
import ch.psi.utils.EncoderJson;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Client to CameraServer
 */
public class CameraClient extends InstanceManagerClient{
    
    final public static String DEFAULT_URL = "localhost:8888";
    final public static String PREFIX = "cam";
    
    public CameraClient(String host, int port) {
        super( host, port, PREFIX);
    }

    public CameraClient(String url) {
        super((url == null) ? DEFAULT_URL : url, PREFIX);
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        WebTarget resource = client.target(prefix);
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (List<String>) map.get("cameras");
    }

    /**
     * Camera aliases.
     */
    public Map<String,String> getCameraAliases() throws IOException {
        WebTarget resource = client.target(prefix+ "/aliases");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (Map<String,String>) map.get("aliases");
    }

    
    /**
     * Return the camera image size.
     */
    public Dimension getGeometry(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/geometry");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        List geometry = (List) map.get("geometry");
        int width = ((Number) geometry.get(0)).intValue();
        int height = ((Number) geometry.get(1)).intValue();
        return new Dimension(width, height);
    }

    public Boolean isOnline(String cameraName) throws IOException {
        checkName(cameraName);
        WebTarget resource = client.target(prefix + "/" + cameraName + "/is_online");
        String json = resource.request().accept(MediaType.TEXT_HTML).get(String.class);
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        Boolean ret = (Boolean) map.get("online");
        return ret;
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
        Map<String, Object> map = (Map) EncoderJson.decode(json, Map.class);
        checkReturn(map);
        return (String) map.get("stream");
    }
    

    public String start(String cameraName) throws IOException {
        return getStream(cameraName);
    }

}
