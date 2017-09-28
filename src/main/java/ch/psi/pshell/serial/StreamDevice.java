package ch.psi.pshell.serial;

import ch.psi.pshell.device.DeviceConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

/**
 *
 */
public abstract class StreamDevice extends SerialDeviceBase {

    InputStream inputStream;
    OutputStream outputStream;

    protected StreamDevice(String name) {
        super(name, null);
    }

    /**
     * Device with configuration
     */
    protected StreamDevice(String name, DeviceConfig config) {
        super(name, config);
    }

    protected void setStreams(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    protected void doClose() throws IOException {
        if (isSimulated()) {
            return;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
        inputStream = null;
        outputStream = null;
        super.doClose();
    }

    @Override
    protected void writeByte(byte b) throws IOException {
        if (outputStream != null) {
            outputStream.write((int) b);
        }
    }

    /**
     * returns -1 if not available
     */
    @Override
    protected int readByte() throws IOException {
        if (inputStream != null) {
            if (inputStream.available() > 0) {
                int ret = inputStream.read();
                if (ret >= 0) {
                    return ret;
                }
            }
        }
        return -1;
    }

    @Override
    public synchronized void flush() {
        if (isSimulated()) {
            return;
        }
        if (inputStream != null) {
            try {
                while (inputStream.available() > 0) {
                    inputStream.read();
                }
            } catch (Exception ex) {
            }
        }
    }

}
