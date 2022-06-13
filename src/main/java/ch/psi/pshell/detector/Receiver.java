package ch.psi.pshell.detector;

import ch.psi.pshell.imaging.StreamSource;
import ch.psi.utils.EncoderJson;
import java.io.IOException;
import org.zeromq.ZMQ;

/**
 * Imaging Source pushing the detector data.
 */
public class Receiver extends StreamSource {

    public Receiver(String name, String address) {
        super(name, address);
        setSocketType(ZMQ.PULL);
    }

    @Override
    protected Frame getFrame(ZMQ.Socket socket) throws IOException {
        Frame frame = new Frame();
        String json = socket.recvStr();
        FrameInfo info = (FrameInfo) EncoderJson.decode(json, FrameInfo.class);
        frame.shape = info.shape;
        frame.data = socket.recv();
        switch (info.dtype) {
            case "int8":
                frame.type = byte.class;
                break;
            case "int16":
                frame.type = short.class;
                break;
            case "int32":
                frame.type = int.class;
                break;
            case "int64":
                frame.type = long.class;
                break;
            case "float32":
                frame.type = float.class;
                break;
            case "float64":
                frame.type = double.class;
                break;
            default:
                throw new IOException("Invalid data type: " + info.dtype);
        }
        return frame;
    }
}
