package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.UnsignedByteType;

/**
 * Wraps an EPICS PV as a byte register.
 */
public class ChannelByte extends EpicsRegisterNumber<Byte> implements UnsignedByteType{

    public ChannelByte(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelByte(String name, String channelName, boolean timestamped) {
        super(name, channelName, -1, timestamped);
    }

    public ChannelByte(String name, String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.ByteTimestamp.class;
        } else {
            return Byte.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0);
    }
}
