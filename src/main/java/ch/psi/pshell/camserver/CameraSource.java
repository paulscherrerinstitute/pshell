package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.StreamCamera;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class CameraSource extends StreamCamera {

    String currentName;
    final CameraClient client;
    
    public CameraClient getClient(){
        return client;
    }
        
    
    /**
     * Optional persisted configuration of CameraServer objects.
     */
    public static class CameraServerConfig extends ColormapSourceConfig {
        public String serverURL = CameraClient.DEFAULT_URL;
    }

    public CameraSource(String name) {
        this(name, null, new CameraServerConfig());
    }

    public CameraSource(String name, String url) {
        this(name, url, null);
    }

    public CameraSource(String name, String host, int port) {
        this(name, "http://" + host + ":" + port);
    }

    protected CameraSource(String name, String url, ColormapSourceConfig cfg) {
        super(name, null, cfg);
        if (cfg instanceof CameraServerConfig) {
            url = ((CameraServerConfig) cfg).serverURL;
        }
        client = new CameraClient(url);
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return client.getUrl();
    }

    /**
     * Return the name of the current streaming camera.
     */
    public String getCurrentName() {
        return currentName;
    }

    /**
     * Return the info of the cam server instance.
     */
    public Map<String, Object> getInfo() throws IOException {
        return client.getInfo();
    }

    /**
     * List existing cameras.
     */
    public List<String> getCameras() throws IOException {
        return client.getCameras();
    }

    /**
     * Camera aliases.
     */
    public Map<String,String> getCameraAliases() throws IOException {
        return client.getCameraAliases();
    }

    /**
     * Camera groups.
     */
    public Map<String,List<String>> getCameraGroups() throws IOException {
        return client.getGroups();
    }

    /**
     * Return the camera configuration.
     */
    public Map<String, Object> getConfig(String cameraName) throws IOException {
        return client.getConfig(cameraName);
    }

    /**
     * Set the camera configuration.
     */
    public void setConfig(String cameraName, Map<String, Object> config) throws IOException {
        client.setConfig(cameraName, config);
    }

    /**
     * Delete the camera configuration.
     */
    public void deleteConfig(String cameraName) throws IOException {
        client.deleteConfig(cameraName);
    }

    /**
     * Return the camera image size.
     */
    public Dimension getGeometry(String cameraName) throws IOException {
        return client.getGeometry(cameraName);
    }

    /**
     * Return the camera image in PNG format.
     */
    public BufferedImage getImage(String cameraName) throws IOException {
         return client.getImage(cameraName);
    }

    /**
     * Get the camera stream address.
     */
    public String getStream(String cameraName) throws IOException {
        return client.getStream(cameraName);
    }

    /**
     * Stop the camera.
     */
    public void stopCamera(String cameraName) throws IOException {
        client.stopInstance(cameraName);
    }

    /**
     * Stop all the cameras in the server.
     */
    public void stopAllCameras() throws IOException {
        client.stopAllInstances();
    }

    /**
     * Start camera streaming and set the stream endpoint to the current stream socket.
     */
    public void start(String cameraName) throws IOException, InterruptedException {
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
