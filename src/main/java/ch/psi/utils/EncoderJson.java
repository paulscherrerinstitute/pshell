package ch.psi.utils;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

/**
 * Object encoder to json files (decode to Map objects).
 */
public class EncoderJson implements Encoder {

    final static private ObjectMapper mapper = new ObjectMapper();
    
    //static{
    //    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    //}

    public static String encode(Object obj, boolean pretty) throws IOException {
        if (pretty) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }
        return mapper.writeValueAsString(obj);
    }

    public static Object decode(String json, Class cls) throws IOException {
        return mapper.readValue(json, cls);
    }

    
    @Override
    public byte[] encode(Object obj) throws IOException{
        return encode(obj, true).getBytes();        
    }

    @Override
    public Object decode(byte[] buf, Class cls) throws IOException{
        return decode(new String(buf), cls);
    }
    
}
