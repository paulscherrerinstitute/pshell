package ch.psi.pshell.utils;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Helper class simplifying creation of a ZMQ publisher.
 */
public class Publisher implements AutoCloseable {

    final Thread thread;
    final int port;
    Socket socket;
    Context context;

    public class Event {

        String type;
        Object data;

        Event(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    final ArrayList<Event> events = new ArrayList();

    public Publisher(int port) {
        this.port = port;
        thread = new Thread(() -> {
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.PUB);

            socket.bind("tcp://*:" + port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Event[] evs = null;
                    synchronized (events) {
                        events.wait();
                        evs = events.toArray(new Event[0]);
                        events.clear();
                    }
                    for (Event ev : evs) {
                        socket.sendMore(ev.type);
                        if (ev.data instanceof byte[] bs) {
                            socket.send(bs);
                        } else if (ev.data instanceof String string) {
                            socket.send(string);
                        } else {
                            socket.send(ev.data.toString());
                        }
                    }

                } catch (InterruptedException ex) {
                    break;
                } catch (Exception ex) {
                    Logger.getLogger(Publisher.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            socket.close();
            context.term();
            Logger.getLogger(Publisher.class.getName()).log(Level.INFO, "Quitting");

        }, "Publisher task - port: " + port);
        thread.start();
    }

    public void sendEvent(String type, Object data) {
        synchronized (events) {
            if (!thread.isInterrupted()) {
                Event ev = new Event(type, data);
                events.add(ev);
                events.notify();
            }
        }
    }

    @Override
    public void close() throws Exception {
        thread.interrupt();
    }
}
