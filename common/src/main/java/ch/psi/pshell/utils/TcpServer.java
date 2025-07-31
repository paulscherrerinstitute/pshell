package ch.psi.pshell.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for TCP servers.
 */
abstract public class TcpServer implements AutoCloseable {

    static final Logger logger = Logger.getLogger(TcpClient.class.getName());

    int port = 0;
    boolean alive = true;
    SocketThread thread = null;
    ServerSocket socket = null;
    Boolean noDelay = null;
    final HashMap connections;

    abstract protected void onReceivedData(byte[] data, SocketAddress address) throws IOException;

    protected void assertAllowed(SocketAddress address) throws IOException {
    }

    protected void onConnected(SocketAddress address) {
    }

    protected void onDisconnected(SocketAddress address) {
    }

    public TcpServer(int port) throws IOException {
        this.port = port;
        socket = new ServerSocket(port);
        connections = new HashMap();
        thread = new SocketThread();
        thread.setName("Thread- Tcp Server - port:" + port);
        thread.setDaemon(true);
        thread.start();
    }

    public int getPort() {
        return port;
    }

    public boolean isAlive() {
        return alive;
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
            for (SocketAddress add : getConnections()) {
                try {
                    getWorkerSocket(add).setTcpNoDelay(value);
                } catch (Exception ex) {
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        alive = false;
        try {
            closeAllConnections();
        } catch (Exception ex) {
        }
        if (socket != null) {
            socket.close();
            Thread.sleep(0);
            socket = null;
        }
        thread.interrupt();
    }

    class SocketThread extends Thread {

        @Override
        public void run() {
            while ((alive) && (!isInterrupted())) {
                try {
                    //accept a connection
                    Socket answerSocket = socket.accept();
                    if (noDelay != null) {
                        try {
                            answerSocket.setTcpNoDelay(noDelay);
                        } catch (Exception ex) {
                        }
                    }

                    //create a thread to deal with the client
                    if ((alive == true) && (answerSocket != null)) {
                        try {
                            TcpWorker w = newWorker(answerSocket);
                            w.start();
                            onConnected(answerSocket.getRemoteSocketAddress());
                        } catch (Exception ex) {
                            logger.log(Level.INFO, null, ex);
                        }
                    }

                } catch (Exception ex) {
                    if (alive) {
                        logger.log(Level.INFO, null, ex);
                    }
                }
            }
            if (alive) {
                try {
                    close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public boolean isConnected(SocketAddress address) {
        TcpWorker worker = getWorker(address);
        return (worker != null);
    }

    public void assertConnected(SocketAddress address) throws IOException {
        if (!isConnected(address)) {
            throw new IOException("Not connected");
        }
    }

    protected void send(byte[] tx, SocketAddress address) throws IOException {
        assertConnected(address);
        TcpWorker worker = getWorker(address);
        worker.send(tx);
    }

    protected void sendAll(byte[] tx) {
        for (SocketAddress address : getConnections()) {
            try {
                send(tx, address);
            } catch (Exception ex) {
                logger.log(Level.FINE, null, ex);
            }
        }
    }

    protected void flush(SocketAddress address) {
        try {
            assertConnected(address);
            TcpWorker worker = getWorker(address);
            if (worker != null) {
                worker.flush();
            }
        } catch (Exception ex) {
            logger.log(Level.FINE, null, ex);
        }
    }

    protected void flushAll() {
        for (SocketAddress address : getConnections()) {
            flush(address);
        }
    }

    protected SocketAddress[] getConnections() {
        SocketAddress[] ret;
        synchronized (connections) {
            ret = new SocketAddress[connections.size()];
            int i = 0;
            return (SocketAddress[]) connections.keySet().toArray(new SocketAddress[0]);
        }
    }

    protected TcpWorker getWorker(SocketAddress address) {
        TcpWorker ret = null;
        synchronized (connections) {
            ret = (TcpWorker) connections.get(address);
        }
        if (ret == null) {
            for (SocketAddress add : getConnections()) {
                if (address.equals(add)) {
                    return getWorker(add);
                }
            }
        }
        return ret;
    }

    protected Socket getWorkerSocket(SocketAddress address) throws IOException {
        assertConnected(address);
        TcpWorker worker = getWorker(address);
        return worker.getSocket();
    }

    protected void closeAllConnections() {
        SocketAddress[] connections = getConnections();
        for (SocketAddress connection : connections) {
            closeConnection(connection);
        }
    }

    protected void closeConnection(SocketAddress address) {
        for (SocketAddress add : getConnections()) {
            if (address.equals(add)) {
                TcpWorker worker = getWorker(add);
                if (worker != null) {
                    try {
                        worker.getSocket().close();
                    } catch (Exception ex) {
                        logger.log(Level.INFO, null, ex);
                    }
                }
                connections.remove(add);
            }
        }
    }

    protected TcpWorker newWorker(Socket s) throws IOException {
        return new TcpServerWorker(s);
    }

    public class TcpServerWorker extends TcpWorker {

        public TcpServerWorker(Socket answerSocket) throws IOException {
            super(answerSocket);
            setName("Thread- Tcp Server - port:" + port + " worker");
            SocketAddress socketAddress = answerSocket.getRemoteSocketAddress();
            closeConnection(socketAddress);
            assertAllowed(socketAddress);
            synchronized (connections) {
                connections.put(socketAddress, this);
            }
        }

        @Override
        public boolean isSocketAlive() {
            return (alive && super.isSocketAlive());
        }

        protected void onClosed() {
            SocketAddress address = getSocket().getRemoteSocketAddress();
            synchronized (connections) {
                try {
                    connections.remove(address);
                } catch (Exception ex) {
                }
            }
            try {
                onDisconnected(address);
            } catch (Exception ex) {
            }
        }

        @Override
        protected void onMessage(byte[] msg) {
            try {
                onReceivedData(msg, this.getSocket().getRemoteSocketAddress());
            } catch (Exception ex) {
                logger.log(Level.FINE, null, ex);
            }

        }
    }
}
