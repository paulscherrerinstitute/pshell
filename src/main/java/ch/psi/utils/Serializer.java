package ch.psi.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A facade to encoder operations.
 */
public class Serializer {

    public enum EncoderType {

        bin,
        json,
        xml;

        public Encoder getEncoder() {
            switch (this) {
                case xml:
                    return new EncoderXml();
                case json:
                    return new EncoderJson();                    
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

    public static void encode(Object obj, Path path) throws IOException {
        EncoderType type = EncoderType.valueOf(IO.getExtension(path.toFile()));
        byte[] encoded =  encode(obj, type);
        Files.write(path, encoded);
    }

    public static Object decode(Path path) throws IOException {
        EncoderType type = EncoderType.valueOf(IO.getExtension(path.toFile()));
        byte[] encoded =  Files.readAllBytes(path);
        return decode(encoded, type);
    }
    
    public static void encode(Object obj, File file) throws IOException {
        encode(obj, file.toPath());
    }

    public static Object decode(File file) throws IOException {
        return decode(file.toPath());
    }
    

    public static Object copy(Object obj) throws IOException {
        EncoderType type = EncoderType.bin;
        return decode(encode(obj, type), type);
    }
}
