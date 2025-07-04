package ch.psi.pshell.detector;

import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.utils.EncoderJson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Base class for PSI streamed detectors. Configuration through REST interface, data on ZMQ stream.
 */
public class DetectorBase extends DeviceBase {

    final String baseUrl;
    Client client;
    WebTarget target;

    public DetectorBase(String name, String baseUrl) {
        super(name, new DetectorConfig());
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        closeClient();
        super.doInitialize();
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
        target = client.target(UriBuilder.fromUri(getBaseUrl()).build());
    }

    public Map readInfoAsMap() throws IOException {
        assertInitialized();
        return target.path("").request().accept(MediaType.APPLICATION_JSON).get(HashMap.class);
    }

    public DetectorInfo readInfo() throws IOException {
        assertInitialized();
        //String jsonResponse = target.path("").request().accept(MediaType.APPLICATION_JSON).get(String.class);
        return target.path("").request().accept(MediaType.APPLICATION_JSON).get(DetectorInfo.class);
    }

    @Override
    public DetectorConfig getConfig() {
        return (DetectorConfig) super.getConfig();
    }

    private static class SettingsWrapper {

        public DetectorConfig settings;
    }

    protected void writeSettings() throws IOException {
        assertInitialized();
        SettingsWrapper obj = new SettingsWrapper();
        obj.settings = getConfig();
        String json = EncoderJson.encode(obj, false);
        Response r = target.path("state").path("configure").request().
                accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
    }

    public void start() throws IOException {
        assertInitialized();
        writeSettings();
        Response r = target.path("state").path("start").request().post(null);
    }

    public void stop() throws IOException {
        assertInitialized();
        Response r = target.path("state").path("stop").request().post(null);
    }
    
    void closeClient(){
        if (client != null) {
            client.close();
            client = null;
        }        
    }

    //Overridables
    @Override
    protected void doClose() throws IOException {
        super.doClose();
        closeClient();
    }

}
