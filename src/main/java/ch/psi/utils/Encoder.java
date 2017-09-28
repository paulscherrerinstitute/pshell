package ch.psi.utils;

import java.io.IOException;

/**
 * A common interface for object encoders and decoders.
 */
public interface Encoder {

    byte[] encode(Object obj) throws IOException;

    Object decode(byte[] buf) throws IOException;
}
