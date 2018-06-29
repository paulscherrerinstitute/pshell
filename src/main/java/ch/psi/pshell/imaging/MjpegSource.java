package ch.psi.pshell.imaging;


import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

/**
 * Image source receive frames from a mjpeg server.
 */
public class MjpegSource extends SourceBase {

    final String url;
    final boolean flushOnUpdate;

    public MjpegSource(String name, String url) {
        this(name, url, false);
    }

    public MjpegSource(String name, String url, boolean flushOnUpdate) {
        super(name, new SourceConfig());
        this.url = url;
        this.flushOnUpdate = flushOnUpdate;
    }

    InputStream stream;

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        URL aux = new URL(url);
        synchronized(url){
            stream = aux.openStream();
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }
        }
    }

    Thread monitoringThread;

    @Override
    protected void doSetMonitored(boolean value) {
        if (value && (monitoringThread == null)) {
            monitoringThread = new Thread(() -> {
                try {
                    while (true) {
                        try {
                            doUpdate();
                            Thread.sleep(1);
                        } catch (IOException ex) {
                            getLogger().log(Level.FINER, null, ex);
                        }
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            });
            monitoringThread.setDaemon(true);
            monitoringThread.start();
        } else if (!value && (monitoringThread != null)) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }
    }

    final byte[] START_OF_FRAME = {(byte) 0xFF, (byte) 0xD8};
    final byte[] END_OF_FRAME = {(byte) 0xFF, (byte) 0xD9};
    final int MAX_FRAME_SIZE = 512 * 1024;

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        byte[] data = null;
        if (stream != null) {
            if (flushOnUpdate) {
                flush();
            }
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
        synchronized(url){
            if (stream != null) {
                stream.mark(MAX_FRAME_SIZE);
                int startOfFrame = waitBytes(START_OF_FRAME) - START_OF_FRAME.length;
                if (startOfFrame >= 0) {
                    int endOfFrame = waitBytes(END_OF_FRAME);
                    if (endOfFrame >= 0) {
                        stream.reset();
                        stream.skip(startOfFrame);
                        int length = endOfFrame;
                        byte[] data = new byte[length];
                        stream.read(data, 0, length);
                        return data;
                    }
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

    public void flush() throws IOException {
        //stream.skip(stream.available());           
        //TODO: Skipping won't make the current image to be displayed
        synchronized(url){
            stream.close();
            stream = new URL(url).openStream();
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }
        }
    }

}