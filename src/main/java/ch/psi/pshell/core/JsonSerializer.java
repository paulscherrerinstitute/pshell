package ch.psi.pshell.core;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class providing JSON serialization.
 */
public class JsonSerializer {

    final static private ObjectMapper mapper = new ObjectMapper();

    public static String encode(Object obj, boolean pretty) throws IOException {
        if (pretty) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }
        return mapper.writeValueAsString(obj);
    }

    public static String encode(Object obj) throws IOException {
        return encode(obj, false);
    }

    public static Object decode(String json, Class cls) throws IOException {
        return mapper.readValue(json, cls);
    }
}
