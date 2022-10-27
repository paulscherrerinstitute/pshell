package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import java.lang.reflect.Array;

/**
 * Represents a 1-dimensional array element in a BS stream.
 */
public class Scalar<T> extends StreamChannel<T> {

    public Scalar(String name, Stream stream) {
        super(name, stream, new StreamChannelConfig());
    }

    public Scalar(String name, Stream stream, String id) {
        super(name, stream, id);
    }

    public Scalar(String name, Stream stream, String id, int modulo) {
        super(name, stream, id, modulo);
    }

    public Scalar(String name, Stream stream, String id, int modulo, int offset) {
        super(name, stream, id, modulo, offset);
    }

    public Scalar(String name, Stream stream, String id, int modulo, int offset, int size) {
        super(name, stream, id, modulo, offset);
    }

    @Override
    public WaveformConfig getConfig() {
        return (WaveformConfig) super.getConfig();
    }
    
    @Override
    public int[] getShape(){
        return new int[0];
    }
    
    @Override
    protected void set(long pulseId, long timestamp, long nanosOffset, T value, ChannelConfig config) {
        if ((value!=null) && (value.getClass().isArray())){
            value = (T) Array.get(value, 0);
        } 
        super.set(pulseId, timestamp, nanosOffset, value, config);
    }
}
