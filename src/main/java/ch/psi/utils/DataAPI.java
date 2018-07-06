package ch.psi.utils;

import ch.psi.pshell.core.JsonSerializer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Access to DataAPI service, which performs queries on the DataBuffer
 */
public class DataAPI {

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

    public List<Map<String, Object>> queryNames(String regex, String[] backends, Ordering ordering, Boolean reload) throws IOException {
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
        String json = JsonSerializer.encode(data);
        WebTarget resource = client.target(url + "/channels");
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
        json = r.readEntity(String.class);
        List<Map<String, Object>> ret = (List) JsonSerializer.decode(json, List.class);
        return ret;
    }

    public List<String> queryNames(String regex, String backend, Ordering ordering, Boolean reload) throws IOException {
        List<Map<String, Object>> ret = queryNames(regex, (backend == null) ? null : new String[]{backend}, ordering, reload);
        return (List<String>) ret.get(0).get("channels");
    }
}
