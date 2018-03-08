package ch.psi.pshell.serial;

import ch.psi.utils.Chrono;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serial communication through a UDP connection.
 */
public class UdpDevice extends SerialDeviceBase {

    volatile byte[] buffer;
    volatile int indexRx;

    String address;
    int port;
    int timeout;

    DatagramSocket socket = null;

    //Constructors
    public UdpDevice(String name) {
        super(name, new SocketDeviceConfig());
    }

    public UdpDevice(String name, String server) {
        this(name, server.substring(0, server.indexOf(":")), Integer.valueOf(server.substring(server.indexOf(":") + 1)));
    }

    public UdpDevice(String name, String address, int port) {
        this(name, address, port, 3000);
    }

    public UdpDevice(String name, String address, int port, int timeout) {
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
        buffer = null;
        indexRx = 0;

        doClose(); //If do not want to close children, call closeSocket) instead.
        super.doInitialize();
        if (isSimulated()) {
            return;
        }
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
        } catch (Exception ex) {
            close();
            throw new DeviceException(ex);
        }
        if (getMode() == Mode.FullDuplex) {
            waitThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        buffer = receive();
                        onString(new String(buffer));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UdpDevice.class.getName()).log(Level.FINE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(UdpDevice.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                waitThread = null;
            });
            waitThread.start();

        }
    }

    public DatagramSocket getClient() {
        return socket;
    }
    
    void closeSocket(){
        if (waitThread != null) {
            waitThread.interrupt();
            waitThread = null;
        }
        
        if (isSimulated()) {
            return;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }        
    }

    @Override
    protected void doClose() throws IOException {
        closeSocket();
        super.doClose();
    }

    @Override
    protected void writeByte(byte b) throws IOException {
        write(new byte[]{b});
    }

    @Override
    public synchronized void write(byte tx[]) throws IOException {
        assertInitialized();
        log("TX", tx);
        if (isSimulated()) {
            return;
        }
        InetAddress address = InetAddress.getByName(this.address);
        DatagramPacket txPack = new DatagramPacket(tx, tx.length, address, port);
        socket.send(txPack);
        startWaitForAnswer();
    }

    /**
     * returns -1 if byte is not available
     */
    @Override
    protected int readByte() throws IOException {
        if ((buffer != null) && (indexRx < buffer.length)) {
            return buffer[indexRx++];
        }
        return -1;
    }

    Thread waitThread;

    public void startWaitForAnswer() {
        if (waitThread == null) {
            buffer = null;
            indexRx = 0;
            waitThread = new Thread(() -> {
                byte[] data;
                try {
                    buffer = receive();
                } catch (InterruptedException ex) {
                    Logger.getLogger(UdpDevice.class.getName()).log(Level.FINE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(UdpDevice.class.getName()).log(Level.SEVERE, null, ex);
                }
                waitThread = null;
            });
            waitThread.start();
        }
    }

    public byte[] receive() throws IOException, TimeoutException, InterruptedException {
        byte[] ret;
        Chrono chrono = new Chrono();
        byte[] buffer = new byte[10000];
        DatagramPacket rxPack = new DatagramPacket(buffer, buffer.length);
        socket.receive(rxPack);
        return Arrays.copyOfRange(rxPack.getData(), 0, rxPack.getLength());
    }

    @Override
    public synchronized void flush() throws IOException {
        if (socket != null) {
            socket.setSoTimeout(1);
            while (true) {
                try {
                    receive();
                } catch (Exception ex) {
                    break;
                }
            }
            socket.setSoTimeout(timeout);
        }
        buffer = null;
        indexRx = 0;
    }
}
