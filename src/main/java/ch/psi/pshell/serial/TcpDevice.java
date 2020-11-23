package ch.psi.pshell.serial;

import ch.psi.utils.TcpClient;
import java.io.IOException;

/**
 * Serial communication through a TCP connection.
 */
public class TcpDevice extends SerialDeviceBase {

    Socket client;
    StringBuffer buffer;    //TODO: This buffering may be optimized

    String address;
    int port;
    int timeout;

    class Socket extends TcpClient {

        public Socket(String address, int port, int timeout) {
            super(address, port, timeout);
        }

        @Override
        protected void onReceivedData(byte[] data) {
            if (TcpDevice.this.isInitialized()) {
                for (byte b : data) {
                    if (getMode() == Mode.HalfDuplex) {
                        buffer.append((char) b);
                    } else {
                        onByte(b);
                    }
                }
            }
        }
    }

    //Constructors
    public TcpDevice(String name) {
        super(name, new SocketDeviceConfig());
    }

    public TcpDevice(String name, String server) {
        this(name, server.substring(0, server.indexOf(":")), Integer.valueOf(server.substring(server.indexOf(":") + 1)));
    }

    public TcpDevice(String name, String address, int port) {
        this(name, address, port, 3000);
    }

    public TcpDevice(String name, String address, int port, int timeout) {
        super(name);
        this.address = address;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public SocketDeviceConfig getConfig() {
        return (SocketDeviceConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (getConfig() != null) {
            this.port = getConfig().port;
            this.address = getConfig().address;
            this.timeout = getConfig().timeout;
        }
        buffer = new StringBuffer(1024);
        doClose(); //If do not want to close children, call closeSocket) instead.
        super.doInitialize();
        if (isSimulated()) {
            return;
        }
        try {
            client = new Socket(address, port, timeout);
        } catch (Exception ex) {
            close();
            throw new DeviceException(ex);
        }
    }

    public TcpClient getClient() {
        return client;
    }

    void closeSocket(){
        if (isSimulated()) {
            return;
        }
        if (client != null) {
            client.close();
            client = null;
        }         
    }    
    
    @Override
    protected void doClose() throws IOException {
        closeSocket();
        super.doClose();
    }

    @Override
    protected void writeByte(byte b) throws IOException {
        client.send(new byte[]{b});
    }

    @Override
    public synchronized void write(byte tx[]) throws IOException {
        assertInitialized();
        log("TX", tx);
        if (isSimulated()) {
            return;
        }
        client.send(tx);
    }

    /**
     * returns -1 if byte is not available
     */
    @Override
    protected int readByte() throws IOException {
        if (buffer.length() > 0) {
            byte ret = (byte) buffer.charAt(0);
            buffer.deleteCharAt(0);
            return ret;
        }
        return -1;
    }

    @Override
    public synchronized void flush() throws IOException {
        client.flush();
        buffer = new StringBuffer(1024);
    }

    @Override
    public synchronized byte[] waitBytes(byte[] header, byte[] trailer, int messageSize, int timeout, boolean payloadOnly) throws IOException, InterruptedException {
        assertInitialized();
        checkConnected();
        return super.waitBytes(header, trailer, messageSize, timeout, payloadOnly);
    }

    @Override
    public synchronized String waitString(String header, String trailer, int bytes, int timeout) throws IOException, InterruptedException {
        assertInitialized();
        checkConnected();
        return super.waitString(header, trailer, bytes, timeout);
    }

    public void checkConnected() throws IOException {
        client.checkConnected();
    }

    public boolean isConnected() {
        return client.isConnected();
    }
}
