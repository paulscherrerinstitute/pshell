package ch.psi.pshell.detector;

import ch.psi.pshell.imaging.StreamSource;
import ch.psi.pshell.utils.EncoderJson;
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
        frame.type = switch (info.dtype) {
            case "int8" -> byte.class;
            case "int16" -> short.class;
            case "int32" -> int.class;
            case "int64" -> long.class;
            case "float32" ->  float.class;
            case "float64" -> double.class;
            default -> throw new IOException("Invalid data type: " + info.dtype);
        };
        return frame;
    }
}
