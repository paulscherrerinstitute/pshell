package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.IntegerType;

/**
 * Wraps an EPICS PV as an integer register array.
 */
public class ChannelIntegerArray extends EpicsRegisterArray<int[]> implements IntegerType{

    public ChannelIntegerArray(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelIntegerArray(String name, String channelName, int size) {
        super(name, channelName, UNDEFINED_PRECISION, size);
    }

    public ChannelIntegerArray(String name, String channelName, int size, boolean timestamped) {
        super(name, channelName, UNDEFINED_PRECISION, size, timestamped);
    }

    public ChannelIntegerArray(String name, String channelName, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, UNDEFINED_PRECISION, size, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.IntegerArrayTimestamp.class;
        } else {
            return int[].class;
        }
    }

    @Override
    public int getComponentSize() {
        return Integer.BYTES / Byte.BYTES;
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(new int[]{0, 0, 0, 0, 0});
    }
}
