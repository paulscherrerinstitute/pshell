package ch.psi.pshell.bs;

import java.lang.reflect.Array;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Represents a 1-dimensional array element in a BS stream.
 */
public class Waveform<T> extends StreamChannel<T> implements ReadonlyRegisterArray<T> {

    Integer size;

    public Waveform(String name, Stream stream) {
        super(name, stream, new StreamChannelConfig());
    }

    public Waveform(String name, Stream stream, String id) {
        super(name, stream, id);
    }

    public Waveform(String name, Stream stream, String id, int modulo) {
        super(name, stream, id, modulo);
    }

    public Waveform(String name, Stream stream, String id, int modulo, int offset) {
        super(name, stream, id, modulo, offset);
    }

    public Waveform(String name, Stream stream, String id, int modulo, int offset, int size) {
        super(name, stream, id, modulo, offset);
        this.size = (size < 0) ? null : size;
    }

    @Override
    public WaveformConfig getConfig() {
        return (WaveformConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (getConfig() != null) {
            size = getConfig().size;
        }
    }

    @Override
    public int getSize() {
        if (size != null) {
            return size;
        }
        Object value = take();

        //TODO: This cannot be blocking: it is called from GUI thread to setup plots
        if ((value == null) || (!value.getClass().isArray())) {
            //update();
            throw new RuntimeException("Indefined waveform size");
        }
        return Array.getLength(value);
    }
}
