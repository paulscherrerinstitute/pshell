package ch.psi.pshell.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Helper class simplifying creation of ZMQ replier.
 */
public abstract class Replier implements AutoCloseable {

    final Thread thread;
    final int port;
    Socket socket;
    Context context;

    public Replier(int port) {
        this.port = port;
        thread = new Thread(() -> {
            context = ZMQ.context(1);
            socket = context.socket(zmq.ZMQ.ZMQ_REP);

            socket.bind("tcp://*:" + port);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String request;
                    try {
                        request = socket.recvStr();
                    } catch (Throwable ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.FINE, null, ex);
                        break;
                    }
                    try {
                        String response = onReceived(request);
                        socket.send(response);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.FINE, null, ex);
                        break;
                    } catch (Exception ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.WARNING, null, ex);
                        socket.send("ERROR: " + ex.getMessage());
                    }

                }
            } finally {
                socket.close();
                context.term();
            }
            Logger.getLogger(Replier.class.getName()).log(Level.INFO, "Quitting");

        }, "Replier task - port: " + port);
        thread.start();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }

    /**
     * Returns the answer
     */
    abstract protected String onReceived(String request) throws Exception;
}
