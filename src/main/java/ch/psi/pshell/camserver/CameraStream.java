package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;

import java.io.IOException;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class CameraStream extends CamServerStream {

    public CameraStream(String name, String url, String instanceId) {
        super(name, new CameraClient(url), instanceId);
    }

    public CameraClient getClient() {
        return (CameraClient) super.getClient();
    }

    void startInstance(String instanceId) throws IOException {
        stop();
        String streamUrl = getStream(instanceId);
        stream = new Stream(getName() + " stream", streamUrl, false);
        setInstance(instanceId);
    }

    /**
     * Start the camera.
     */
    public void start() throws IOException, InterruptedException {
        startInstance(instanceId);

        if (stream != null) {
            stream.addListener(new DeviceAdapter() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    setCache(value, timestamp);
                }
            });
            stream.initialize();
            if (isMonitored()) {
                startReceiver();
            }
        }
    }
}
