package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * Wraps an EPICS PV as a double register array.
 */
public class ChannelDoubleArray extends EpicsRegisterArray<double[]> implements DoubleType{

    public ChannelDoubleArray(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelDoubleArray(String name, String channelName, int precision) {
        super(name, channelName, precision);
    }

    public ChannelDoubleArray(String name, String channelName, int precision, int size) {
        super(name, channelName, precision, size);
    }

    public ChannelDoubleArray(String name, String channelName, int precision, int size, boolean timestamped) {
        super(name, channelName, precision, size, timestamped);
    }

    public ChannelDoubleArray(String name, String channelName, int precision, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, size, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.DoubleArrayTimestamp.class;
        } else {
            return double[].class;
        }
    }

    @Override
    public int getComponentSize() {
        return Double.BYTES / Byte.BYTES;
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(new double[]{0, 0, 0, 0, 0});
    }
}
