package ch.psi.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Access to IOC info
 * http://iocinfo.psi.ch/swagger-ui.html#!/iocinfo45query45controller/findRecordsUsingGET
 */
public class IocInfoAPI implements ChannelQueryAPI{

    final String url;
    final Client client;

    
    public IocInfoAPI(String url) {
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

    public List<Map<String, Object>> query(String text, String facility, boolean regex, Integer limit) throws IOException {
        
        WebTarget resource = client.target(url + "/records");
        if (facility != null) {
            resource = resource.queryParam("facility", facility); //"swissfel", "sls" ...
        }
        if (limit != null) {
            resource = resource.queryParam("limit", limit);
        }        
        if (!regex){
            text=".*" +text + ".*";
        }
        resource = resource.queryParam("pattern", text);
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).get();
        String json = r.readEntity(String.class);
        List<Map<String, Object>> ret = (List) EncoderJson.decode(json, List.class);
        return ret;
    }
    
    public List<String> queryNames(String text, String facility, boolean regex, Integer limit) throws IOException {
        List list = query(text, facility, regex, limit);
        for (int i =0; i< list.size(); i++){
            try{
                list.set(i, ((Map)list.get(i)).get("name"));
            } catch (Exception ex){
                
            }
        }
        return list;
    }

    @Override
    public List<String> queryChannels(String text, String backend, int limit) throws IOException {
        return queryNames(text, backend, false, limit);
    }
    
}
