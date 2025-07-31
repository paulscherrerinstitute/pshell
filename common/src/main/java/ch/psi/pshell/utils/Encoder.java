package ch.psi.pshell.utils;

import java.io.IOException;

/**
 * A common interface for object encoders and decoders.
 */
public interface Encoder {

    byte[] encode(Object obj) throws IOException;

    default Object decode(byte[] buf) throws IOException{
        return decode(buf, null);
    }
    
    Object decode(byte[] buf, Class cls) throws IOException;
}
