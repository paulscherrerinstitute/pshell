package ch.psi.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Implementation of a generic TCP client.
 */
public class TcpClient implements AutoCloseable {

    static final Logger logger = Logger.getLogger(TcpClient.class.getName());

    String address;
    int port;
    int localPort = -1;
    Socket socket;
    Boolean noDelay;//Starting as null to keep default config    
    int currentTimeout;
    int defaultTimeout;
    TcpWorker socketWorker;

    final ArrayList receiveBuffer = new ArrayList();
    final Object lock = new Object();

    public TcpClient(String address, int port) {
        this(address, port, 3000);
    }

    public TcpClient(String address, int port, int timeout) {
        this.address = address;
        this.port = port;
        this.currentTimeout = timeout;
        this.defaultTimeout = timeout;
    }

    public String getServerAddress() {
        return address;
    }

    public int getServerPort() {
        return port;
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
        } catch (Exception ex) {
        }
        socket = null;
    }

    public void setTimeout(int timeout) {
        this.currentTimeout = timeout;
    }

    public void restoreTimeout() {
        currentTimeout = defaultTimeout;
    }

    public void assertConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }
    }

    public void checkConnected() throws IOException {
        if (!isConnected()) {
            connect();
        }
        assertConnected();
    }

    public boolean isConnected() {
        if ((socket == null) || (socketWorker == null)) {
            return false;
        }
        return socketWorker.isSocketAlive();
    }

    public boolean isNoDelay() {
        if (noDelay == null) {
            return false;
        }
        return noDelay;
    }

    public void setNoDelay(boolean value) {
        if ((noDelay == null) || (noDelay != value)) {
            noDelay = value;
            if (socket != null) {
                try {
                    socket.setTcpNoDelay(value);
                } catch (SocketException ex) {
                }
            }
        }
    }

    public void connect() throws IOException {
        synchronized (lock) {
            if (isConnected() == false) {
                disconnect();

                socket = new Socket();
                localPort = socket.getLocalPort();
                if (noDelay != null) {
                    try {
                        socket.setTcpNoDelay(noDelay);
                    } catch (SocketException ex) {
                    }
                }

                SocketAddress server = new InetSocketAddress(InetAddress.getByName(address), port);
                socket.connect(server, currentTimeout);
                socketWorker = newWorker(socket);
                socketWorker.start();
            }
        }
    }

    public void disconnect() {
        synchronized (lock) {
            if (isConnected()) {
                close();
            }
        }
    }

    protected TcpWorker newWorker(Socket s) throws IOException {
        return new TcpClientWorker(s);
    }

    public class TcpClientWorker extends TcpWorker {

        public TcpClientWorker(Socket socket) {
            super(socket);
            setName("Thread- Tcp Client worker");
        }

        @Override
        protected void onMessage(byte[] msg) {
            onReceivedData(msg);
        }

        @Override
        protected void onClosed() {
        }
    }

    public byte[] receive() throws IOException, InterruptedException, TimeoutException {
        checkConnected();
        Chrono chrono = new Chrono();
        if (receiveBuffer.isEmpty()) {
            synchronized (lock) {
                lock.wait(currentTimeout);
            }
            if (receiveBuffer.isEmpty()) {
                throw new TimeoutException();
            }
        }
        byte[] rx = null;
        synchronized (receiveBuffer) {
            rx = (byte[]) receiveBuffer.get(0);
            receiveBuffer.remove(0);
        }
        return rx;
    }

    private final Object messageLock = new Object();

    public void send(byte[] tx) throws IOException {
        checkConnected();
        socketWorker.send(tx);
    }

    public byte[] sendReceive(byte[] cmd, int timeout) throws IOException, InterruptedException, TimeoutException {
        synchronized (messageLock) {
            try {
                if (timeout > 0) {
                    setTimeout(timeout);
                }
                byte[] ret = sendReceive(cmd);
                return ret;
            } finally {
                if (timeout > 0) {
                    restoreTimeout();
                }
            }
        }
    }

    public byte[] sendReceive(byte[] cmd) throws IOException, InterruptedException, TimeoutException {
        synchronized (messageLock) {
            receiveBuffer.clear();
            send(cmd);
            return receive();
        }
    }

    //Overridables
    protected void onReceivedData(byte[] data) {
        synchronized (receiveBuffer) {
            receiveBuffer.add(data);
            synchronized (lock) {
                lock.notifyAll();
            }

        }
    }
}
