package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.AddressableDevice;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.utils.Str;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public abstract class CamServerStream extends ReadonlyRegisterBase<StreamValue> implements ch.psi.pshell.device.Readable.ReadableType, AddressableDevice {

    final InstanceManagerClient client;
    final ProxyClient proxy;
    final String instanceId;
    final Map<String, Object> config;
    String instance;
    Stream stream;
    boolean push;

    public InstanceManagerClient getClient() {
        return client;
    }

    public ProxyClient getProxy() {
        return proxy;
    }

    public CamServerStream(String name, InstanceManagerClient client, String instanceId) {
        super(name, null);
        this.client = client;
        proxy = new ProxyClient(client.getUrl());
        this.instanceId = instanceId;
        this.config = null;
    }

    public CamServerStream(String name, InstanceManagerClient client, Map<String, Object> config, String instanceId) {
        super(name, null);
        this.client = client;
        proxy = new ProxyClient(client.getUrl());
        this.instanceId = instanceId;
        this.config = config;
    }

    @Override
    public Class _getElementType() {
        return Long.class;
    }

    public Object getValue(String name) {
        StreamValue cache = take();
        if (cache != null) {
            for (int i = 0; i < cache.getIdentifiers().size(); i++) {
                if (cache.getIdentifiers().get(i).equals(name)) {
                    return cache.getValues().get(i);
                }
            }
        }
        return null;
    }

    public Device getChannel(String name) {
        if (stream != null) {
            return stream.getChild(name);
        }
        return null;
    }

    public List<Device> getChannels() {
        if (stream != null) {
            return Arrays.asList(stream.getChildren());
        }
        return new ArrayList();
    }

    public List<String> getChannelNames() {
        if (stream != null) {
            return stream.getIdentifiers();
        }
        return new ArrayList();
    }

    public Stream getStream() {
        return stream;
    }

    protected void startReceiver() {
        try {
            stream.start(true);
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void stopReceiver() {
        try {
            stream.stop();
        } catch (Exception ex) {
            Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (isInitialized() && (stream != null)) {
            if (value) {
                startReceiver();
            } else {
                stopReceiver();
            }
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (stream != null) {
            Object cache = stream.take();
            if (!isMonitored()) {
                try {
                    startReceiver();
                    stream.waitValueNot(cache, -1);
                } finally {
                    stopReceiver();
                }
            } else {
                stream.waitValueNot(cache, -1);
            }
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        start();
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return client.getUrl();
    }

    /**
     * Return the name of the current streaming pipeline.
     */
    public String getInstance() {
        return instance;
    }

    protected void setInstance(String value) {
        instance = value;
    }

    protected void setPush(boolean value) {
        push = value;
    }

    @Override
    public String getAddress() {
        return getUrl() + "/" + Str.toString(instanceId);
    }

    @Override
    protected StreamValue doRead() throws IOException, InterruptedException {
        update();
        return take();
    }

    @Override
    public StreamValue request() {
        if (stream == null) {
            return null;
        }
        return stream.request();
    }

    /**
     * Return the info of the server instance.
     */
    public Map<String, Object> getServerInfo() throws IOException {
        return client.getInfo();
    }

    /**
     * Return the pipeline configuration.
     */
    public Map<String, Object> getConfig(String pipelineName) throws IOException {
        return client.getConfig(pipelineName);
    }

    /**
     * Set configuration of the pipeline.
     */
    public void setConfig(String pipelineName, Map<String, Object> config) throws IOException {
        client.setConfig(pipelineName, config);
    }

    /**
     * Delete configuration of the pipeline.
     */
    public void deleteConfig(String pipelineName) throws IOException {
        client.deleteConfig(pipelineName);
    }

    /**
     * Return the instance stream. If the instance does not exist, it will be
     * created and will be read only - no config changes will be allowed. If
     * instanceId then return existing (writable).
     */
    protected String getStream(String instanceId) throws IOException {
        return client.getStream(instanceId);
    }

    /*
     * Start the pipeline.
     */
    abstract public void start() throws IOException, InterruptedException;

    /**
     * Stop the pipeline.
     */
    public void stop() {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                Logger.getLogger(PipelineStream.class.getName()).log(Level.WARNING, null, ex);
            }
            stream = null;
        }
        instance = null;
    }

    /**
     * Return the camera configuration.
     */
    public Map<String, Object> getInstanceConfig() throws IOException {
        return getConfig(instanceId);
    }

    /**
     * Set camera config
     */
    public void setInstanceConfig(Map<String, Object> config) throws IOException {
        setConfig(instanceId, config);
    }

    /**
     * Delete configuration of the camera.
     */

    public void deleteInstanceConfig() throws IOException {
        deleteConfig(instanceId);
    }

    @Override
    protected void doClose() throws IOException {
        stop();
        for (Device d : this.getComponents()) {
            try {
                d.close();
            } catch (Exception ex) {
                Logger.getLogger(PipelineStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.doClose();
    }

}
