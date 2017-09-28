package ch.psi.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Object encoder to binary files.
 */
public class EncoderBinary implements Encoder {

    @Override
    public byte[] encode(Object obj) throws IOException {

        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); ObjectOutputStream o = new ObjectOutputStream(b);) {
            o.writeObject(obj);
            return b.toByteArray();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }//Implicit close
    }

    @Override
    public Object decode(byte[] buf) throws IOException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(buf); ObjectInputStream o = new ObjectInputStream(b);) {
            return o.readObject();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }//Implicit close
    }
}
