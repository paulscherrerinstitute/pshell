package ch.psi.utils;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Object encoder to xml files.
 */
public class EncoderXml implements Encoder {

    @Override
    public byte[] encode(Object obj) throws IOException {

        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); XMLEncoder e = new XMLEncoder(new BufferedOutputStream(b));) {
            e.writeObject(obj);
            return b.toByteArray();
        } catch (Exception ex) {
            throw new IOException(ex);
        } //Implicit close
    }

    @Override
    public Object decode(byte[] buf) throws IOException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(buf); XMLDecoder d = new XMLDecoder(new BufferedInputStream(b));) {
            Object result = d.readObject();
            return result;
        } catch (Exception ex) {
            throw new IOException(ex);
        } //Implicit close
    }
}
