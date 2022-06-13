package ch.psi.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Access to DataAPI service, which performs queries on the DataBuffer
 */
public class DispatcherAPI extends DataAPI{

    public DispatcherAPI(String url) {
        super(url);
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
        WebTarget resource = client.target(url + "/channels/live");
        Response r = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));
        json = r.readEntity(String.class);
        List<Map<String, Object>> query = (List) EncoderJson.decode(json, List.class);
        List<Map> list = (List<Map>) query.get(0).get("channels");
        List<String> ret = new ArrayList<>();
        for (Map map : list){
            ret.add(String.valueOf(map.get("name")));
        }
        return ret;
    }

    public List<String> queryNames(String regex, String backend, Ordering ordering, Boolean reload) throws IOException {
        return queryNames(regex, (backend == null) ? null : new String[]{backend}, ordering, reload);
    }    
    
}
