package ch.psi.pshell.camserver;

import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.imaging.Utils;
import ch.psi.pshell.utils.EncoderJson;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Client to CameraServer
 */
public class CameraClient extends InstanceManagerClient{
    
    final public static String DEFAULT_URL = "localhost:8888";
    final public static String PREFIX = "cam";
    
    public CameraClient() {
        this(null);
    }
    
    public CameraClient(String url) {
        super((url == null) ? Setup.getCameraServer() : url, PREFIX);
    }

    public CameraClient(String host, int port) {
        super( host, port, PREFIX);
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
    

    public String start(String cameraName) throws IOException {
        return getStream(cameraName);
    }
    
    @Override
    public Map<String, Object> getInstanceConfig(String instanceId) throws IOException {
        return getConfig(instanceId);
    }    

    /**
     * Set instance configuration.
     */
    @Override
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        setInstanceConfig(instanceId, config);
    }    
    
            
    public void resetRoi(String cameraName) throws IOException {
        setConfigValue(cameraName, "roi", null);
    }

    public void setRoi(String cameraName, int x, int y, int width, int height) throws IOException {
        if (x<0) {
            throw new IOException("Invalid ROI x: " + x );
        }        
        if (y<0) {
            throw new IOException("Invalid ROI y: " + y );
        }        
        if ((width<=0) ||(height<=0)){
            throw new IOException("Invalid ROI size: " + width + " x " + height);
        }        
        setConfigValue(cameraName, "roi", new int[]{x, width, y, height});
    }

    public void setRoi(String cameraName, int[] roi) throws IOException {
        setRoi(cameraName, roi[0], roi[1], roi[2], roi[3]);
    }

    public int[] getRoi(String cameraName) throws IOException {
        Object roi = getConfigValue(cameraName, "roi");
        if (roi instanceof List) {
            List<Integer> l = (List<Integer>) roi;
            if (l.size() >= 4) {
                return new int[]{l.get(0), l.get(2), l.get(1), l.get(3)};
            }
        }
        return null;
    }
    
    public void setMirrorX(String cameraName, boolean value) throws IOException {
        setConfigValue(cameraName, "mirror_x", value);
    }

    public boolean getMirrorX(String cameraName) throws IOException {
        Object ret = getConfigValue(cameraName, "mirror_x");
        return Boolean.TRUE.equals(ret);
    }    
    
    public void setMirrorY(String cameraName, boolean value) throws IOException {
        setConfigValue(cameraName, "mirror_y", value);
    }

    public boolean getMirrorY(String cameraName) throws IOException {
        Object ret = getConfigValue(cameraName, "mirror_y");
        return Boolean.TRUE.equals(ret);
    }    

    public void setRotate(String cameraName, int value) throws IOException {
        setConfigValue(cameraName, "rotate", value);
    }

    public int getRotate(String cameraName) throws IOException {
        Object ret = getConfigValue(cameraName, "rotate");
        return (ret instanceof Number n) ? n.intValue(): 0;
    }    

    public void setBackground(String cameraName, String value) throws IOException {
        setConfigValue(cameraName, "image_background", value);
    }

    public void setLatestBackground(String cameraName) throws IOException {
        setConfigValue(cameraName, "image_background", true);
    }

    public String getBackground(String cameraName) throws IOException {
        Object ret = getConfigValue(cameraName, "image_background");
        return (ret == null) ? null : ret.toString();
    }    
    
}
