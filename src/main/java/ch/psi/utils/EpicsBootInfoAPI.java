package ch.psi.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Access to EPICS Boot info
 * https://intranet.psi.ch/wiki/bin/viewauth/Controls_IT/EpicsBootInfo
 */
public class EpicsBootInfoAPI implements ChannelQueryAPI{

    final String url;
    final Client client;

    public enum Ordering {
        none,
        asc,
        desc
    }

    public EpicsBootInfoAPI(String url) {
        if (!url.contains("://")) {
            url = "http://" + url;
        }       
        this.url = url;
        ClientConfig config = new ClientConfig().register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
    }

    /**
     * Return the REST api endpoint address.
     */
    public String getUrl() {
        return url;
    }

    public List<Map<String, Object>> query(String text, String facility, String match, Integer limit) throws IOException {
        
        WebTarget resource = client.target(url + "/find-channel.aspx/" + text);
        if (match != null) {
            resource = resource.queryParam("match", match); //exact, regex, substring
        }        
        if (facility != null) {
            resource = resource.queryParam("facility", facility); //"swissfel", "sls" ...
        }
        if (limit != null) {
            resource = resource.queryParam("limit", limit);
        }        
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).get();
        String json = r.readEntity(String.class);
        List<Map<String, Object>> ret = (List) EncoderJson.decode(json, List.class);
        return ret;
    }
    
    public List<String> queryNames(String regex, String facility, String match, Integer limit) throws IOException {
        List list = query(regex, facility, match, limit);
        for (int i =0; i< list.size(); i++){
            try{
                list.set(i, ((Map)list.get(i)).get("Channel"));
            } catch (Exception ex){
                
            }
        }
        return list;
    }

    @Override
    public List<String> queryChannels(String text, String backend, int limit) throws IOException {
        return queryNames(text, backend, "substring", limit);
    }
}
