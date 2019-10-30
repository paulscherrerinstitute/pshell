package ch.psi.utils;

import ch.psi.pshell.core.JsonSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
 *
 */
public class Inventory {
    

    public static <T> T inventoryRequest(String url, Map attributes, Class<T> type) throws IOException {
        String json = JsonSerializer.encode(attributes);
        Client client = ClientBuilder.newClient(new ClientConfig().register(JacksonFeature.class));
        try {
            WebTarget resource = client.target(url);
            Response r = resource.request().accept(MediaType.TEXT_HTML).post(Entity.json(json));
            json = r.readEntity(String.class);
            if (r.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new IOException("Inventory returned error: " + json);
            }
            Map ret = (Map) JsonSerializer.decode(json, Map.class);
            return (T) ret.get("d");
        } finally {
            client.close();
        }
    }

    public static String findPartidByHoly(String holy_name, String type) throws IOException {
        if (type == null) {
            type = "DSCR";
        }
        Map query = new HashMap();
        Map qn = new HashMap();
        Map qt = new HashMap();
        qn.put("Field", "Holy List Name");
        qn.put("Operator", "Is");
        qn.put("Value", holy_name);
        qt.put("Field", "Type");
        qt.put("Operator", "Is");
        qt.put("Value", type);
        query.put("query", Arrays.asList(new Map[]{qn, qt}));
        query.put("columns", Arrays.asList(new String[]{"Label"}));

        Map attr = new HashMap();
        attr.put("search", query);
        Map ret = (Map) inventoryRequest("https://inventory.psi.ch/DataAccess.asmx/FindObjects", attr, Map.class);
        try {
            return (String) ((List) (((List) ret.get("Rows")).get(0))).get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    public static List findAllByType(String type, String column) throws IOException {
        if (type == null) {
            type = "DSCR";
        }
        if (column == null) {
            column = "Holy List Name";
        }

        Map query = new HashMap();
        Map q = new HashMap();
        q.put("Field", "Type");
        q.put("Operator", "Is");
        q.put("Value", type);
        query.put("query", Arrays.asList(new Map[]{q}));
        query.put("columns", Arrays.asList(new String[]{"Label"}));

        Map attr = new HashMap();
        attr.put("search", query);
        Map r = (Map) inventoryRequest("https://inventory.psi.ch/DataAccess.asmx/FindObjects", attr, Map.class);
        List rows = (List) (r.get("Rows"));
        List ret = new ArrayList();
        for (Object list : rows) {
            ret.add(((List) list).get(0));
        }
        return ret;
    }

    public static List<Map> getPartAttributesFromInventory(String part_label, String holy_name) throws IOException {
        if ((holy_name != null) && !holy_name.isEmpty()) {
            part_label = findPartidByHoly(holy_name, "DSCR");
            if ((part_label == null) || part_label.isEmpty()) {
                throw new IOException("Could not find inventory part for: " + holy_name);
            }
        }
        Map map = new HashMap();
        map.put("psiLabel", part_label);
        return (List) inventoryRequest("https://inventory.psi.ch/DataAccess.asmx/GetPartAttributes", map, List.class);
    }

    public static List<Double> getCalibFromInventory(String part_label, String holy_name) throws IOException {
        double horizontal_dist = 0.0;
        double vertical_dist = 0.0;
        double horizontal_tilt = 0.0;
        double vertical_tilt = 0.0;

        List<Map> attributes = getPartAttributesFromInventory(part_label, holy_name);

        for (Map a : attributes) {
            String name = (String) a.get("Name");
            Double val = 0.0;
            try {
                val = Double.valueOf(a.get("Value").toString());
            } catch (Exception ex) {
            }

            switch (name) {
                case "Crystal angle in x (e-beam system) [deg]":
                    horizontal_tilt = val;
                    break;
                case "Crystal angle in y (e-beam system) [deg]":
                    vertical_tilt = val;
                    break;
                case "Mark distance in x (e-beam system) [mm]":
                    horizontal_dist = val;
                    break;
                case "Mark distance in y (e-beam system) [mm]":
                    vertical_dist = val;
                    break;
            }
        }

        return Arrays.asList(new Double[]{horizontal_dist, vertical_dist, horizontal_tilt, vertical_tilt});
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getCalibFromInventory("SINEG01-DSCR190", "SINEG01-DSCR190"));
        System.out.println(findAllByType(null, null));        
    }
}