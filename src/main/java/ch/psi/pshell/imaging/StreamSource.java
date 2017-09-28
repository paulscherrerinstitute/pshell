package ch.psi.pshell.imaging;

import ch.psi.utils.Convert;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;

/**
 *
 */
public abstract class StreamSource extends ColormapSource {

    final ZMQ.Context context;
    final String address;
    ZMQ.Socket socket;

    final Object lock;
    int socketType = ZMQ.SUB;

    public StreamSource(String name, String address) {
        super(name, new ColormapSourceConfig());
        this.lock = new Object();
        this.address = address;
        context = ZMQ.context(1);
    }

    public int getSocketType() {
        return socketType;
    }

    public void setSocketType(int value) {
        socketType = value;
    }

    public String getAddress() {
        return address;
    }

    @Override
    protected void doInitialize() {
        checkStream();
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (isInitialized()) {
            checkStream();
        }
    }

    void checkStream() {
        stopReceiverThread();
        if (isMonitored()) {
            try {
                startReceiverThread();
                setState(State.Busy);
            } catch (Exception ex) {
                Logger.getLogger(StreamSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (!isMonitored()) {
            try {
                setState(State.Busy);
                openStream();
                readStream();
            } finally {
                closeStream();
                setState(State.Ready);
            }
        } else {
            try {
                waitNext(0);
            } catch (java.util.concurrent.TimeoutException ex) {
            }
        }
    }

    Thread receiverThread;

    void startReceiverThread() {
        receiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openStream();
                    while (!Thread.currentThread().isInterrupted()) {
                        if (isInitialized()) {
                            readStream();
                        } else {
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                } finally {
                    closeStream();
                }
            }
        }, "StreamCamera receiver: " + getName());
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    void stopReceiverThread() {
        //closeStream();
        if (receiverThread != null) {
            try {
                //TODO: Killing thread because it blocks  if no message is received
                Threading.stop(receiverThread, true, 2000);
            } catch (InterruptedException ex) {
            }
            receiverThread = null;
        }
    }

    protected void closeStream() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    protected void openStream() {
        if (socket == null) {
            socket = context.socket(getSocketType());
            socket.connect(address);
            if (getSocketType() == ZMQ.SUB) {
                socket.subscribe("".getBytes());
            }
        }
    }

    public static class Frame {

        public int[] shape;
        public Class type;
        public byte[] data;
    }

    void readStream() throws IOException {
        Frame frame = getFrame(socket);
        int width, height;
        switch (frame.shape.length) {
            case 1:
                width = frame.shape[0];
                height = 1;
                break;
            case 2:
                width = frame.shape[0];
                height = frame.shape[1];
                break;
            default:
                throw new IOException("Invalid shape: " + Str.toString(frame.shape, 10));
        }
        pushData(Convert.cast(frame.data, frame.type), width, height, false);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    //Overridables
    @Override
    protected void doClose() throws IOException {
        super.doClose();
        stopReceiverThread();
        if (context != null) {
            try {
                context.term();
            } catch (Exception ex) {
            }
        }
    }

    abstract protected Frame getFrame(ZMQ.Socket socket) throws IOException;
}
