package ch.psi.pshell.archiver;

import ch.psi.pshell.utils.EncoderJson;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Access to DataAPI service, which performs queries on the DataBuffer
 */
public class DataAPI implements ChannelQueryAPI {

    final String url;
    final Client client;
    
    public enum Ordering {
        none,
        asc,
        desc
    }

    public DataAPI(String url) {
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

    //public List<Map<String, Object>> queryNames(String regex, String[] backends, Ordering ordering, Boolean reload) throws IOException {
    public List<String> queryNames(String regex, String[] backends, Ordering ordering, Boolean reload) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("regex", regex);
        if (ordering != null) {
            data.put("ordering", ordering.toString());
        }
        if (backends != null) {
            data.put("backends", backends);
        }
        if (reload != null) {
            data.put("reload", reload);
        }
        String json = EncoderJson.encode(data, false);
        WebTarget resource = client.target(url + "/channels");
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
        json = r.readEntity(String.class);
        //List<Map<String, Object>> ret = (List) EncoderJson.decode(json, List.class);
        List<String> ret = (List) EncoderJson.decode(json, List.class);
        return ret;
    }

    public List<String> queryNames(String regex, String backend, Ordering ordering, Boolean reload) throws IOException {
        //List<Map<String, Object>> ret = queryNames(regex, (backend == null) ? null : new String[]{backend}, ordering, reload);
        //return (List<String>) ret.get(0).get("channels");
        return  queryNames(regex, (backend == null) ? null : new String[]{backend}, ordering, reload);
    }
    

    
    public List<Map<String, Object>> queryData(String[] channels, Object start, Object end) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        data.put("channels", channels);
        data.put("ordering", Ordering.none.toString());
        
        Map<String, Object> range = new HashMap<>();
        if (start instanceof Number number){
            range.put("startPulseId", number.longValue());
        } else if (start instanceof String str){
            range.put("startDate", str);
        }
        if (end instanceof Number number){
            range.put("endPulseId", number.longValue());
        } else if (end instanceof String str){
            range.put("endDate", str);
        }
        data.put("range", range);
        
        data.put("configFields", new String[]{"globalDate",  "type", "shape"});
        data.put("eventFields", new String[]{"pulseId", "globalDate",  "value"});
        
        String json = EncoderJson.encode(data, false);
        WebTarget resource = client.target(url + "/query");
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
        json = r.readEntity(String.class);
        List<Map<String, Object>> ret = (List) EncoderJson.decode(json, List.class);
        return ret;
    }
    
    

    @Override
    public List<String> queryChannels(String text, String backend, int limit) throws IOException{
        return queryNames(text, backend, DataAPI.Ordering.desc, Boolean.FALSE);
    }
    
}
