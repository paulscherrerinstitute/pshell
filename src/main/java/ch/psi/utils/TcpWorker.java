package ch.psi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker thread for TcpServer and TcpClient.
 */
abstract class TcpWorker extends Thread {

    private final Socket socket;
    private boolean alive = true;

    TcpWorker(Socket socket) {
        this.socket = socket;
        setDaemon(true);
    }

    boolean isSocketAlive() {
        return (alive) && (socket.isConnected()) && (!socket.isClosed());
    }

    /**
     * Protocol can be implemented overriding
     */
    protected void onBytes(byte[] bufferRx, int bytes) {
        byte[] rx = new byte[bytes];
        System.arraycopy(bufferRx, 0, rx, 0, bytes);
        onMessage(rx);
    }

    /**
     * Protocol can be implemented overriding
     */
    protected synchronized void send(byte[] tx) throws IOException {
        OutputStream output = socket.getOutputStream();
        output.write(tx);
    }

    protected synchronized void flush() throws IOException {
        OutputStream output = socket.getOutputStream();
        output.flush();
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        alive = true;
        try {
            InputStream input = socket.getInputStream();
            while (!isInterrupted()) {
                byte[] bufferRx = new byte[4096];
                int bytes = input.read(bufferRx);
                if (isSocketAlive() == false) {
                    return;
                }
                if (bytes < 0) {
                    return; //End of stream
                }
                try {
                    onBytes(bufferRx, bytes);
                } catch (Exception ex) {
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TcpWorker.class.getName()).log(Level.FINE, null, ex);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ex) {
                }
            }
            alive = false;
            onClosed();
        }
    }

    //Abstracts
    protected abstract void onMessage(byte[] msg);

    protected abstract void onClosed();

}
