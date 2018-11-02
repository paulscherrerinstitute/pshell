package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.UnsignedShortType;

/**
 * Wraps an EPICS PV as a short register array.
 */
public class ChannelShortArray extends EpicsRegisterArray<short[]> implements UnsignedShortType{

    public ChannelShortArray(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelShortArray(String name, String channelName, int size) {
        super(name, channelName, -1, size);
    }

    public ChannelShortArray(String name, String channelName, int size, boolean timestamped) {
        super(name, channelName, -1, size, timestamped);
    }

    public ChannelShortArray(String name, String channelName, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, size, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.ShortArrayTimestamp.class;
        } else {
            return short[].class;
        }
    }

    @Override
    public int getComponentSize() {
        return Short.BYTES / Byte.BYTES;
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(new short[]{0, 0, 0, 0, 0});
    }
}
