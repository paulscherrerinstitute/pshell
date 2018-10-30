package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.ByteType;

/**
 * Wraps an EPICS PV as a byte register array.
 */
public class ChannelByteArray extends EpicsRegisterArray<byte[]> implements ByteType{

    public ChannelByteArray(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelByteArray(String name, String channelName, int size) {
        super(name, channelName, -1, size);
    }

    public ChannelByteArray(String name, String channelName, int size, boolean timestamped) {
        super(name, channelName, -1, size, timestamped);
    }

    public ChannelByteArray(String name, String channelName, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, size, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.ByteArrayTimestamp.class;
        } else {
            return byte[].class;
        }
    }

    @Override
    public int getComponentSize() {
        return 1;
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(new byte[]{0, 0, 0, 0, 0});
    }
}
