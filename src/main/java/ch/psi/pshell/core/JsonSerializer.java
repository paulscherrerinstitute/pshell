package ch.psi.pshell.core;

import ch.psi.utils.EncoderJson;
import java.io.IOException;

/**
 * Utility class providing JSON serialization.
 */
@Deprecated
public class JsonSerializer {

    public static String encode(Object obj, boolean pretty) throws IOException {
        return EncoderJson.encode(obj, pretty);
    }

    public static String encode(Object obj) throws IOException {
        return encode(obj, false);
    }

    public static Object decode(String json, Class cls) throws IOException {
        return EncoderJson.decode(json, cls);
    }
}
