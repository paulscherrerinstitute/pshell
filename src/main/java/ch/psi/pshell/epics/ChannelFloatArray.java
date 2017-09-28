package ch.psi.pshell.epics;

/**
 * Wraps an EPICS PV as a float register array.
 */
public class ChannelFloatArray extends EpicsRegisterArray<float[]> {

    public ChannelFloatArray(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelFloatArray(String name, String channelName, int precision) {
        super(name, channelName, precision);
    }

    public ChannelFloatArray(String name, String channelName, int precision, int size) {
        super(name, channelName, precision, size);
    }

    public ChannelFloatArray(String name, String channelName, int precision, int size, boolean timestamped) {
        super(name, channelName, precision, size, timestamped);
    }

    public ChannelFloatArray(String name, String channelName, int precision, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, size, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.FloatArrayTimestamp.class;
        } else {
            return float[].class;
        }
    }

    @Override
    public int getComponentSize() {
        return Float.BYTES / Byte.BYTES;
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(new float[]{0, 0, 0, 0, 0});
    }
}
