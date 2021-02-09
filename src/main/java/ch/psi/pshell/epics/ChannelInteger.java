package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.IntegerType;

/**
 * Wraps an EPICS PV as an integer register.
 */
public class ChannelInteger extends EpicsRegisterNumber<Integer> implements IntegerType{

    public ChannelInteger(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelInteger(String name, String channelName, boolean timestamped) {
        super(name, channelName, UNDEFINED_PRECISION, timestamped);
    }

    public ChannelInteger(String name, String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, UNDEFINED_PRECISION, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.IntegerTimestamp.class;
        } else {
            return Integer.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0);
    }
}
