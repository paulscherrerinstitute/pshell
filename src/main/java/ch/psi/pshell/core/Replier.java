package ch.psi.pshell.core;

import java.util.ArrayList;
import java.util.List;
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
    int requestParts=1;

    public Replier(int port) {
        this.port = port;
        thread = new Thread(() -> {
            context = ZMQ.context(1);
            socket = context.socket(zmq.ZMQ.ZMQ_REP);

            socket.bind("tcp://*:" + port);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    List request;
                    try {
                        request = receive();
                    } catch (Throwable ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.FINE, null, ex);
                        break;
                    }
                    try {
                        List response = reply(request);
                        process(response);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.FINE, null, ex);
                        break;
                    } catch (Exception ex) {
                        Logger.getLogger(Replier.class.getName()).log(Level.WARNING, null, ex);
                    }

                }
            } finally {
                socket.close();
                context.term();
            }
            Logger.getLogger(Replier.class.getName()).log(Level.INFO, "Quitting");

        }, "Replier task - port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Logger.getLogger(Replier.class.getName()).fine("Interrupt received, killing server…");
                context.term();
                try {
                    thread.interrupt();
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
        });

        thread.start();
    }

    protected void setRequestParts(int requestParts) {
        this.requestParts = Math.max(requestParts,1);
    }

    protected void process(List response) throws Exception {
        if (response != null) {
            for (int i = 0; i < response.size(); i++) {
                boolean more = (i < (response.size() - 1));
                Object msg = response.get(i);
                send(msg, more);
            }
        }
    }

    protected void send(Object msg, boolean more) throws Exception {
        if ((msg != null) && (msg instanceof byte[])) {
            if (more) {
                socket.sendMore((byte[]) msg);
            } else {
                socket.send((byte[]) msg);
            }
        } else {
            if (more) {
                socket.sendMore(String.valueOf(msg));
            } else {
                socket.send(String.valueOf(msg));
            }
        }
    }

    protected String recvStr() throws Exception {
        return socket.recvStr();
    }

    protected byte[] recv() throws Exception {
        return socket.recv();
    }

    /**
     * Overridable: default reads requestParts strings.
     */
    protected List receive() throws Exception {
        List ret = new ArrayList();
        for (int i = 0; i < requestParts; i++) {
            ret.add(recvStr());
        }
        return ret;

    }

    /**
     * Returns the answer
     */
    abstract protected List reply(List request) throws InterruptedException;

    @Override
    public void close() throws Exception {
        socket.close();
    }

}
