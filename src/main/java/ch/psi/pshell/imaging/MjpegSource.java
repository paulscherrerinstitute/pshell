package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Image source receive frames from a mjpeg server.
 */
public class MjpegSource extends SourceBase {

    final String url;

    public MjpegSource(String name, String url) {
        super(name, new SourceConfig());
        this.url = url;
    }

    InputStream stream;

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        URL aux = new URL(url);
        stream = aux.openStream();
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
    }

    final byte[] START_OF_FRAME = {(byte) 0xFF, (byte) 0xD8};
    final byte[] END_OF_FRAME = {(byte) 0xFF, (byte) 0xD9};
    final int MAX_FRAME_SIZE = 512 * 1024;

    @Override
    protected void doUpdate() throws IOException, InterruptedException {

        byte[] data = null;
        if (stream != null) {
            stream.mark(MAX_FRAME_SIZE);
            try {
                data = readData();
            } catch (EOFException ex) {
                //Try to reopen stream
                doInitialize();
                data = readData();
            }
        }

        if (data == null) {
            pushImage(null);
        } else {
            BufferedImage img = Utils.newImage(data);
            pushImage(img);
        }

    }

    byte[] readData() throws IOException {
        if (stream != null) {
            stream.mark(MAX_FRAME_SIZE);
            int startOfFrame = waitBytes(START_OF_FRAME) - START_OF_FRAME.length;
            if (startOfFrame >= 0) {
                int endOfFrame = waitBytes(END_OF_FRAME);
                if (endOfFrame >= 0) {
                    stream.reset();
                    stream.skip(startOfFrame);
                    int length = endOfFrame + START_OF_FRAME.length;
                    byte[] data = new byte[length];
                    stream.read(data, 0, length);
                    return data;
                }
            }
        }
        return null;
    }

    int waitBytes(byte[] data) throws IOException {
        int index = 0;
        int dataPos = 0;
        while (true) {
            int ret = stream.read();
            if (ret < 0) {
                throw new EOFException();
            }
            byte value = (byte) ret;
            if (value == data[dataPos]) {
                dataPos++;
                if (dataPos == data.length) {
                    return (index + 1);
                }
            } else {
                dataPos = 0;
            }
            index++;
            if (index >= MAX_FRAME_SIZE) {
                return -1;
            }
        }
    }

}
