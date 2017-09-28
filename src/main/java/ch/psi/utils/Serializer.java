package ch.psi.utils;

import java.io.IOException;

/**
 * A facade to encoder operations.
 */
public class Serializer {

    public enum EncoderType {

        bin,
        xml;

        public Encoder getEncoder() {
            switch (this) {
                case xml:
                    return new EncoderXml();
                case bin:
                    return new EncoderBinary();
            }
            return null;
        }
    }

    public static byte[] encode(Object obj) throws IOException {
        return encode(obj, EncoderType.bin);
    }

    public static Object decode(byte[] buf) throws IOException {
        return decode(buf, EncoderType.bin);
    }

    public static byte[] encode(Object obj, Encoder encoder) throws IOException {
        return encoder.encode(obj);
    }

    public static Object decode(byte[] buf, Encoder encoder) throws IOException {
        return encoder.decode(buf);
    }

    public static byte[] encode(Object obj, EncoderType encoderType) throws IOException {
        return encoderType.getEncoder().encode(obj);
    }

    public static Object decode(byte[] buf, EncoderType encoderType) throws IOException {
        return encoderType.getEncoder().decode(buf);
    }

    public static Object copy(Object obj) throws IOException {
        EncoderType type = EncoderType.bin;
        return decode(encode(obj, type), type);
    }
}
