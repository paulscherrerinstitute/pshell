package ch.psi.pshell.serial;

import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceConfig;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class SerialDeviceBase extends DeviceBase implements SerialDevice {

    Level logLevel = Level.FINEST;

    protected SerialDeviceBase(String name) {
        super(name, null);
    }

    /**
     * Device with configuration
     */
    protected SerialDeviceBase(String name, DeviceConfig config) {
        super(name, config);
    }

    Mode mode = Mode.HalfDuplex;

    @Override
    public Mode getMode() {
        return mode;
    }

    protected void setMode(Mode mode) {
        this.mode = mode;
    }

    public void assertFullDuplex() throws IOException {
        if (getMode() != Mode.FullDuplex) {
            throw new DeviceException("Not in full duplex mode");
        }
    }

    public void assertHalfDuplex() throws IOException {
        if (getMode() != Mode.HalfDuplex) {
            throw new DeviceException("Not in half duplex mode");
        }
    }

    abstract protected void writeByte(byte b) throws IOException;

    /**
     * returns -1 if byte is not available
     */
    abstract protected int readByte() throws IOException;

    //Writing     
    @Override
    public synchronized void write(byte tx) throws IOException {
        assertInitialized();
        log("TX", tx);
        if (isSimulated()) {
            return;
        }
        writeByte(tx);
    }

    @Override
    /**
     * Implementations can improve
     */
    public synchronized void write(byte tx[]) throws IOException {
        assertInitialized();
        log("TX", tx);
        if (isSimulated()) {
            return;
        }
        for (byte b : tx) {
            writeByte(b);
        }
    }

    @Override
    public void write(String str) throws IOException {
        write(str.getBytes());
    }

    /**
     * Implementations can improve
     */
    @Override
    public synchronized void flush() throws IOException {
        assertInitialized();
        if (isSimulated()) {
            return;
        }
        while (read() > 0) {
        }
    }

    //Reading single byte
    /**
     * Returns -1 if no byte available
     */
    @Override
    public synchronized int read() throws IOException {
        assertInitialized();
        assertHalfDuplex();
        if (isSimulated()) {
            return -1;
        }

        int ret = readByte();
        if (ret <= 0) {
            return -1;
        }

        log("RX", (byte) ret);
        return ret;
    }

    //Waiting answer 
    @Override
    public byte waitByte(int timeout) throws IOException, InterruptedException {
        return waitByte(timeout, null);
    }

    @Override
    public synchronized byte waitByte(int timeout, Byte value) throws IOException, InterruptedException {
        assertHalfDuplex();

        Chrono chrono = new Chrono();
        while (true) {
            int rx = read();
            if (rx >= 0) {
                if ((value == null) || (value == rx)) {
                    return (byte) rx;
                }
            }
            if (chrono.isTimeout(timeout)) {
                throw new DeviceTimeoutException();
            }
            sleep();
        }
    }

    @Override
    public byte[] waitBytes(int bytes, int timeout) throws IOException, InterruptedException {
        return waitBytes(null, null, bytes, timeout, false);
    }

    @Override
    public byte[] waitBytes(byte[] header, byte[] trailer, int messageSize, int timeout) throws IOException, InterruptedException {
        return waitBytes(header, trailer, messageSize, timeout, false);
    }

    @Override
    public synchronized byte[] waitBytes(byte[] header, byte[] trailer, int messageSize, int timeout, boolean payloadOnly) throws IOException, InterruptedException {
        assertInitialized();
        assertHalfDuplex();
        if (isSimulated()) {
            return null;
        }
        if (header == null) {
            header = new byte[0];
        }
        if (trailer == null) {
            trailer = new byte[0];
        }
        boolean receivedHeader = (header.length == 0);
        boolean receivedTrailer = (trailer.length == 0);
        int bytes = messageSize;

        byte[] buffer = new byte[0x4000];
        int index = 0;

        Chrono chrono = new Chrono();
        while (true) {
            int rx;
            while ((rx = readByte()) >= 0) {
                if (index >= buffer.length) {
                    throw new DeviceException("Buffer overload");
                }
                buffer[index++] = (byte) rx;
                if (receivedHeader) {
                    if ((receivedTrailer == false) && (index >= (header.length + trailer.length - bytes))) {
                        if (Arr.endsWith(buffer, index, trailer)) {
                            receivedTrailer = true;
                        }
                    } else {
                        bytes--;
                    }

                    if ((receivedTrailer) && (bytes == 0)) {

                        byte[] ret = new byte[payloadOnly ? index - header.length - trailer.length - messageSize : index];
                        System.arraycopy(buffer, (payloadOnly ? header.length : 0), ret, 0, ret.length);
                        log("RX", ret);
                        return ret;
                    }
                } else {
                    if (Arr.endsWith(buffer, index, header)) {
                        index = header.length;
                        System.arraycopy(header, 0, buffer, 0, index);
                        receivedHeader = true;
                    }
                }
            }
            if (chrono.isTimeout(timeout)) {
                throw new DeviceTimeoutException();
            }
            sleep();
        }
    }

    @Override
    public String waitString(int timeout) throws IOException, InterruptedException {
        String trailer = new String(new byte[]{13, 10});
        return waitString(trailer, timeout);
    }

    @Override
    public String waitString(String trailer, int timeout) throws IOException, InterruptedException {
        return waitString(null, trailer, 0, timeout);
    }

    @Override
    public synchronized String waitString(String header, String trailer, int bytes, int timeout) throws IOException, InterruptedException {
        assertInitialized();
        assertHalfDuplex();
        if (isSimulated()) {
            return null;
        }
        boolean receivedHeader = (header == null);
        boolean receivedTrailer = (trailer == null);
        StringBuffer buffer = new StringBuffer();

        Chrono chrono = new Chrono();
        while (true) {
            int rx;
            while ((rx = readByte()) >= 0) {
                if (receivedHeader) {
                    buffer.append((char) rx);
                    if (receivedTrailer == false) {
                        if (buffer.toString().endsWith(trailer)) {
                            receivedTrailer = true;
                        }
                    } else {
                        bytes--;
                    }

                    if ((receivedTrailer) && (bytes == 0)) {
                        String ret = buffer.toString();
                        log("RX", ret);
                        return ret;
                    }
                } else {
                    buffer.append((char) rx);
                    if (buffer.toString().endsWith(header)) {
                        buffer = new StringBuffer();
                        buffer.append(header);
                        receivedHeader = true;
                    }
                }
            }
            if (chrono.isTimeout(timeout)) {
                throw new DeviceTimeoutException();
            }
            sleep();
        }
    }

    //Send/receive pairs with retries
    /**
     * Send/receive pair of Strings with CRLF delimitation
     */
    @Override
    public synchronized byte[] sendReceive(byte[] tx, byte[] header, byte[] trailer, int bytes, int timeout, int retries) throws IOException, InterruptedException {
        int retry = 1;
        while (true) {
            try {
                flush();
                write(tx);
                return waitBytes(header, trailer, bytes, timeout);
            } catch (IOException ex) {
                if (retry >= retries) {
                    throw ex;
                }
                retry++;
            }
        }
    }

    @Override
    public synchronized String sendReceive(String tx, int timeout, int retries) throws IOException, InterruptedException {
        int retry = 1;
        while (true) {
            try {
                flush();
                write(tx);
                return waitString(timeout);
            } catch (IOException ex) {
                if (retry >= retries) {
                    throw ex;
                }
                retry++;
            }
        }
    }

    @Override
    public synchronized String sendReceive(String tx, String header, String trailer, int bytes, int timeout, int retries) throws IOException, InterruptedException {
        int retry = 1;
        while (true) {
            try {
                flush();
                write(tx);
                return waitString(header, trailer, bytes, timeout);
            } catch (IOException ex) {
                if (retry >= retries) {
                    throw ex;
                }
                retry++;
            }
        }
    }

    //Reading in full-duplex mode (asynchronous)
    private StringBuffer bufferAsyncRx = new StringBuffer();

    /**
     * To be called by implementations in full-duplex mode (asynchronous). If rx < 0 clears
     * reception buffers.
     */
    protected void onByte(int rx) {
        if (rx < 0) {
            bufferAsyncRx = new StringBuffer();
            return;
        }

        switch (rx) {
            case 10:
            case 13:
                if (bufferAsyncRx.length() > 0) {
                    String ret = bufferAsyncRx.toString();
                    log("RX", ret);
                    onString(ret);
                    bufferAsyncRx = new StringBuffer();
                }
                break;
            default:
                bufferAsyncRx.append((char) rx);
                break;

        }
    }

    /**
     * Default parsing of messages in full-duplex mode using CR/LF to delimit. For other protocols
     * override onByte
     */
    protected void onString(String str) {
    }

    //Tools
    private void sleep() throws InterruptedException {
        Thread.sleep(1);
    }
    
    public void setLogLevel(Level level){
        logLevel = level;
    }

    public Level getLogLevel(){
        return logLevel;
    }

    //Repeating prototype to avoid overhead when not logging
    void log(String event, byte[] data) {
        Logger logger = getLogger();
        if (logger.isLoggable(logLevel)) {
            logger.log(logLevel, event + ": " + Convert.arrayToHexString(data, " "));
        }
    }

    void log(String event, byte data) {
        Logger logger = getLogger();
        if (logger.isLoggable(logLevel)) {
            logger.log(logLevel, event + ": " + Convert.arrayToHexString(new byte[]{data}, " "));
        }
    }

    void log(String event, String data) {
        Logger logger = getLogger();
        if (logger.isLoggable(logLevel)) {
            logger.log(logLevel, event + ": " + Convert.arrayToHexString(data.getBytes(), " "));
        }
    }

}
