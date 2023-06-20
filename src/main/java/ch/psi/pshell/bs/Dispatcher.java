package ch.psi.pshell.bs;

import ch.psi.pshell.bs.ProviderConfig.SocketType;
import static ch.psi.pshell.bs.StreamChannel.DEFAULT_MODULO;
import static ch.psi.pshell.bs.StreamChannel.DEFAULT_OFFSET;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.Str;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Extension to Provider class, managing the creation and disposal of streams at the Dispatcher
 * layer.
 */
public class Dispatcher extends Provider {

    public static final String PROPERTY_DISPATCHER_URL = "ch.psi.pshell.dispatcher.url";    
    Client client;

    public Dispatcher(String name, String address) {
        super(name, address, new DispatcherConfig());
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        //In order to be able to delete an entity
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        client = ClientBuilder.newClient(config);
    }
    
    @Override
    public DispatcherConfig getConfig() {
        return (DispatcherConfig) super.getConfig();
    }    

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        for (Stream stream : streamSockets.keySet()) {
            closeStream(stream);
        }
    }

    @Override
    public String getStreamSocket(Stream stream) {
        return streamSockets.get(stream);
    }

    public void addSource(String stream, int split) throws IOException {
        ArrayList list = new ArrayList();
        HashMap map = new HashMap();
        map.put("stream", stream);
        map.put("split", split);
        list.add(map);
        addSources(list);
    }

    public void removeSource(String stream, int split) throws IOException {
        ArrayList list = new ArrayList();
        HashMap map = new HashMap();
        map.put("stream", stream);
        map.put("split", split);
        list.add(map);
        removeSources(list);
    }

    public void addSources(List<Map> sources) throws IOException {
        WebTarget resource = client.target(getAddress() + "/sources");
        HashMap map = new HashMap();
        map.put("sources", sources);
        String json = EncoderJson.encode(map, false);
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
    }

    public void removeSources(List<Map> sources) throws IOException {
        WebTarget resource = client.target(getAddress() + "/sources");
        HashMap map = new HashMap();
        map.put("sources", sources);
        String json = EncoderJson.encode(map, false);
        Response r = resource.request().accept(MediaType.TEXT_PLAIN).method("DELETE", Entity.json(json));
    }

    public List<Map> getSources() {
        WebTarget resource = client.target(getAddress() + "/sources");
        return resource.request().accept(MediaType.APPLICATION_JSON).get(ArrayList.class);
    }

    public List<Map> getStreams() {
        WebTarget resource = client.target(getAddress() + "/streams");
        return resource.request().accept(MediaType.APPLICATION_JSON).get(ArrayList.class);
    }

    public void removeStream(String stream) throws IOException {
        WebTarget resource = client.target(getAddress() + "/stream");
        Response r = resource.request().accept(MediaType.TEXT_PLAIN).method("DELETE", Entity.text(stream));
    }

    public String addStream(List channels) throws IOException {
        return addStream(channels, null);
    }
    
    public String addStream(List channels, Stream stream) throws IOException {
        Map config = new HashMap();
        List channelsList = new ArrayList();
        config.put("channels", channelsList);
        config.put("streamType", (getConfig().socketType == SocketType.PULL) ? "push_pull" : "pub_sub");
        if (getConfig().disableCompression) {
            config.put("compression", "none");
        }
        Map mapping = new HashMap();
        StreamConfig.Incomplete streamIncomplete = (stream==null) ? null : stream.getIncomplete();
        if (streamIncomplete!=null){
            mapping.put("incomplete", streamIncomplete.getConfigValue());
        } else if (getConfig().mappingIncomplete!=null){
            mapping.put("incomplete", getConfig().mappingIncomplete.getConfigValue());
        }
        config.put("mapping", mapping);
        
        Map validation = new HashMap();
        if (getConfig().validationInconsistency != null){
            validation.put("inconsistency", getConfig().validationInconsistency.getConfigValue());
        }
        config.put("channelValidation", validation);

        Map sendBehavior = new HashMap();
        if (getConfig().sendStrategy != null){
            sendBehavior.put("strategy", getConfig().sendStrategy.getConfigValue());
        }
        if (getConfig().sendBuildChannelConfig != null){
            sendBehavior.put("buildChannelConfig", getConfig().sendBuildChannelConfig.getConfigValue());
        }
        if (getConfig().sendSyncTimeout>0){
            sendBehavior.put("syncTimeout", getConfig().sendSyncTimeout);
        }
        if (getConfig().sendAwaitFirstMessage){
            sendBehavior.put("awaitFirstMessage", true);
        }        
        config.put("sendBehavior", sendBehavior);
        
        for (Object channel : channels) {
            Map channelDict = new HashMap();
            if (channel instanceof String) {
                channelDict.put("name", channel);
            } else if ((channel instanceof List) && (((List) channel).size() > 0)) {
                channelDict.put("name", ((List) channel).get(0));
                if (((List) channel).size() > 1) {
                    if (((List) channel).get(1)!=null){
                        channelDict.put("modulo", ((List) channel).get(1));
                    }
                }
                if (((List) channel).size() > 2) {
                    if (((List) channel).get(2)!=null){
                        channelDict.put("offset", ((List) channel).get(2));
                    }
                }
            } else if ((channel instanceof Map) && (((Map) channel).containsKey("name"))) {
                channelDict.put("name", ((Map) channel).get("name"));
                if (((Map) channel).containsKey("modulo")) {
                    if (((Map) channel).get("modulo") != null){
                        channelDict.put("modulo", ((Map) channel).get("modulo"));
                    }
                }
                if (((Map) channel).containsKey("offset")) {
                    if (((Map) channel).get("offset") != null){
                        channelDict.put("offset", ((Map) channel).get("offset"));
                    }
                }
            } else {
                continue;
            }
            channelsList.add(channelDict);
        }

        WebTarget resource = client.target(getAddress() + "/stream");
        String json = EncoderJson.encode(config, false);
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
        Map streamData = r.readEntity(Map.class);
        Object url = streamData.get("stream");
        getLogger().fine("Creating stream from: " + json);
        if ((r.getStatus() == Response.Status.OK.getStatusCode()) && (url != null)) {
            getLogger().fine("Created stream: " + url);
            return url.toString();
        }
        String message = "Error creating stream: " + Str.toString(streamData.get("message"))  + " status: " + r.getStatus();
        getLogger().warning(message);
        throw new DeviceException(message);
    }

    final Map<Stream, String> streamSockets = new HashMap<>();

    @Override
    protected void createStream(Stream stream) throws IOException {
        List channels = new ArrayList();
        for (StreamChannel s : stream.channels.values()) {
            List channel = new ArrayList();
            channel.add(s.getId());
            channel.add( (s.getModulo() <= 0) ? null : s.getModulo());
            channel.add( (s.getOffset() < 0 ) ? null : s.getOffset());
            channels.add(channel);
        }
        try {
            String socket = addStream(channels, stream);
            streamSockets.put(stream, socket);
        } catch (IOException ex) {
            throw ex;
        }
    }

    @Override
    protected void closeStream(Stream stream) {
        try {
            String socket = streamSockets.get(stream);
            if (socket != null) {
                streamSockets.remove(stream);
                removeStream(socket);
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }    
                                                                                                           
    public static Dispatcher createDefault() {
        Dispatcher dispatcher =  new Dispatcher("dispatcher", System.getProperty(PROPERTY_DISPATCHER_URL, "https://dispatcher-api.psi.ch/sf"));
        try{
            dispatcher.initialize();
        } catch (Exception ex){
             Logger.getLogger(Provider.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dispatcher;
    }                            
}
